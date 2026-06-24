package org.example.util;

import io.jsonwebtoken.Claims;
import org.example.entity.UserEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret",
                "unit-test-secret-key-must-be-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwtUtils, "expire", 60_000L);
    }

    @Test
    void shouldGenerateAndParseToken() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setUsername("admin");
        user.setRole("admin");

        String token = jwtUtils.generateToken(user);
        Claims claims = jwtUtils.parseToken(token);

        Assertions.assertEquals("7", claims.getSubject(), "subject 应保存用户 ID");
        Assertions.assertEquals("admin", claims.get("username", String.class), "应保存用户名 claim");
        Assertions.assertEquals("admin", claims.get("role", String.class), "应保存角色 claim");
    }

    @Test
    void shouldReadUserIdFromToken() {
        UserEntity user = new UserEntity();
        user.setId(8L);
        user.setUsername("bob");
        user.setRole("user");

        String token = jwtUtils.generateToken(user);

        Assertions.assertEquals(8L, jwtUtils.getUserIdFromToken(token), "应从 token subject 解析用户 ID");
    }

    @Test
    void shouldRejectInvalidToken() {
        Assertions.assertThrows(RuntimeException.class, () -> jwtUtils.parseToken("invalid-token"),
                "非法 token 应被 JWT 解析器拒绝");
    }
}
