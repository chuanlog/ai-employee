package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话上下文数据对象，存储到 Redis 中。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    private String sessionId;
    private Long userId; // 新增 userId 字段
    // 历史消息格式：[{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}]
    private List<Map<String, String>> messageHistory = new ArrayList<>();
    private long createTime;

    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.createTime = System.currentTimeMillis();
        this.messageHistory = new ArrayList<>();
    }

    public ChatSession(String sessionId, Long userId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createTime = System.currentTimeMillis();
        this.messageHistory = new ArrayList<>();
    }

    public void addMessage(String userQuestion, String aiAnswer, int maxWindowSize) {
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userQuestion);
        messageHistory.add(userMsg);

        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", aiAnswer);
        messageHistory.add(assistantMsg);

        int maxMessages = maxWindowSize * 2;
        while (messageHistory.size() > maxMessages) {
            messageHistory.remove(0);
            if (!messageHistory.isEmpty()) {
                messageHistory.remove(0);
            }
        }
    }

    @JsonIgnore
    public List<Map<String, String>> getHistory() {
        return new ArrayList<>(messageHistory);
    }

    public void clearHistory() {
        messageHistory.clear();
    }

    @JsonIgnore
    public int getMessagePairCount() {
        return messageHistory.size() / 2;
    }
}
