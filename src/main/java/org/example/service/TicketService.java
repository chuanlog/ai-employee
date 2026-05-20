package org.example.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.TicketCreateRequest;
import org.example.dto.TicketDTO;
import org.example.dto.TicketHandleRequest;
import org.example.entity.TicketEntity;

public interface TicketService extends IService<TicketEntity> {

    TicketDTO createTicket(TicketCreateRequest request, Long userId, String username, String aiAnswer);

    TicketDTO getTicketById(Long id);

    IPage<TicketDTO> listTickets(Integer pageNum, Integer pageSize, Integer status, Long userId);

    TicketDTO handleTicket(Long id, TicketHandleRequest request, Long handlerId, String handlerName);

    void addToKnowledgeBase(Long ticketId);

    void deleteTicket(Long id);
}
