package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ChatMessageEntity;

import java.util.List;

public interface ChatMessageService extends IService<ChatMessageEntity> {
    List<ChatMessageEntity> getRecentMessages(Long userId, int limit);
}
