package com.example.springboot.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 从当前请求中读取用户身份信息，供后端做权限判断。
 */

public class AuthContext {

    public static String getIdentity(HttpSession session) {
        String identity = getHeader("X-Identity");
        if (identity != null && !identity.isEmpty()) {
            return identity;
        }
        Object sessionIdentity = session == null ? null : session.getAttribute("Identity");
        return sessionIdentity == null ? null : sessionIdentity.toString();
    }

    public static boolean isAdmin(HttpSession session) {
        return "admin".equals(getIdentity(session));
    }

    public static String getUsername() {
        String username = getHeader("X-Username");
        return username == null || username.isEmpty() ? null : username;
    }

    public static Integer getDormBuildId(HttpSession session) {
        String headerDormBuildId = getHeader("X-DormBuild-Id");
        if (headerDormBuildId != null && !headerDormBuildId.isEmpty()) {
            try {
                return Integer.valueOf(headerDormBuildId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String getHeader(String name) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(name);
    }
}
