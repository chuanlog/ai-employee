package org.example.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RequestUserUtilsTest {

    @Test
    void shouldReadUserAttributes_whenTypesAreValid() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute("userId")).thenReturn(100L);
        Mockito.when(request.getAttribute("username")).thenReturn("alice");

        Assertions.assertEquals(100L, RequestUserUtils.getUserId(request), "应读取 Long 类型用户 ID");
        Assertions.assertEquals("alice", RequestUserUtils.getUsername(request), "应读取 String 类型用户名");
    }

    @Test
    void shouldReturnDefaults_whenAttributeTypesAreInvalid() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute("userId")).thenReturn("100");
        Mockito.when(request.getAttribute("username")).thenReturn(100L);

        Assertions.assertNull(RequestUserUtils.getUserId(request), "非 Long 类型用户 ID 应返回 null");
        Assertions.assertEquals("unknown", RequestUserUtils.getUsername(request), "非 String 类型用户名应返回默认值");
    }

    @Test
    void shouldRecognizeAdminRoleFromStringAndNumber() {
        Assertions.assertTrue(RequestUserUtils.isAdminRole("admin"), "admin 字符串应识别为管理员");
        Assertions.assertTrue(RequestUserUtils.isAdminRole("1"), "字符串 1 应识别为管理员");
        Assertions.assertTrue(RequestUserUtils.isAdminRole(1), "数字 1 应识别为管理员");
        Assertions.assertFalse(RequestUserUtils.isAdminRole("user"), "普通用户不应识别为管理员");
    }

    @Test
    void shouldThrowException_whenAdminRequiredButRoleIsNotAdmin() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute("userRole")).thenReturn("user");

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> RequestUserUtils.requireAdmin(request));

        Assertions.assertEquals("仅管理员可操作知识库", exception.getMessage(), "非管理员应被拒绝");
    }
}
