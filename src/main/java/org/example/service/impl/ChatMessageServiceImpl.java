package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ChatMessageEntity;
import org.example.mapper.ChatMessageMapper;
import org.example.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessageEntity> implements ChatMessageService {

    @Override
    public List<ChatMessageEntity> getRecentMessages(Long userId, int limit) {
        QueryWrapper<ChatMessageEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                    .orderByDesc("created_at", "id")
                    .last("LIMIT " + limit);
        List<ChatMessageEntity> list = this.list(queryWrapper);
        // 因为按倒序查的，所以要反转回来变成正序
        Collections.reverse(list);
        return list;
    }
}
