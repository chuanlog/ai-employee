package org.example.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.TicketCreateRequest;
import org.example.dto.TicketDTO;
import org.example.dto.TicketHandleRequest;
import org.example.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    private Long getOperatorId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private String getUsername(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return username != null ? username : "unknown";
    }

    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(@RequestBody TicketCreateRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = getOperatorId(httpRequest);
        String username = getUsername(httpRequest);
        TicketDTO ticket = ticketService.createTicket(request, userId, username, request.getAiAnswer());
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        TicketDTO ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<IPage<TicketDTO>> listTickets(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) Long userId) {
        IPage<TicketDTO> page = ticketService.listTickets(pageNum, pageSize, status, userId);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}/handle")
    public ResponseEntity<TicketDTO> handleTicket(@PathVariable Long id,
                                                    @RequestBody TicketHandleRequest request,
                                                    HttpServletRequest httpRequest) {
        Long handlerId = getOperatorId(httpRequest);
        String handlerName = getUsername(httpRequest);
        TicketDTO ticket = ticketService.handleTicket(id, request, handlerId, handlerName);
        return ResponseEntity.ok(ticket);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.ok().build();
    }
}
