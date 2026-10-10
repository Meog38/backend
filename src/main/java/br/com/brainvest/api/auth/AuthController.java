package br.com.brainvest.api.auth;

import br.com.brainvest.api.api.ApiModels.AuthRequest;
import br.com.brainvest.api.api.ApiModels.AuthResponse;
import br.com.brainvest.api.api.ApiModels.ChangePasswordRequest;
import br.com.brainvest.api.api.ApiModels.ForgotPasswordRequest;
import br.com.brainvest.api.api.ApiModels.RegisterRequest;
import br.com.brainvest.api.api.ApiModels.ResetPasswordRequest;
import br.com.brainvest.api.config.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final RateLimiterService rateLimiter;

    public AuthController(AuthService auth, RateLimiterService rateLimiter) {
        this.auth = auth;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check("auth-register-ip", clientIp(httpRequest), 5, Duration.ofHours(1));
        rateLimiter.check("auth-register-email", request.email(), 3, Duration.ofHours(1));
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check("auth-login-ip", clientIp(httpRequest), 30, Duration.ofMinutes(10));
        rateLimiter.check("auth-login-email", request.email(), 10, Duration.ofMinutes(10));
        return auth.login(request);
    }

    @GetMapping("/me")
    public AuthResponse me(HttpServletRequest request) {
        return auth.me(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest body) {
        rateLimiter.check("auth-password-change", clientIp(request), 10, Duration.ofMinutes(10));
        auth.changePassword(request, body);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(HttpServletRequest request, @Valid @RequestBody ForgotPasswordRequest body) {
        rateLimiter.check("auth-password-forgot-ip", clientIp(request), 5, Duration.ofHours(1));
        rateLimiter.check("auth-password-forgot-email", body.email(), 3, Duration.ofHours(1));
        auth.requestPasswordReset(body);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(HttpServletRequest request, @Valid @RequestBody ResetPasswordRequest body) {
        rateLimiter.check("auth-password-reset", clientIp(request), 10, Duration.ofHours(1));
        auth.resetPassword(body);
        return ResponseEntity.noContent().build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
