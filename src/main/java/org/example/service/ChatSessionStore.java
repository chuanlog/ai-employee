package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的会话存储。
 */
@Service
public class ChatSessionStore {

    private static final Logger logger = LoggerFactory.getLogger(ChatSessionStore.class);
    private static final String KEY_PREFIX = "chat:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration sessionTtl;

    public ChatSessionStore(StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper,
                            @Value("${chat.session.ttl-hours:168}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sessionTtl = Duration.ofHours(ttlHours);
    }

    public ChatSession getOrCreate(String sessionId, Long userId) {
        return get(sessionId).orElseGet(() -> {
            ChatSession session = new ChatSession(sessionId, userId);
            save(session);
            logger.info("创建新会话并写入 Redis - SessionId: {}, UserId: {}", sessionId, userId);
            return session;
        });
    }

    public Optional<ChatSession> get(String sessionId) {
        String value = redisTemplate.opsForValue().get(buildKey(sessionId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, ChatSession.class));
        } catch (Exception e) {
            throw new RuntimeException("读取 Redis 会话失败: " + sessionId, e);
        }
    }

    public void save(ChatSession session) {
        try {
            String value = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(buildKey(session.getSessionId()), value, sessionTtl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化会话失败: " + session.getSessionId(), e);
        }
    }

    public boolean clearHistory(String sessionId) {
        Optional<ChatSession> existing = get(sessionId);
        if (existing.isEmpty()) {
            return false;
        }

        ChatSession session = existing.get();
        session.clearHistory();
        save(session);
        return true;
    }

    public void enqueueSyncMessage(Long userId, String sessionId, String role, String content) {
        try {
            java.util.Map<String, Object> msg = new java.util.HashMap<>();
            msg.put("userId", userId);
            msg.put("sessionId", sessionId);
            msg.put("role", role);
            msg.put("content", content);
            msg.put("createdAt", java.time.LocalDateTime.now().toString());
            
            String json = objectMapper.writeValueAsString(msg);
            redisTemplate.opsForList().rightPush("chat:sync:queue", json);
        } catch (Exception e) {
            logger.error("消息加入同步队列失败", e);
        }
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
