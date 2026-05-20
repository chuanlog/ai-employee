package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.FileUploadRes;
import org.example.dto.KnowledgeDocumentDTO;
import org.example.service.KnowledgeDocumentService;
import org.example.util.RequestUserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Tag(name = "文件管理", description = "提供知识库文件上传与索引构建能力")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Operation(summary = "上传知识库文件", description = "上传文件并创建对应的向量索引。")
    @PostMapping(value = "/api/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            RequestUserUtils.requireAdmin(request);
            KnowledgeDocumentDTO document = knowledgeDocumentService.uploadDocument(
                    file,
                    RequestUserUtils.getUserId(request),
                    RequestUserUtils.getUsername(request)
            );

            FileUploadRes response = new FileUploadRes(
                    document.getFileName(),
                    document.getObjectKey(),
                    document.getFileSize()
            );

            ApiResponse<FileUploadRes> apiResponse = new ApiResponse<>();
            apiResponse.setCode(200);
            apiResponse.setMessage("ERROR".equalsIgnoreCase(document.getStatus())
                    ? "文档已保存到知识库，但索引失败，可稍后执行向量库重建"
                    : "success");
            apiResponse.setData(response);

            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            logger.warn("知识库上传失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("知识库上传失败", e);
            ApiResponse<String> errorResponse = new ApiResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage("文件上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 统一 API 响应格式
     */
    @Schema(name = "FileUploadApiWrapper", description = "统一接口响应包装")
    public static class ApiResponse<T> {
        @Schema(description = "业务状态码", example = "200")
        private int code;
        @Schema(description = "响应消息", example = "success")
        private String message;
        @Schema(description = "响应数据体")
        private T data;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    @Schema(name = "FileUploadApiResponse", description = "文件上传成功响应")
    public static class FileUploadApiResponse extends ApiResponse<FileUploadRes> {
    }

    @Schema(name = "UploadErrorApiResponse", description = "文件上传失败响应")
    public static class UploadErrorApiResponse extends ApiResponse<String> {
    }
}
