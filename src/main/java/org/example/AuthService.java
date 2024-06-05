package org.example;

import java.util.HashSet;
import java.util.Set;

public class AuthService {
    private static final Set<String> authenticatedTokens = new HashSet<>();
    private static final Set<String> authorizedTokens = new HashSet<>();

    // Добавляем метод для регистрации токенов
    public static void registerToken(String token, boolean isAdmin) {
        authenticatedTokens.add(token);
        if (isAdmin) {
            authorizedTokens.add(token);
        }
    }

    public static boolean isAuthenticated(String token) {
        return authenticatedTokens.contains(token);
    }

    public static boolean isAuthorized(String token) {
        return authorizedTokens.contains(token);
    }

    public static boolean isAdmin(String token) {
        return authorizedTokens.contains(token);
    }
}
