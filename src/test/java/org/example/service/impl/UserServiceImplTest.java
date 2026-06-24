package org.example.service.impl;

import org.example.constant.UserConstants;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserCreateRequest;
import org.example.dto.UserDTO;
import org.example.dto.UserUpdateRequest;
import org.example.entity.UserEntity;
import org.example.util.JwtUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

class UserServiceImplTest {

    private JwtUtils jwtUtils;
    private JdbcTemplate jdbcTemplate;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        jwtUtils = Mockito.mock(JwtUtils.class);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        userService = Mockito.spy(new UserServiceImpl());
        ReflectionTestUtils.setField(userService, "jwtUtils", jwtUtils);
        ReflectionTestUtils.setField(userService, "jdbcTemplate", jdbcTemplate);
    }

    @Test
    void shouldLoginSuccessfully_whenPasswordAndStatusValid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        Mockito.when(jdbcTemplate.queryForMap(Mockito.anyString(), Mockito.eq("admin"))).thenReturn(adminUserMap());
        Mockito.when(jwtUtils.generateToken(Mockito.any(UserEntity.class))).thenReturn("token-value");

        LoginResponse response = userService.login(request);

        Assertions.assertEquals("token-value", response.getToken(), "登录成功应返回 JWT");
        Assertions.assertEquals("admin", response.getUser().getUsername(), "响应用户信息应来自数据库记录");
        Assertions.assertEquals("admin", response.getUser().getRole(), "数字角色 1 应转换为 admin");
    }

    @Test
    void shouldRejectLogin_whenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");
        Mockito.when(jdbcTemplate.queryForMap(Mockito.anyString(), Mockito.eq("admin"))).thenReturn(adminUserMap());

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> userService.login(request));

        Assertions.assertTrue(exception.getMessage().contains("用户名或密码错误"), "密码错误应返回统一登录错误");
    }

    @Test
    void shouldCreateUser_whenOperatorIsAdmin() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("new-user");
        request.setPassword("pwd");
        request.setEmail("new@example.com");
        request.setPhone("123");
        request.setRole("1");
        Mockito.doReturn(0L).when(userService).count(Mockito.any());
        Mockito.doReturn(true).when(userService).save(Mockito.any(UserEntity.class));

        UserDTO dto = userService.createUser(request, 1L, "admin");

        Assertions.assertEquals("new-user", dto.getUsername(), "应返回新建用户名称");
        Assertions.assertEquals("admin", dto.getRole(), "角色 1 应转换为 admin");
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        Mockito.verify(userService).save(captor.capture());
        Assertions.assertEquals(UserConstants.STATUS_ENABLED, captor.getValue().getStatus(), "新用户默认启用");
    }

    @Test
    void shouldRejectCreateUser_whenOperatorIsNotAdmin() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("new-user");

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> userService.createUser(request, 2L, "user"));

        Assertions.assertEquals("无权限创建用户", exception.getMessage(), "非管理员不能创建用户");
        Mockito.verify(userService, Mockito.never()).save(Mockito.any(UserEntity.class));
    }

    @Test
    void shouldUpdateOwnBasicProfile_whenOperatorIsOwner() {
        UserEntity existing = new UserEntity();
        existing.setId(2L);
        existing.setUsername("user");
        existing.setRole("user");
        existing.setStatus(UserConstants.STATUS_ENABLED);
        Mockito.doReturn(existing).when(userService).getById(2L);
        Mockito.doReturn(true).when(userService).updateById(Mockito.any(UserEntity.class));

        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("updated@example.com");
        request.setRole("admin");

        UserDTO dto = userService.updateUser(2L, request, 2L, "user");

        Assertions.assertEquals("updated@example.com", dto.getEmail(), "用户本人可更新基础资料");
        Assertions.assertEquals("user", dto.getRole(), "非管理员更新时不应修改角色");
    }

    @Test
    void shouldRejectDeleteAdminUser() {
        UserEntity existing = new UserEntity();
        existing.setId(1L);
        existing.setRole("admin");
        Mockito.doReturn(existing).when(userService).getById(1L);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> userService.deleteUser(1L, 99L, "admin"));

        Assertions.assertEquals("不能删除管理员", exception.getMessage(), "管理员账号不允许被删除");
        Mockito.verify(userService, Mockito.never()).removeById(Mockito.any(Long.class));
    }

    private Map<String, Object> adminUserMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1L);
        map.put("username", "admin");
        map.put("password", "123456");
        map.put("role", 1);
        map.put("status", UserConstants.STATUS_ENABLED);
        map.put("email", "admin@example.com");
        map.put("phone", "18800000000");
        return map;
    }
}
