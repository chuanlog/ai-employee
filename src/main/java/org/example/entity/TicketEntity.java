package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_ticket")
public class TicketEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String question;

    private String aiAnswer;

    private Integer status;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handledAt;

    private String result;

    private String followUp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
