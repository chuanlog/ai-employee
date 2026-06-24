package org.example.service.impl;

import org.example.constant.TicketConstants;
import org.example.dto.TicketCreateRequest;
import org.example.dto.TicketDTO;
import org.example.dto.TicketHandleRequest;
import org.example.entity.TicketEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TicketServiceImplTest {

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = Mockito.spy(new TicketServiceImpl());
    }

    @Test
    void shouldCreatePendingTicket() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setQuestion("CPU 使用率过高如何处理？");
        Mockito.doReturn(true).when(ticketService).save(Mockito.any(TicketEntity.class));

        TicketDTO dto = ticketService.createTicket(request, 10L, "alice", "先查看进程");

        Assertions.assertEquals("CPU 使用率过高如何处理？", dto.getQuestion(), "工单问题应来自请求");
        Assertions.assertEquals(TicketConstants.STATUS_PENDING, dto.getStatus(), "新建工单应为待处理状态");
        ArgumentCaptor<TicketEntity> captor = ArgumentCaptor.forClass(TicketEntity.class);
        Mockito.verify(ticketService).save(captor.capture());
        Assertions.assertEquals("alice", captor.getValue().getUsername(), "应记录创建人用户名");
    }

    @Test
    void shouldReturnTicketWithStatusText() {
        TicketEntity ticket = buildTicket(TicketConstants.STATUS_COMPLETED);
        Mockito.doReturn(ticket).when(ticketService).getById(1L);

        TicketDTO dto = ticketService.getTicketById(1L);

        Assertions.assertEquals("已完成", dto.getStatusText(), "状态 3 应转换为已完成文案");
    }

    @Test
    void shouldRejectHandleTicket_whenAlreadyCompleted() {
        TicketEntity ticket = buildTicket(TicketConstants.STATUS_COMPLETED);
        Mockito.doReturn(ticket).when(ticketService).getById(1L);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> ticketService.handleTicket(1L, new TicketHandleRequest(), 99L, "handler"));

        Assertions.assertEquals("工单已完成，不可修改", exception.getMessage(), "已完成工单不能重复处理");
        Mockito.verify(ticketService, Mockito.never()).updateById(Mockito.any(TicketEntity.class));
    }

    @Test
    void shouldHandlePendingTicketAndMarkCompleted() {
        TicketEntity ticket = buildTicket(TicketConstants.STATUS_PENDING);
        TicketHandleRequest request = new TicketHandleRequest();
        request.setResult("扩容并清理异常进程");
        request.setFollowUp("继续观察 30 分钟");
        Mockito.doReturn(ticket).when(ticketService).getById(1L);
        Mockito.doReturn(true).when(ticketService).updateById(Mockito.any(TicketEntity.class));

        TicketDTO dto = ticketService.handleTicket(1L, request, 99L, "handler");

        Assertions.assertEquals(TicketConstants.STATUS_COMPLETED, dto.getStatus(), "处理后应标记为已完成");
        Assertions.assertEquals("扩容并清理异常进程", dto.getResult(), "处理结果应写入工单");
        Mockito.verify(ticketService).updateById(ticket);
    }

    @Test
    void shouldRejectDeleteTicket_whenTicketIsNotCompleted() {
        TicketEntity ticket = buildTicket(TicketConstants.STATUS_PENDING);
        Mockito.doReturn(ticket).when(ticketService).getById(1L);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> ticketService.deleteTicket(1L));

        Assertions.assertEquals("只能删除已完成的工单", exception.getMessage(), "仅已完成工单可删除");
        Mockito.verify(ticketService, Mockito.never()).removeById(Mockito.any(Long.class));
    }

    private TicketEntity buildTicket(Integer status) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(1L);
        ticket.setUserId(10L);
        ticket.setUsername("alice");
        ticket.setQuestion("问题");
        ticket.setAiAnswer("AI 回答");
        ticket.setStatus(status);
        return ticket;
    }
}
