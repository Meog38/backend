package br.com.brainvest.api.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiModels {

    private ApiModels() {}

    public record CreateLearnerRequest(
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Size(max = 100) String objective) {}

    public record AuthRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Size(max = 100) String objective,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(min = 8, max = 100) String currentPassword,
            @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Size(max = 100) String objective) {}

    public record SubmitAnswerRequest(
            @NotNull UUID requestId,
            @NotBlank @Size(max = 20) String questionId,
            @NotBlank @Pattern(regexp = "[ABC]") String selectedOptionId) {}

    public record OptionView(String id, String text) {}

    public record QuestionView(
            String id,
            String context,
            String prompt,
            String concept,
            List<OptionView> options) {}

    public record LevelView(
            int id,
            String title,
            String emoji,
            String tone,
            String description,
            String missionTitle,
            String missionBrief,
            String missionSummary,
            List<QuestionView> questions) {}

    public record CurriculumResponse(int version, List<LevelView> levels) {}

    public record LearnerResponse(
            UUID id,
            String displayName,
            String objective,
            int xp,
            int gems,
            int lives,
            int streak,
            int activeLevelId,
            int currentQuestionIndex,
            Instant createdAt) {}

    public record UserResponse(
            UUID id,
            String email,
            String displayName,
            UUID learnerId) {}

    public record AuthResponse(
            String token,
            UserResponse user,
            ProgressResponse progress) {}

    public record MissionProgressResponse(
            int levelId,
            int completedQuestionCount,
            boolean completed,
            Instant completedAt) {}

    public record ProgressResponse(
            UUID learnerId,
            String displayName,
            String objective,
            int xp,
            int gems,
            int lives,
            int streak,
            List<Integer> completedMissionIds,
            Map<Integer, Integer> completedQuestionCounts,
            List<String> rewardedQuestionIds,
            Integer activeMissionId,
            int currentQuestionIndex,
            Map<Integer, MissionProgressResponse> missions) {}

    public record AnswerResponse(
            String questionId,
            boolean correct,
            String concept,
            String feedback,
            int xpAwarded,
            int gemsAwarded,
            int lives,
            int streak,
            int completedQuestionCount,
            boolean missionCompleted,
            boolean rewardsAlreadyClaimed) {}

    public record RestartMissionResponse(
            UUID learnerId,
            int activeLevelId,
            int currentQuestionIndex,
            int lives,
            int streak) {}

    public record ErrorResponse(String code, String message, Instant timestamp) {}
}
