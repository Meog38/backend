package br.com.brainvest.api.auth;

import br.com.brainvest.api.api.ApiModels.AuthRequest;
import br.com.brainvest.api.api.ApiModels.AuthResponse;
import br.com.brainvest.api.api.ApiModels.ChangePasswordRequest;
import br.com.brainvest.api.api.ApiModels.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
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
        auth.changePassword(request, body);
        return ResponseEntity.noContent().build();
    }
}
