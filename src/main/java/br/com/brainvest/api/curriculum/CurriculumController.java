package br.com.brainvest.api.curriculum;

import br.com.brainvest.api.api.ApiModels.CurriculumResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/curriculum")
public class CurriculumController {

    private final CurriculumCatalog curriculum;

    public CurriculumController(CurriculumCatalog curriculum) {
        this.curriculum = curriculum;
    }

    @GetMapping
    @Operation(summary = "Lista níveis, missões e perguntas sem expor as respostas corretas")
    public CurriculumResponse getCurriculum() {
        List<br.com.brainvest.api.api.ApiModels.LevelView> levels = curriculum.publicLevels();
        return new CurriculumResponse(1, levels);
    }
}