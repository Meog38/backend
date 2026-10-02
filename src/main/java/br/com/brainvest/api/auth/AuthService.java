package br.com.brainvest.api.auth;

import br.com.brainvest.api.api.ApiException;
import br.com.brainvest.api.api.ApiModels.AuthRequest;
import br.com.brainvest.api.api.ApiModels.AuthResponse;
import br.com.brainvest.api.api.ApiModels.CreateLearnerRequest;
import br.com.brainvest.api.api.ApiModels.RegisterRequest;
import br.com.brainvest.api.api.ApiModels.UserResponse;
import br.com.brainvest.api.learner.LearnerService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int SESSION_DAYS = 30;

    private final JdbcTemplate jdbc;
    private final LearnerService learners;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, LearnerService learners) {
        this.jdbc = jdbc;
        this.learners = learners;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM app_users WHERE email = ?", Integer.class, email);
        if (existing != null && existing > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Este e-mail ja esta cadastrado.");
        }

        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO app_users (id, email, password_hash, display_name)
                VALUES (?, ?, ?, ?)
                """, userId, email, passwordEncoder.encode(request.password()), request.displayName().trim());

        UUID learnerId = learners.createForUser(
                new CreateLearnerRequest(request.displayName(), request.objective()), userId).id();
        String token = createSession(userId);
        return authResponse(token, userId, learnerId);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String email = normalizeEmail(request.email());
        UserCredentials user = jdbc.query("""
                SELECT u.id, u.email, u.display_name, u.password_hash, l.id AS learner_id
                FROM app_users u
                LEFT JOIN learners l ON l.user_id = u.id
                WHERE u.email = ?
                """, (result, rowNumber) -> new UserCredentials(
                result.getObject("id", UUID.class),
                result.getString("email"),
                result.getString("display_name"),
                result.getString("password_hash"),
                result.getObject("learner_id", UUID.class)), email).stream().findFirst()
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw invalidCredentials();
        }
        String token = createSession(user.id());
        return authResponse(token, user.id(), user.learnerId());
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) return Optional.empty();
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) return Optional.empty();
        String tokenHash = sha256(token);
        return jdbc.query("""
                SELECT u.id, u.email, u.display_name, l.id AS learner_id
                FROM auth_sessions s
                JOIN app_users u ON u.id = s.user_id
                LEFT JOIN learners l ON l.user_id = u.id
                WHERE s.token_hash = ? AND s.expires_at > now()
                """, (result, rowNumber) -> new AuthenticatedUser(
                result.getObject("id", UUID.class),
                result.getString("email"),
                result.getString("display_name"),
                result.getObject("learner_id", UUID.class)), tokenHash).stream().findFirst();
    }

    @Transactional(readOnly = true)
    public AuthResponse me(HttpServletRequest request) {
        AuthenticatedUser user = authenticate(request).orElseThrow(AuthService::unauthorized);
        return new AuthResponse("", userResponse(user), learners.getProgress(user.learnerId()));
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) return;
        jdbc.update("DELETE FROM auth_sessions WHERE token_hash = ?", sha256(authorization.substring("Bearer ".length()).trim()));
    }

    public AuthenticatedUser requireUser(HttpServletRequest request) {
        return authenticate(request).orElseThrow(AuthService::unauthorized);
    }

    private AuthResponse authResponse(String token, UUID userId, UUID learnerId) {
        AuthenticatedUser user = jdbc.query("""
                SELECT u.id, u.email, u.display_name, l.id AS learner_id
                FROM app_users u
                JOIN learners l ON l.user_id = u.id
                WHERE u.id = ?
                """, (result, rowNumber) -> new AuthenticatedUser(
                result.getObject("id", UUID.class),
                result.getString("email"),
                result.getString("display_name"),
                result.getObject("learner_id", UUID.class)), userId).stream().findFirst()
                .orElse(new AuthenticatedUser(userId, "", "", learnerId));
        return new AuthResponse(token, userResponse(user), learners.getProgress(user.learnerId()));
    }

    private UserResponse userResponse(AuthenticatedUser user) {
        return new UserResponse(user.id(), user.email(), user.displayName(), user.learnerId());
    }

    private String createSession(UUID userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("""
                INSERT INTO auth_sessions (id, user_id, token_hash, expires_at)
                VALUES (?, ?, ?, ?)
                """, UUID.randomUUID(), userId, sha256(token), OffsetDateTime.now().plusDays(SESSION_DAYS));
        return token;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail ou senha invalidos.");
    }

    private static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Entre na sua conta para continuar.");
    }

    public record AuthenticatedUser(UUID id, String email, String displayName, UUID learnerId) {}

    private record UserCredentials(UUID id, String email, String displayName, String passwordHash, UUID learnerId) {}
}
