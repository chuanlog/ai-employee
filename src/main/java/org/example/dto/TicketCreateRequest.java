package org.example.dto;

import lombok.Data;

@Data
public class TicketCreateRequest {
    private String question;
    private String aiAnswer;
}
