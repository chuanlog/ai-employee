package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.constant.TicketConstants;
import org.example.dto.TicketCreateRequest;
import org.example.dto.TicketDTO;
import org.example.dto.TicketHandleRequest;
import org.example.entity.TicketEntity;
import org.example.mapper.TicketMapper;
import org.example.service.TicketKnowledgeBaseFeedbackService;
import org.example.service.TicketService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketEntity> implements TicketService {

    @Autowired
    private TicketKnowledgeBaseFeedbackService ticketKnowledgeBaseFeedbackService;

    @Override
    @Transactional
    public TicketDTO createTicket(TicketCreateRequest request, Long userId, String username, String aiAnswer) {
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new RuntimeException("工单问题不能为空");
        }

        TicketEntity ticket = new TicketEntity();
        ticket.setUserId(userId);
        ticket.setUsername(username);
        ticket.setQuestion(request.getQuestion().trim());
        ticket.setAiAnswer(aiAnswer);
        ticket.setStatus(TicketConstants.STATUS_PENDING);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        save(ticket);
        return convertToDTO(ticket);
    }

    @Override
    public TicketDTO getTicketById(Long id) {
        TicketEntity ticket = getById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }
        return convertToDTO(ticket);
    }

    @Override
    public IPage<TicketDTO> listTickets(Integer pageNum, Integer pageSize, Integer status, Long userId) {
        Page<TicketEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TicketEntity> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(TicketEntity::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(TicketEntity::getUserId, userId);
        }

        wrapper.orderByDesc(TicketEntity::getCreatedAt);

        IPage<TicketEntity> entityPage = page(page, wrapper);
        Page<TicketDTO> dtoPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        dtoPage.setRecords(entityPage.getRecords().stream().map(this::convertToDTO).toList());

        return dtoPage;
    }

    @Override
    @Transactional
    public TicketDTO handleTicket(Long id, TicketHandleRequest request, Long handlerId, String handlerName) {
        TicketEntity ticket = getById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }

        if (ticket.getStatus().equals(TicketConstants.STATUS_COMPLETED)) {
            throw new RuntimeException("工单已完成，不可修改");
        }

        ticket.setHandlerId(handlerId);
        ticket.setHandlerName(handlerName);
        ticket.setHandledAt(LocalDateTime.now());
        ticket.setResult(request.getResult());
        ticket.setFollowUp(request.getFollowUp());
        ticket.setStatus(TicketConstants.STATUS_COMPLETED);
        ticket.setUpdatedAt(LocalDateTime.now());

        updateById(ticket);

        ticketKnowledgeBaseFeedbackService.feedHandledTicket(buildFeedbackSnapshot(ticket));

        return convertToDTO(ticket);
    }

    @Override
    public void addToKnowledgeBase(Long ticketId) {
        TicketEntity ticket = getById(ticketId);
        if (ticket == null || !StringUtils.hasText(ticket.getResult())) {
            return;
        }
        ticketKnowledgeBaseFeedbackService.feedHandledTicket(buildFeedbackSnapshot(ticket));
    }

    @Override
    @Transactional
    public void deleteTicket(Long id) {
        TicketEntity ticket = getById(id);
        if (ticket == null) {
            throw new RuntimeException("工单不存在");
        }
        if (!ticket.getStatus().equals(TicketConstants.STATUS_COMPLETED)) {
            throw new RuntimeException("只能删除已完成的工单");
        }
        removeById(id);
    }

    private TicketDTO convertToDTO(TicketEntity ticket) {
        TicketDTO dto = new TicketDTO();
        BeanUtils.copyProperties(ticket, dto);

        if (ticket.getStatus() != null) {
            switch (ticket.getStatus()) {
                case 1:
                    dto.setStatusText("待处理");
                    break;
                case 2:
                    dto.setStatusText("处理中");
                    break;
                case 3:
                    dto.setStatusText("已完成");
                    break;
                default:
                    dto.setStatusText("未知");
            }
        }

        return dto;
    }

    private TicketEntity buildFeedbackSnapshot(TicketEntity source) {
        TicketEntity snapshot = new TicketEntity();
        BeanUtils.copyProperties(source, snapshot);
        return snapshot;
    }
}
