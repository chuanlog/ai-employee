package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.milvus.client.MilvusServiceClient;
import org.example.client.MilvusClientFactory;
import org.example.config.FileUploadConfig;
import org.example.dto.KnowledgeDocumentDTO;
import org.example.dto.RebuildVectorStoreResponse;
import org.example.entity.KnowledgeDocumentEntity;
import org.example.mapper.KnowledgeDocumentMapper;
import org.example.service.KnowledgeDocumentService;
import org.example.service.MinioService;
import org.example.service.VectorIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocumentEntity>
        implements KnowledgeDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeDocumentServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_INDEXED = "INDEXED";
    private static final String STATUS_ERROR = "ERROR";
    private static final DateTimeFormatter OBJECT_KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Autowired
    private MinioService minioService;

    @Autowired
    private VectorIndexService vectorIndexService;

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private MilvusClientFactory milvusClientFactory;

    @Override
    public IPage<KnowledgeDocumentDTO> listDocuments(Integer pageNum, Integer pageSize) {
        Page<KnowledgeDocumentEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                .orderByDesc(KnowledgeDocumentEntity::getUpdatedAt)
                .orderByDesc(KnowledgeDocumentEntity::getId);
        IPage<KnowledgeDocumentEntity> entityPage = page(page, wrapper);
        Page<KnowledgeDocumentDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::toDto).toList());
        return dtoPage;
    }

    @Override
    public KnowledgeDocumentDTO getDocument(Long id) {
        return toDto(requireDocument(id));
    }

    @Override
    public KnowledgeDocumentDTO uploadDocument(MultipartFile file, Long userId, String username) throws Exception {
        String fileName = validateFile(file);
        KnowledgeDocumentEntity existing = lambdaQuery()
                .eq(KnowledgeDocumentEntity::getFileName, fileName)
                .one();
        if (existing != null) {
            return replaceDocument(existing.getId(), file, userId, username);
        }

        byte[] bytes = file.getBytes();
        String objectKey = buildObjectKey(fileName);
        minioService.uploadObject(objectKey, bytes, file.getContentType());

        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setFileName(fileName);
        entity.setObjectKey(objectKey);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setUploaderId(userId);
        entity.setUploaderName(username);
        entity.setStatus(STATUS_PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        try {
            vectorIndexService.indexDocument(objectKey, fileName, new String(bytes, StandardCharsets.UTF_8));
            entity.setStatus(STATUS_INDEXED);
            entity.setErrorMessage(null);
        } catch (Exception e) {
            logger.error("新文档索引失败: {}", fileName, e);
            entity.setStatus(STATUS_ERROR);
            entity.setErrorMessage(truncateError(e.getMessage()));
        }

        save(entity);
        return toDto(entity);
    }

    @Override
    public KnowledgeDocumentDTO replaceDocument(Long id, MultipartFile file, Long userId, String username) throws Exception {
        KnowledgeDocumentEntity existing = requireDocument(id);
        String fileName = validateFile(file);

        KnowledgeDocumentEntity duplicated = lambdaQuery()
                .eq(KnowledgeDocumentEntity::getFileName, fileName)
                .ne(KnowledgeDocumentEntity::getId, id)
                .one();
        if (duplicated != null) {
            throw new RuntimeException("已存在同名知识库文档，请先删除或更换文件名");
        }

        byte[] bytes = file.getBytes();
        String newObjectKey = buildObjectKey(fileName);
        minioService.uploadObject(newObjectKey, bytes, file.getContentType());

        try {
            vectorIndexService.indexDocument(newObjectKey, fileName, new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("替换文档索引失败: {}", fileName, e);
            minioService.deleteObject(newObjectKey);
            throw new RuntimeException("替换文档失败，原文档已保留: " + e.getMessage(), e);
        }

        vectorIndexService.deleteBySource(existing.getObjectKey());
        minioService.deleteObject(existing.getObjectKey());

        existing.setFileName(fileName);
        existing.setObjectKey(newObjectKey);
        existing.setContentType(file.getContentType());
        existing.setFileSize(file.getSize());
        existing.setUploaderId(userId);
        existing.setUploaderName(username);
        existing.setStatus(STATUS_INDEXED);
        existing.setErrorMessage(null);
        existing.setUpdatedAt(LocalDateTime.now());
        updateById(existing);
        return toDto(existing);
    }

    @Override
    public void deleteDocument(Long id) throws Exception {
        KnowledgeDocumentEntity entity = requireDocument(id);
        vectorIndexService.deleteBySource(entity.getObjectKey());
        minioService.deleteObject(entity.getObjectKey());
        removeById(id);
    }

    @Override
    public RebuildVectorStoreResponse rebuildVectorStore() {
        RebuildVectorStoreResponse response = new RebuildVectorStoreResponse();
        List<KnowledgeDocumentEntity> documents = lambdaQuery()
                .orderByAsc(KnowledgeDocumentEntity::getId)
                .list();
        response.setTotalDocuments(documents.size());

        milvusClientFactory.recreateCollection(milvusClient);

        for (KnowledgeDocumentEntity document : documents) {
            try {
                document.setStatus(STATUS_PENDING);
                document.setErrorMessage(null);
                document.setUpdatedAt(LocalDateTime.now());
                updateById(document);

                byte[] bytes = minioService.getObjectBytes(document.getObjectKey());
                String content = new String(bytes, StandardCharsets.UTF_8);
                vectorIndexService.indexDocument(document.getObjectKey(), document.getFileName(), content);

                document.setStatus(STATUS_INDEXED);
                document.setErrorMessage(null);
                document.setUpdatedAt(LocalDateTime.now());
                updateById(document);
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception e) {
                logger.error("重建文档索引失败: {}", document.getFileName(), e);
                document.setStatus(STATUS_ERROR);
                document.setErrorMessage(truncateError(e.getMessage()));
                document.setUpdatedAt(LocalDateTime.now());
                updateById(document);
                response.setFailCount(response.getFailCount() + 1);
                response.getFailedDocuments().add(document.getFileName() + ": " + truncateError(e.getMessage()));
            }
        }
        return response;
    }

    private KnowledgeDocumentEntity requireDocument(Long id) {
        KnowledgeDocumentEntity entity = getById(id);
        if (entity == null) {
            throw new RuntimeException("知识库文档不存在");
        }
        return entity;
    }

    private String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        String sourceFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(sourceFileName)) {
            throw new RuntimeException("文件名不能为空");
        }
        String originalFilename = StringUtils.cleanPath(sourceFileName);
        String extension = getFileExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new RuntimeException("不支持的文件格式，仅支持: " + fileUploadConfig.getAllowedExtensions());
        }
        return originalFilename;
    }

    private boolean isAllowedExtension(String extension) {
        String allowedExtensions = fileUploadConfig.getAllowedExtensions();
        if (!StringUtils.hasText(allowedExtensions)) {
            return false;
        }
        return Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .anyMatch(item -> item.equalsIgnoreCase(extension));
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex < 0) {
            return "";
        }
        return fileName.substring(lastIndex + 1);
    }

    private String buildObjectKey(String fileName) {
        String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String datePrefix = LocalDateTime.now().format(OBJECT_KEY_DATE_FORMATTER);
        return "knowledge-base/" + datePrefix + "/" + UUID.randomUUID() + "-" + safeFileName;
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private KnowledgeDocumentDTO toDto(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
