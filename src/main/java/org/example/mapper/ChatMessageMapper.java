package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.ChatMessageEntity;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
