package br.com.brainvest.api.learner;

import br.com.brainvest.api.api.ApiModels.CreateLearnerRequest;
import br.com.brainvest.api.api.ApiModels.LearnerResponse;
import br.com.brainvest.api.api.ApiModels.ProgressResponse;
import br.com.brainvest.api.api.ApiModels.RestartMissionResponse;
import br.com.brainvest.api.api.ApiModels.SubmitAnswerRequest;
import br.com.brainvest.api.api.ApiModels.UpdateProfileRequest;
import br.com.brainvest.api.api.ApiModels.AnswerResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learners")
public class LearnerController {

    private final LearnerService learnerService;

    public LearnerController(LearnerService learnerService) {
        this.learnerService = learnerService;
    }

    @PostMapping
    public ResponseEntity<LearnerResponse> createLearner(@Valid @RequestBody CreateLearnerRequest request) {
        LearnerResponse learner = learnerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/learners/" + learner.id())).body(learner);
    }

    @DeleteMapping("/{learnerId}")
    public ResponseEntity<Void> deleteLearner(@PathVariable UUID learnerId) {
        learnerService.deleteLearner(learnerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{learnerId}/progress")
    public ProgressResponse getProgress(@PathVariable UUID learnerId) {
        return learnerService.getProgress(learnerId);
    }

    @PatchMapping("/{learnerId}/profile")
    public LearnerResponse updateProfile(
            @PathVariable UUID learnerId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return learnerService.updateProfile(learnerId, request);
    }

    @PostMapping("/{learnerId}/answers")
    public AnswerResponse submitAnswer(
            @PathVariable UUID learnerId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return learnerService.submitAnswer(learnerId, request);
    }

    @PostMapping("/{learnerId}/missions/{levelId}/restart")
    public RestartMissionResponse restartMission(
            @PathVariable UUID learnerId,
            @PathVariable int levelId) {
        return learnerService.restartMission(learnerId, levelId);
    }
}