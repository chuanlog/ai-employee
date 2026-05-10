package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.ShowCollectionsResponse;
import io.milvus.param.R;
import io.milvus.param.collection.ShowCollectionsParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Milvus 测试控制器
 * 用于测试数据库连接和数据读取
 */
@RestController
@RequestMapping("/milvus")
@Tag(name = "Milvus 检查", description = "提供向量数据库连通性与集合信息检查接口")
public class MilvusCheckController {

    @Autowired
    private MilvusServiceClient milvusClient;

    /**
     * 简单的健康检查
     */
    @Operation(summary = "Milvus 健康检查", description = "检查 Milvus 连接状态和集合信息。")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> simpleHealth() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            R<ShowCollectionsResponse> response = milvusClient.showCollections(
                ShowCollectionsParam.newBuilder().build()
            );
            
            if (response.getStatus() == 0) {
                result.put("message", "ok");
                result.put("collections", response.getData().getCollectionNamesList());
                return ResponseEntity.ok(result);
            } else {
                result.put("message", response.getMessage());
                return ResponseEntity.status(503).body(result);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(503).body(result);
        }
    }

    @Schema(name = "MilvusHealthResponse", description = "Milvus 健康检查结果")
    public static class MilvusHealthResponse {
        @Schema(description = "健康检查结果消息", example = "ok")
        private String message;

        @ArraySchema(schema = @Schema(description = "Milvus 中已有的集合名称", example = "document_chunks"))
        private java.util.List<String> collections;

        @Schema(description = "连接异常时的错误信息", example = "connection refused")
        private String error;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public java.util.List<String> getCollections() {
            return collections;
        }

        public void setCollections(java.util.List<String> collections) {
            this.collections = collections;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
