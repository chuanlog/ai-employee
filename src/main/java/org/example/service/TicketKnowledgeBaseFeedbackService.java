package org.example.service;

import org.example.entity.TicketEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

@Service
public class TicketKnowledgeBaseFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(TicketKnowledgeBaseFeedbackService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeDocumentService knowledgeDocumentService;

    public TicketKnowledgeBaseFeedbackService(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @Async("ticketKnowledgeBaseTaskExecutor")
    public void feedHandledTicket(TicketEntity ticket) {
        if (ticket == null || ticket.getId() == null || !StringUtils.hasText(ticket.getResult())) {
            return;
        }

        String fileName = buildFileName(ticket.getId());
        String content = buildKnowledgeContent(ticket);
        try {
            knowledgeDocumentService.saveTextDocument(fileName, content, ticket.getHandlerId(), ticket.getHandlerName());
            logger.info("工单问答对已异步反哺知识库: ticketId={}, fileName={}", ticket.getId(), fileName);
        } catch (Exception e) {
            logger.error("工单问答对反哺知识库失败: ticketId={}, fileName={}", ticket.getId(), fileName, e);
        }
    }

    private String buildFileName(Long ticketId) {
        return "ticket-" + ticketId + "-faq.md";
    }

    private String buildKnowledgeContent(TicketEntity ticket) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 工单问答沉淀 #").append(ticket.getId()).append("\n\n");
        builder.append("## 问题\n\n").append(ticket.getQuestion()).append("\n\n");
        builder.append("## 答案\n\n").append(ticket.getResult()).append("\n\n");
        if (StringUtils.hasText(ticket.getFollowUp())) {
            builder.append("## 回访记录\n\n").append(ticket.getFollowUp()).append("\n\n");
        }
        builder.append("## 元数据\n\n");
        builder.append("- 工单ID: ").append(ticket.getId()).append("\n");
        builder.append("- 提交用户: ").append(nullToEmpty(ticket.getUsername())).append("\n");
        builder.append("- 处理人: ").append(nullToEmpty(ticket.getHandlerName())).append("\n");
        if (ticket.getHandledAt() != null) {
            builder.append("- 处理时间: ").append(ticket.getHandledAt().format(DATE_TIME_FORMATTER)).append("\n");
        }
        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
