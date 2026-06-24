package org.example.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ChatSessionTest {

    @Test
    void shouldKeepOnlyLatestMessagePairs_whenHistoryExceedsWindowSize() {
        ChatSession session = new ChatSession("session-1", 10L);

        session.addMessage("q1", "a1", 2);
        session.addMessage("q2", "a2", 2);
        session.addMessage("q3", "a3", 2);

        List<Map<String, String>> history = session.getHistory();
        Assertions.assertEquals(2, session.getMessagePairCount(), "只应保留最近 2 组问答");
        Assertions.assertEquals("q2", history.get(0).get("content"), "最早一组问答应被裁剪");
        Assertions.assertEquals("a3", history.get(3).get("content"), "最新回答应保留在历史中");
    }

    @Test
    void shouldReturnDefensiveCopy_whenGettingHistory() {
        ChatSession session = new ChatSession("session-1");
        session.addMessage("question", "answer", 1);

        List<Map<String, String>> history = session.getHistory();
        history.clear();

        Assertions.assertEquals(1, session.getMessagePairCount(), "外部修改 getHistory 返回值不应影响会话内部历史");
    }

    @Test
    void shouldClearHistory() {
        ChatSession session = new ChatSession("session-1");
        session.addMessage("question", "answer", 1);

        session.clearHistory();

        Assertions.assertTrue(session.getHistory().isEmpty(), "清空后历史消息应为空");
        Assertions.assertEquals(0, session.getMessagePairCount(), "清空后消息对数量应为 0");
    }
}
