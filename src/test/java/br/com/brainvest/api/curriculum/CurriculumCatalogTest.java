package br.com.brainvest.api.curriculum;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.brainvest.api.api.ApiModels.CurriculumResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurriculumCatalogTest {

    private final CurriculumCatalog catalog = new CurriculumCatalog();

    @Test
    void exposesFiveLevelsAndTwentyFiveQuestions() {
        assertThat(catalog.levels()).hasSize(5);
        assertThat(catalog.levels()).allSatisfy(level -> assertThat(level.questions()).hasSize(5));
        assertThat(catalog.levels().stream().flatMap(level -> level.questions().stream()).map(CurriculumCatalog.Question::id))
                .doesNotHaveDuplicates();
    }

    @Test
    void hasValidAnswerKeysAndThreeChoicesForEveryQuestion() {
        catalog.levels().stream().flatMap(level -> level.questions().stream()).forEach(question -> {
            assertThat(question.options()).hasSize(3);
            assertThat(question.options().stream().map(CurriculumCatalog.Option::id))
                    .contains(question.correctOptionId());
            assertThat(question.correctFeedback()).isNotBlank();
            assertThat(question.incorrectFeedback()).isNotBlank();
        });
    }

    @Test
    void publicCurriculumDoesNotExposeAnswerKeys() throws Exception {
        CurriculumResponse response = new CurriculumResponse(1, catalog.publicLevels());
        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json).contains("l1-q1");
        assertThat(json).doesNotContain("correctOptionId", "correctFeedback", "incorrectFeedback");
    }

    @Test
    void correctChoicesAreDistributedAcrossABC() {
        catalog.levels().forEach(level -> {
            Set<String> positions = level.questions().stream()
                    .map(CurriculumCatalog.Question::correctOptionId)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(positions).containsExactlyInAnyOrder("A", "B", "C");
        });
    }
}