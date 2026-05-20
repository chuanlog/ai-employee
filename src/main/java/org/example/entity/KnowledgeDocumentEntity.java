package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("knowledge_document")
public class KnowledgeDocumentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String objectKey;

    private String contentType;

    private Long fileSize;

    private Long uploaderId;

    private String uploaderName;

    private String status;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
