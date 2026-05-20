package org.example.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.KnowledgeDocumentDTO;
import org.example.dto.RebuildVectorStoreResponse;
import org.example.entity.KnowledgeDocumentEntity;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService extends IService<KnowledgeDocumentEntity> {

    IPage<KnowledgeDocumentDTO> listDocuments(Integer pageNum, Integer pageSize);

    KnowledgeDocumentDTO getDocument(Long id);

    KnowledgeDocumentDTO uploadDocument(MultipartFile file, Long userId, String username) throws Exception;

    KnowledgeDocumentDTO replaceDocument(Long id, MultipartFile file, Long userId, String username) throws Exception;

    void deleteDocument(Long id) throws Exception;

    RebuildVectorStoreResponse rebuildVectorStore();
}
