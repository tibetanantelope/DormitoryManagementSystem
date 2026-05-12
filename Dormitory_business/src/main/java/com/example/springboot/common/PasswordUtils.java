package com.example.springboot.common;

import cn.hutool.crypto.digest.BCrypt;

public class PasswordUtils {

    private PasswordUtils() {
    }

    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return rawPassword;
        }
        if (isEncoded(rawPassword)) {
            return rawPassword;
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isEncoded(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }

    public static boolean isEncoded(String password) {
        return password != null && password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }
}
