package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserUpdateRequest;
import org.example.entity.UserEntity;
import org.example.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long getOperatorId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
            return ResponseEntity.ok("Database connected, user count: " + count);
        } catch (Exception e) {
            return ResponseEntity.ok("Database error: " + e.getMessage());
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugUser() {
        try {
            String sql = "SELECT * FROM sys_user WHERE username = 'admin'";
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            if (result.isEmpty()) {
                return ResponseEntity.ok("No user found");
            }
            Map<String, Object> user = result.get(0);
            Map<String, Object> info = new HashMap<>();
            info.put("data", user);
            info.put("role_type", user.get("role") != null ? user.get("role").getClass().getName() : "null");
            info.put("role_value", user.get("role"));
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.ok(error);
        }
    }

    @PostMapping("/updatepass")
    public ResponseEntity<String> updatePassword() {
        try {
            String sql = "UPDATE sys_user SET password = '123456' WHERE username = 'admin'";
            int rows = jdbcTemplate.update(sql);
            return ResponseEntity.ok("Password updated, rows affected: " + rows);
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserCreateRequest request,
                                              HttpServletRequest httpRequest) {
        Long operatorId = getOperatorId(httpRequest);
        Object userRole = httpRequest.getAttribute("userRole");
        UserDTO user = userService.createUser(request, operatorId, userRole);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/fixRole")
    public ResponseEntity<String> fixRoleData() {
        try {
            String sql = "UPDATE sys_user SET role = CASE role WHEN 'admin' THEN 1 WHEN 'user' THEN 2 ELSE 2 END";
            int rows = jdbcTemplate.update(sql);
            return ResponseEntity.ok("Role data fixed, rows affected: " + rows);
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listUsers(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         HttpServletRequest httpRequest) {
        try {
            Long operatorId = getOperatorId(httpRequest);
            Object userRole = httpRequest.getAttribute("userRole");
            boolean isAdmin = isAdminRole(userRole);
            
            String sql;
            String countSql;
            List<Object> params = new java.util.ArrayList<>();
            
            if (isAdmin) {
                sql = "SELECT id, username, email, phone, role, status, created_at, updated_at FROM sys_user ORDER BY created_at DESC LIMIT ?, ?";
                countSql = "SELECT COUNT(*) FROM sys_user";
                params.add((pageNum - 1) * pageSize);
                params.add(pageSize);
            } else {
                sql = "SELECT id, username, email, phone, role, status, created_at, updated_at FROM sys_user WHERE id = ? ORDER BY created_at DESC LIMIT ?, ?";
                countSql = "SELECT COUNT(*) FROM sys_user WHERE id = ?";
                params.add(operatorId);
                params.add((pageNum - 1) * pageSize);
                params.add(pageSize);
            }
            
            List<Map<String, Object>> userList = jdbcTemplate.queryForList(sql, params.toArray());
            
            Integer total;
            if (isAdmin) {
                total = jdbcTemplate.queryForObject(countSql, Integer.class);
            } else {
                total = jdbcTemplate.queryForObject(countSql, Integer.class, operatorId);
            }

            List<Map<String, Object>> dtoList = userList.stream().map(map -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", ((Number) map.get("id")).longValue());
                dto.put("username", map.get("username"));
                dto.put("email", map.get("email"));
                dto.put("phone", map.get("phone"));
                dto.put("role", map.get("role") != null ? map.get("role").toString() : "user");
                dto.put("status", ((Number) map.get("status")).intValue());
                dto.put("createdAt", map.get("created_at") != null ? map.get("created_at").toString() : null);
                dto.put("updatedAt", map.get("updated_at") != null ? map.get("updated_at").toString() : null);
                return dto;
            }).toList();

            Map<String, Object> result = new HashMap<>();
            result.put("records", dtoList);
            result.put("total", total);
            result.put("size", pageSize);
            result.put("current", pageNum);
            result.put("pages", (int) Math.ceil((double) total / pageSize));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private boolean isAdminRole(Object role) {
        if (role == null) return false;
        if (role instanceof String) {
            return "admin".equals(role) || "1".equals(role);
        }
        if (role instanceof Number) {
            return ((Number) role).intValue() == 1;
        }
        return false;
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @RequestBody UserUpdateRequest request,
                                              HttpServletRequest httpRequest) {
        Long operatorId = getOperatorId(httpRequest);
        Object userRole = httpRequest.getAttribute("userRole");
        UserDTO user = userService.updateUser(id, request, operatorId, userRole);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           HttpServletRequest httpRequest) {
        Long operatorId = getOperatorId(httpRequest);
        Object userRole = httpRequest.getAttribute("userRole");
        userService.deleteUser(id, operatorId, userRole);
        return ResponseEntity.noContent().build();
    }
}