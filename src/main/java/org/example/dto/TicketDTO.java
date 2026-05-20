package org.example.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketDTO {
    private Long id;
    private Long userId;
    private String username;
    private String question;
    private String aiAnswer;
    private Integer status;
    private String statusText;
    private Long handlerId;
    private String handlerName;
    private LocalDateTime handledAt;
    private String result;
    private String followUp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
