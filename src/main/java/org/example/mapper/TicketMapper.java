package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.TicketEntity;

@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {
}
