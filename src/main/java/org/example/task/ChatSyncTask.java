package org.example.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ChatMessageEntity;
import org.example.service.ChatMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChatSyncTask {
    private static final Logger logger = LoggerFactory.getLogger(ChatSyncTask.class);
    private static final String QUEUE_KEY = "chat:sync:queue";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private ObjectMapper objectMapper;

    private volatile boolean running = true;
    private Thread syncThread;

    @PostConstruct
    public void init() {
        syncThread = new Thread(() -> {
            logger.info("启动聊天记录 Redis 队列消费线程...");
            while (running) {
                try {
                    consumeAndSave();
                    // 控制消费和写库频率，0.5秒一次
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.warn("聊天记录同步线程被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("聊天记录同步线程发生异常", e);
                }
            }
        }, "ChatSyncThread");
        syncThread.start();
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (syncThread != null) {
            syncThread.interrupt();
        }
        logger.info("停止聊天记录 Redis 队列消费线程...");
    }
    
    private void consumeAndSave() {
        List<ChatMessageEntity> batch = new ArrayList<>();
        int count = 0;
        
        while (count < 50) { // 每次最多取 50 条批量插入
            // 这里可以直接 leftPop，因为外层有 sleep 控制频率，不用阻塞等
            String json = redisTemplate.opsForList().leftPop(QUEUE_KEY);
            if (json == null) {
                break;
            }
            
            try {
                JsonNode node = objectMapper.readTree(json);
                ChatMessageEntity entity = new ChatMessageEntity();
                entity.setUserId(node.get("userId").asLong());
                entity.setSessionId(node.get("sessionId").asText());
                entity.setRole(node.get("role").asText());
                entity.setContent(node.get("content").asText());
                
                String createdAtStr = node.get("createdAt").asText();
                entity.setCreatedAt(LocalDateTime.parse(createdAtStr));
                
                batch.add(entity);
                count++;
            } catch (Exception e) {
                logger.error("解析同步消息失败: {}", json, e);
            }
        }
        
        if (!batch.isEmpty()) {
            chatMessageService.saveBatch(batch);
            logger.info("成功同步 {} 条聊天记录到数据库", batch.size());
        }
    }
}
