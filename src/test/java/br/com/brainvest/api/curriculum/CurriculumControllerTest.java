package br.com.brainvest.api.curriculum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CurriculumController.class)
@Import(CurriculumCatalog.class)
@TestPropertySource(properties = "brainvest.cors.allowed-origins=http://localhost:5173")
class CurriculumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsFiveLevelsAndDoesNotExposeCorrectAnswers() throws Exception {
        mockMvc.perform(get("/api/v1/curriculum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.levels.length()").value(5))
                .andExpect(jsonPath("$.levels[0].questions.length()").value(5))
                .andExpect(jsonPath("$.levels[0].questions[0].correctOptionId").doesNotExist())
                .andExpect(jsonPath("$.levels[0].questions[0].options.length()").value(3));
    }
}