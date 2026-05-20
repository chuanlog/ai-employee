package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserUpdateRequest;
import org.example.entity.UserEntity;

public interface UserService extends IService<UserEntity> {

    LoginResponse login(LoginRequest request);

    UserDTO createUser(UserCreateRequest request, Long operatorId, Object userRole);

    UserDTO updateUser(Long id, UserUpdateRequest request, Long operatorId, Object userRole);

    void deleteUser(Long id, Long operatorId, Object userRole);

    UserDTO getUserById(Long id);

    boolean isAdmin(Long userId);

    UserEntity getCurrentUser();
}