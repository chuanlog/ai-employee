package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.constant.UserConstants;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserUpdateRequest;
import org.example.entity.UserEntity;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.example.util.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            String sql = "SELECT id, username, password, role, status, email, phone, created_at, updated_at FROM sys_user WHERE username = ?";
            Map<String, Object> userMap = jdbcTemplate.queryForMap(sql, request.getUsername());
            
            String storedPassword = (String) userMap.get("password");
            // 明文密码比较
            if (!request.getPassword().equals(storedPassword)) {
                throw new RuntimeException("用户名或密码错误");
            }

            int status = ((Number) userMap.get("status")).intValue();
            if (status != UserConstants.STATUS_ENABLED) {
                throw new RuntimeException("用户已被禁用");
            }

            UserEntity user = new UserEntity();
            user.setId(((Number) userMap.get("id")).longValue());
            user.setUsername((String) userMap.get("username"));
            user.setPassword(storedPassword);
            
            Object roleObj = userMap.get("role");
            if (roleObj instanceof String) {
                String roleStr = (String) roleObj;
                if ("admin".equalsIgnoreCase(roleStr) || "1".equals(roleStr)) {
                    user.setRole("admin");
                } else {
                    user.setRole("user");
                }
            } else if (roleObj instanceof Number) {
                int roleInt = ((Number) roleObj).intValue();
                user.setRole(roleInt == 1 ? "admin" : "user");
            } else {
                user.setRole("user");
            }
            
            user.setStatus(status);
            user.setEmail((String) userMap.get("email"));
            user.setPhone((String) userMap.get("phone"));

            String token = jwtUtils.generateToken(user);
            UserDTO userDTO = convertToDTO(user);

            return new LoginResponse(token, userDTO);
            
        } catch (Exception e) {
            throw new RuntimeException("用户名或密码错误: " + e.getMessage());
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

    @Override
    @Transactional
    public UserDTO createUser(UserCreateRequest request, Long operatorId, Object userRole) {
        if (!isAdminRole(userRole)) {
            throw new RuntimeException("无权限创建用户");
        }

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, request.getUsername());
        if (count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // 明文存储
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        
        String roleStr = request.getRole();
        if (roleStr != null) {
            if ("1".equals(roleStr) || "admin".equalsIgnoreCase(roleStr)) {
                user.setRole("admin");
            } else {
                user.setRole("user");
            }
        } else {
            user.setRole("user");
        }
        
        user.setStatus(UserConstants.STATUS_ENABLED);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        save(user);
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserById(Long id) {
        UserEntity user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserUpdateRequest request, Long operatorId, Object userRole) {
        boolean isAdmin = isAdminRole(userRole);
        
        UserEntity user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!isAdmin && !id.equals(operatorId)) {
            throw new RuntimeException("无权限编辑其他用户");
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (isAdmin) {
            if (request.getRole() != null) {
                String roleStr = request.getRole();
                if ("1".equals(roleStr) || "admin".equalsIgnoreCase(roleStr)) {
                    user.setRole("admin");
                } else {
                    user.setRole("user");
                }
            }
            if (request.getStatus() != null) {
                user.setStatus(request.getStatus());
            }
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(request.getPassword()); // 明文存储
        }

        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        return convertToDTO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long operatorId, Object userRole) {
        if (!isAdminRole(userRole)) {
            throw new RuntimeException("无权限删除用户");
        }

        UserEntity user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if ("admin".equals(user.getRole())) {
            throw new RuntimeException("不能删除管理员");
        }

        removeById(id);
    }

    private UserDTO convertToDTO(UserEntity user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    @Override
    public boolean isAdmin(Long userId) {
        UserEntity user = getById(userId);
        return user != null && "admin".equals(user.getRole());
    }

    @Override
    public UserEntity getCurrentUser() {
        return null;
    }
}