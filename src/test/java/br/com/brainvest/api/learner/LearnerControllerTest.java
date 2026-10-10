package br.com.brainvest.api.learner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brainvest.api.api.ApiException;
import br.com.brainvest.api.auth.AuthService;
import br.com.brainvest.api.config.RateLimiterService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LearnerController.class)
class LearnerControllerTest {

    private static final UUID LEARNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LearnerService learnerService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @Test
    void rejectsLearnerProgressEndpointsWithoutAuthentication() throws Exception {
        when(authService.requireUser(any())).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Entre na sua conta para continuar."));

        mockMvc.perform(get("/api/v1/learners/{learnerId}/progress", LEARNER_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/learners/{learnerId}/profile", LEARNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Luisa","objective":"Estudar para a CPA"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/learners/{learnerId}/answers", LEARNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"22222222-2222-2222-2222-222222222222","questionId":"l1-q1","selectedOptionId":"A"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/learners/{learnerId}/missions/1/restart", LEARNER_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/learners/{learnerId}", LEARNER_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(learnerService);
    }

    @Test
    void rejectsCreatingRemoteLearnerWithoutAuthentication() throws Exception {
        when(authService.requireUser(any())).thenThrow(new ApiException(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Entre na sua conta para continuar."));

        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Luisa","objective":"Estudar para a CPA"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(learnerService);
    }
}
