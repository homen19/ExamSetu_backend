package com.mocktestapp.dto;

import com.mocktestapp.entity.User;
import lombok.*;

import java.time.LocalDateTime;

public class UserDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String profilePicture;
        private User.Role role;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
        private UserResponse user;
    }
}
