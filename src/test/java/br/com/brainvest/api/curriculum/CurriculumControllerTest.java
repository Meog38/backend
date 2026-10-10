package br.com.brainvest.api.curriculum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.brainvest.api.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurriculumController.class)
@Import({CurriculumCatalog.class, WebConfig.class})
@TestPropertySource(properties = "brainvest.cors.allowed-origins=https://pixel-perfect-snap-5512.lovable.app")
class CurriculumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsFiveLevelsAndDoesNotExposeCorrectAnswers() throws Exception {
        mockMvc.perform(get("/api/v1/curriculum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.levels.length()").value(5))
                .andExpect(jsonPath("$.levels[0].questions.length()").value(20))
                .andExpect(jsonPath("$.levels[4].questions.length()").value(20))
                .andExpect(jsonPath("$.levels[0].questions[0].correctOptionId").doesNotExist())
                .andExpect(jsonPath("$.levels[0].questions[0].options.length()").value(3));
    }

    @Test
    void allowsLocalFrontendCorsPreflightAlongsideConfiguredProductionOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/curriculum")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }
}
