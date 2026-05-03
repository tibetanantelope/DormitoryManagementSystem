package com.example.springboot.common;

import com.example.springboot.entity.DormManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

    public static Integer getDormBuildId(HttpSession session) {
        String headerDormBuildId = getHeader("X-DormBuild-Id");
        if (headerDormBuildId != null && !headerDormBuildId.isEmpty()) {
            try {
                return Integer.valueOf(headerDormBuildId);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        Object user = session == null ? null : session.getAttribute("User");
        if (user instanceof DormManager) {
            return ((DormManager) user).getDormBuildId();
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
