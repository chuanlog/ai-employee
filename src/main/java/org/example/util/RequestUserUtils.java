package org.example.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUserUtils {

    private RequestUserUtils() {
    }

    public static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    public static String getUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username instanceof String ? (String) username : "unknown";
    }

    public static boolean isAdminRole(Object role) {
        if (role == null) {
            return false;
        }
        if (role instanceof String roleStr) {
            return "admin".equalsIgnoreCase(roleStr) || "1".equals(roleStr);
        }
        if (role instanceof Number number) {
            return number.intValue() == 1;
        }
        return false;
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!isAdminRole(request.getAttribute("userRole"))) {
            throw new RuntimeException("仅管理员可操作知识库");
        }
    }
}
