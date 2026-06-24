package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.KnowledgeDocumentDTO;
import org.example.dto.RebuildVectorStoreResponse;
import org.example.service.KnowledgeDocumentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

class KnowledgeBaseControllerTest {

    private KnowledgeDocumentService knowledgeDocumentService;
    private KnowledgeBaseController controller;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        knowledgeDocumentService = Mockito.mock(KnowledgeDocumentService.class);
        controller = new KnowledgeBaseController();
        ReflectionTestUtils.setField(controller, "knowledgeDocumentService", knowledgeDocumentService);
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute("userRole")).thenReturn("admin");
        Mockito.when(request.getAttribute("userId")).thenReturn(7L);
        Mockito.when(request.getAttribute("username")).thenReturn("admin");
    }

    @Test
    void shouldUploadDocumentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ops.md", "text/markdown", "content".getBytes());
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setId(1L);
        dto.setFileName("ops.md");
        dto.setStatus("INDEXED");
        Mockito.when(knowledgeDocumentService.uploadDocument(file, 7L, "admin")).thenReturn(dto);

        ResponseEntity<?> response = controller.uploadDocument(file, request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(), "上传成功应返回 200");
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Assertions.assertEquals("知识库文档上传成功", body.get("message"), "索引成功时应返回成功消息");
        Assertions.assertSame(dto, body.get("data"), "响应数据应为服务层返回的文档 DTO");
    }

    @Test
    void shouldReturnBadRequest_whenRuntimeExceptionThrown() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[0]);
        Mockito.when(knowledgeDocumentService.uploadDocument(file, 7L, "admin"))
                .thenThrow(new RuntimeException("不支持的文件格式"));

        ResponseEntity<?> response = controller.uploadDocument(file, request);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "业务异常应转换为 400");
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Assertions.assertEquals("不支持的文件格式", body.get("message"), "响应应透出业务错误信息");
    }

    @Test
    void shouldReturnErrorMessage_whenIndexFailedButDocumentSaved() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "ops.md", "text/markdown", "content".getBytes());
        KnowledgeDocumentDTO dto = new KnowledgeDocumentDTO();
        dto.setStatus("ERROR");
        Mockito.when(knowledgeDocumentService.uploadDocument(file, 7L, "admin")).thenReturn(dto);

        ResponseEntity<?> response = controller.uploadDocument(file, request);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Assertions.assertEquals("文档已保存到知识库，但索引失败，可稍后执行向量库重建",
                body.get("message"), "索引失败但文档保存成功时应返回降级提示");
    }

    @Test
    void shouldRebuildVectorStore_whenAdmin() {
        RebuildVectorStoreResponse rebuildResponse = new RebuildVectorStoreResponse();
        rebuildResponse.setTotalDocuments(3);
        Mockito.when(knowledgeDocumentService.rebuildVectorStore()).thenReturn(rebuildResponse);

        ResponseEntity<?> response = controller.rebuildVectorStore(request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(), "管理员重建向量库应返回 200");
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        Assertions.assertEquals("向量库重建完成", body.get("message"), "应返回重建完成消息");
    }
}
