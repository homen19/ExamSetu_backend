package com.mocktestapp.controller;

import com.mocktestapp.dto.UserDto;
import com.mocktestapp.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints — Google OAuth2 login flow and token management")
public class AuthController {

    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Returns the authenticated user's profile. Requires valid Bearer JWT token."
    )
    public ResponseEntity<UserDto.UserResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserDto.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build());
    }

    @GetMapping("/login-url")
    @Operation(
            summary = "Get Google OAuth2 login URL",
            description = "Returns the URL to initiate Google OAuth2 login. For mobile apps, open this URL in a browser/WebView."
    )
    public ResponseEntity<?> getLoginUrl() {
        return ResponseEntity.ok(java.util.Map.of(
                "loginUrl", "/oauth2/authorization/google",
                "description", "Open this URL in a browser to initiate Google OAuth2 login. After login, you will be redirected to /auth/callback?token=<jwt>&role=<role>"
        ));
    }
}
