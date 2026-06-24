package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.ChatSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ChatSessionStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private ChatSessionStore chatSessionStore;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        chatSessionStore = new ChatSessionStore(redisTemplate, new ObjectMapper(), 2L);
    }

    @Test
    void shouldReturnEmpty_whenSessionValueMissing() {
        Mockito.when(valueOperations.get("chat:session:missing")).thenReturn(null);

        Optional<ChatSession> session = chatSessionStore.get("missing");

        Assertions.assertTrue(session.isEmpty(), "Redis 中无数据时应返回 Optional.empty");
    }

    @Test
    void shouldDeserializeSession_whenSessionExists() {
        Mockito.when(valueOperations.get("chat:session:s1")).thenReturn("""
                {"sessionId":"s1","userId":9,"messageHistory":[],"createTime":123}
                """);

        Optional<ChatSession> session = chatSessionStore.get("s1");

        Assertions.assertTrue(session.isPresent(), "有效 JSON 应反序列化为会话对象");
        Assertions.assertEquals(9L, session.get().getUserId(), "应保留会话用户 ID");
    }

    @Test
    void shouldSaveSessionWithConfiguredTtl() {
        ChatSession session = new ChatSession("s1", 9L);

        chatSessionStore.save(session);

        Mockito.verify(valueOperations).set(Mockito.eq("chat:session:s1"),
                Mockito.contains("\"sessionId\":\"s1\""), Mockito.eq(Duration.ofHours(2)));
    }

    @Test
    void shouldClearExistingHistoryAndSaveSession() {
        Mockito.when(valueOperations.get("chat:session:s1")).thenReturn("""
                {"sessionId":"s1","userId":9,"messageHistory":[{"role":"user","content":"q"}],"createTime":123}
                """);

        boolean cleared = chatSessionStore.clearHistory("s1");

        Assertions.assertTrue(cleared, "存在的会话应返回清理成功");
        Mockito.verify(valueOperations).set(Mockito.eq("chat:session:s1"),
                Mockito.contains("\"messageHistory\":[]"), Mockito.eq(Duration.ofHours(2)));
    }

    @Test
    void shouldEnqueueSyncMessage() {
        Mockito.when(redisTemplate.opsForList()).thenReturn(listOperations);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        chatSessionStore.enqueueSyncMessage(9L, "s1", "user", "hello");

        Mockito.verify(listOperations).rightPush(Mockito.eq("chat:sync:queue"), jsonCaptor.capture());
        String json = jsonCaptor.getValue();
        Assertions.assertTrue(json.contains("\"userId\":9"), "入队消息应包含用户 ID");
        Assertions.assertTrue(json.contains("\"content\":\"hello\""), "入队消息应包含消息内容");
    }
}
