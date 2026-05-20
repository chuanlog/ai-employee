package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class KnowledgeDocumentDTO {

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
