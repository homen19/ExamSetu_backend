package com.mocktestapp.config;

import com.mocktestapp.entity.User;
import com.mocktestapp.repository.UserRepository;
import com.mocktestapp.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .googleId(googleId)
                    .profilePicture(picture)
                    .role(User.Role.USER)
                    .build();
            return userRepository.save(newUser);
        });

        // Update Google ID and picture if changed
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
        }
        if (picture != null && !picture.equals(user.getProfilePicture())) {
            user.setProfilePicture(picture);
        }
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", token)
                .queryParam("refreshToken", refreshToken)
                .queryParam("role", user.getRole().name())
                .build().toUriString();

        log.info("OAuth2 login success for user: {}", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
