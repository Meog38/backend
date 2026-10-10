package br.com.brainvest.api.learner;

import br.com.brainvest.api.auth.AuthService;
import br.com.brainvest.api.api.ApiModels.CreateLearnerRequest;
import br.com.brainvest.api.api.ApiModels.LearnerResponse;
import br.com.brainvest.api.api.ApiModels.ProgressResponse;
import br.com.brainvest.api.api.ApiModels.RestartMissionResponse;
import br.com.brainvest.api.api.ApiModels.SubmitAnswerRequest;
import br.com.brainvest.api.api.ApiModels.UpdateProfileRequest;
import br.com.brainvest.api.api.ApiModels.AnswerResponse;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthService authService;

    public LearnerController(LearnerService learnerService, AuthService authService) {
        this.learnerService = learnerService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<LearnerResponse> createLearner(
            @Valid @RequestBody CreateLearnerRequest request,
            HttpServletRequest httpRequest) {
        AuthService.AuthenticatedUser user = authService.requireUser(httpRequest);
        LearnerResponse learner = learnerService.createForUser(request, user.id());
        return ResponseEntity.created(URI.create("/api/v1/learners/" + learner.id())).body(learner);
    }

    @DeleteMapping("/{learnerId}")
    public ResponseEntity<Void> deleteLearner(@PathVariable UUID learnerId, HttpServletRequest request) {
        AuthService.AuthenticatedUser user = authService.requireUser(request);
        learnerService.assertCanAccess(learnerId, user.id());
        learnerService.deleteLearner(learnerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{learnerId}/progress")
    public ProgressResponse getProgress(@PathVariable UUID learnerId, HttpServletRequest request) {
        AuthService.AuthenticatedUser user = authService.requireUser(request);
        learnerService.assertCanAccess(learnerId, user.id());
        return learnerService.getProgress(learnerId);
    }

    @PatchMapping("/{learnerId}/profile")
    public LearnerResponse updateProfile(
            @PathVariable UUID learnerId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateProfileRequest request) {
        AuthService.AuthenticatedUser user = authService.requireUser(httpRequest);
        learnerService.assertCanAccess(learnerId, user.id());
        return learnerService.updateProfile(learnerId, request);
    }

    @PostMapping("/{learnerId}/answers")
    public AnswerResponse submitAnswer(
            @PathVariable UUID learnerId,
            HttpServletRequest httpRequest,
            @Valid @RequestBody SubmitAnswerRequest request) {
        AuthService.AuthenticatedUser user = authService.requireUser(httpRequest);
        learnerService.assertCanAccess(learnerId, user.id());
        return learnerService.submitAnswer(learnerId, request);
    }

    @PostMapping("/{learnerId}/missions/{levelId}/restart")
    public RestartMissionResponse restartMission(
            @PathVariable UUID learnerId,
            @PathVariable int levelId,
            HttpServletRequest request) {
        AuthService.AuthenticatedUser user = authService.requireUser(request);
        learnerService.assertCanAccess(learnerId, user.id());
        return learnerService.restartMission(learnerId, levelId);
    }
}
