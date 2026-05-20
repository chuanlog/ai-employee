package org.example.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.KnowledgeDocumentDTO;
import org.example.dto.RebuildVectorStoreResponse;
import org.example.service.KnowledgeDocumentService;
import org.example.util.RequestUserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @GetMapping("/documents")
    public ResponseEntity<IPage<KnowledgeDocumentDTO>> listDocuments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                                     HttpServletRequest request) {
        RequestUserUtils.requireAdmin(request);
        return ResponseEntity.ok(knowledgeDocumentService.listDocuments(pageNum, pageSize));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<KnowledgeDocumentDTO> getDocument(@PathVariable Long id,
                                                            HttpServletRequest request) {
        RequestUserUtils.requireAdmin(request);
        return ResponseEntity.ok(knowledgeDocumentService.getDocument(id));
    }

    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        try {
            RequestUserUtils.requireAdmin(request);
            KnowledgeDocumentDTO document = knowledgeDocumentService.uploadDocument(
                    file,
                    RequestUserUtils.getUserId(request),
                    RequestUserUtils.getUsername(request)
            );
            String message = "知识库文档上传成功";
            if ("ERROR".equalsIgnoreCase(document.getStatus())) {
                message = "文档已保存到知识库，但索引失败，可稍后执行向量库重建";
            }
            return ResponseEntity.ok(Map.of("message", message, "data", document));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping(value = "/documents/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> replaceDocument(@PathVariable Long id,
                                             @RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        try {
            RequestUserUtils.requireAdmin(request);
            KnowledgeDocumentDTO document = knowledgeDocumentService.replaceDocument(
                    id,
                    file,
                    RequestUserUtils.getUserId(request),
                    RequestUserUtils.getUsername(request)
            );
            return ResponseEntity.ok(Map.of("message", "知识库文档替换成功", "data", document));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id,
                                            HttpServletRequest request) {
        try {
            RequestUserUtils.requireAdmin(request);
            knowledgeDocumentService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "知识库文档删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/rebuild")
    public ResponseEntity<?> rebuildVectorStore(HttpServletRequest request) {
        try {
            RequestUserUtils.requireAdmin(request);
            RebuildVectorStoreResponse response = knowledgeDocumentService.rebuildVectorStore();
            return ResponseEntity.ok(Map.of("message", "向量库重建完成", "data", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }
}
