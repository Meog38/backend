package br.com.brainvest.api.learner;

import br.com.brainvest.api.api.ApiException;
import br.com.brainvest.api.api.ApiModels.AnswerResponse;
import br.com.brainvest.api.api.ApiModels.CreateLearnerRequest;
import br.com.brainvest.api.api.ApiModels.LearnerResponse;
import br.com.brainvest.api.api.ApiModels.MissionProgressResponse;
import br.com.brainvest.api.api.ApiModels.ProgressResponse;
import br.com.brainvest.api.api.ApiModels.RestartMissionResponse;
import br.com.brainvest.api.api.ApiModels.SubmitAnswerRequest;
import br.com.brainvest.api.api.ApiModels.UpdateProfileRequest;
import br.com.brainvest.api.curriculum.CurriculumCatalog;
import br.com.brainvest.api.curriculum.CurriculumCatalog.Level;
import br.com.brainvest.api.curriculum.CurriculumCatalog.Question;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerService {

    private static final int STARTER_LIVES = 3;
    private static final int XP_PER_NEW_CORRECT_ANSWER = 20;
    private static final int GEMS_PER_NEW_MISSION = 15;

    private static final RowMapper<LearnerState> LEARNER_MAPPER = (result, rowNumber) -> new LearnerState(
            result.getObject("id", UUID.class),
            result.getString("display_name"),
            result.getString("objective"),
            result.getInt("xp"),
            result.getInt("gems"),
            result.getInt("lives"),
            result.getInt("streak"),
            result.getInt("active_level_id"),
            result.getInt("current_question_index"),
            result.getObject("created_at", OffsetDateTime.class).toInstant());

    private final JdbcTemplate jdbc;
    private final CurriculumCatalog curriculum;

    public LearnerService(JdbcTemplate jdbc, CurriculumCatalog curriculum) {
        this.jdbc = jdbc;
        this.curriculum = curriculum;
    }

    @Transactional
    public LearnerResponse create(CreateLearnerRequest request) {
        return createForUser(request, null);
    }

    @Transactional
    public LearnerResponse createForUser(CreateLearnerRequest request, UUID userId) {
        UUID learnerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO learners (id, display_name, objective, user_id)
                VALUES (?, ?, ?, ?)
                """, learnerId, request.displayName().trim(), request.objective().trim(), userId);
        jdbc.batchUpdate("""
                INSERT INTO mission_progress (learner_id, level_id)
                VALUES (?, ?)
                """, curriculum.levels(), curriculum.levels().size(), (statement, level) -> {
                    statement.setObject(1, learnerId);
                    statement.setInt(2, level.id());
                });
        return response(loadLearner(learnerId, false));
    }

    @Transactional
    public void attachGuestLearner(UUID learnerId, UUID userId) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM learners WHERE user_id = ?", Integer.class, userId);
        if (existing != null && existing > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_ALREADY_HAS_PROGRESS", "Esta conta ja possui progresso salvo.");
        }
        int changed = jdbc.update("""
                UPDATE learners
                SET user_id = ?, updated_at = now(), row_version = row_version + 1
                WHERE id = ? AND user_id IS NULL
                """, userId, learnerId);
        if (changed == 0) throw learnerNotFound();
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(UUID learnerId) {
        LearnerState learner = loadLearner(learnerId, false);
        Map<Integer, MissionProgressResponse> missions = new LinkedHashMap<>();
        Map<Integer, Integer> completedQuestionCounts = new LinkedHashMap<>();
        List<Integer> completedMissionIds = new ArrayList<>();
        jdbc.query("""
                SELECT level_id, completed_question_count, completed, completed_at
                FROM mission_progress
                WHERE learner_id = ?
                ORDER BY level_id
                """, result -> {
                    OffsetDateTime completedAt = result.getObject("completed_at", OffsetDateTime.class);
                    missions.put(result.getInt("level_id"), new MissionProgressResponse(
                            result.getInt("level_id"),
                            result.getInt("completed_question_count"),
                            result.getBoolean("completed"),
                            completedAt == null ? null : completedAt.toInstant()));
                        completedQuestionCounts.put(result.getInt("level_id"), result.getInt("completed_question_count"));
                        if (result.getBoolean("completed")) completedMissionIds.add(result.getInt("level_id"));
                }, learnerId);
                List<String> rewardedQuestionIds = jdbc.queryForList("""
                    SELECT question_id FROM rewarded_questions WHERE learner_id = ? ORDER BY question_id
                    """, String.class, learnerId);
        return new ProgressResponse(learner.id(), learner.displayName(), learner.objective(), learner.xp(),
                    learner.gems(), learner.lives(), learner.streak(), completedMissionIds, completedQuestionCounts,
                    rewardedQuestionIds, learner.activeLevelId(), learner.currentQuestionIndex(), missions);
    }

    @Transactional
    public LearnerResponse updateProfile(UUID learnerId, UpdateProfileRequest request) {
        int changed = jdbc.update("""
                UPDATE learners
                SET display_name = ?, objective = ?, updated_at = now(), row_version = row_version + 1
                WHERE id = ?
                """, request.displayName().trim(), request.objective().trim(), learnerId);
        if (changed == 0) throw learnerNotFound();
        return response(loadLearner(learnerId, false));
    }

    @Transactional
    public void deleteLearner(UUID learnerId) {
        int deleted = jdbc.update("DELETE FROM learners WHERE id = ?", learnerId);
        if (deleted == 0) throw learnerNotFound();
    }

    @Transactional
    public AnswerResponse submitAnswer(UUID learnerId, SubmitAnswerRequest request) {
        LearnerState learner = loadLearner(learnerId, true);
        AnswerResponse previous = findPreviousAnswer(learnerId, request);
        if (previous != null) return previous;

        Question question = curriculum.question(request.questionId());
        int levelId = curriculum.levelIdFor(question.id());
        Level level = curriculum.level(levelId);
        boolean correct = question.correctOptionId().equals(request.selectedOptionId());
        boolean rewardedBefore = hasRewardedQuestion(learnerId, question.id());
        MissionState mission = loadMission(learnerId, levelId);
        boolean alreadyCompleted = mission.completed();

        if (!alreadyCompleted && levelId != learner.activeLevelId()) {
            throw new ApiException(HttpStatus.CONFLICT, "MISSION_LOCKED", "Conclua o nível anterior para acessar esta missão.");
        }
        if (!alreadyCompleted && learner.lives() == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "LIVES_EXHAUSTED", "Reinicie a missão antes de responder novamente.");
        }
        if (!alreadyCompleted && questionIndex(level, question.id()) != mission.completedQuestionCount()) {
            throw new ApiException(HttpStatus.CONFLICT, "QUESTION_OUT_OF_ORDER", "Responda às questões na ordem da missão.");
        }

        int xpAwarded = correct && !alreadyCompleted && !rewardedBefore ? XP_PER_NEW_CORRECT_ANSWER : 0;
        int nextQuestionCount = mission.completedQuestionCount();
        boolean missionCompleted = alreadyCompleted;
        int gemsAwarded = 0;
        int lives = learner.lives();
        int streak = correct ? learner.streak() + 1 : 0;

        if (correct && !alreadyCompleted) {
            nextQuestionCount++;
            if (xpAwarded > 0) {
                jdbc.update("""
                        INSERT INTO rewarded_questions (learner_id, question_id, xp_awarded)
                        VALUES (?, ?, ?)
                        ON CONFLICT (learner_id, question_id) DO NOTHING
                        """, learnerId, question.id(), xpAwarded);
            }
            missionCompleted = nextQuestionCount == level.questions().size();
            if (missionCompleted) {
                gemsAwarded = GEMS_PER_NEW_MISSION;
                jdbc.update("""
                        UPDATE mission_progress
                        SET completed_question_count = ?, completed = TRUE, completed_at = now()
                        WHERE learner_id = ? AND level_id = ?
                        """, nextQuestionCount, learnerId, levelId);
            } else {
                jdbc.update("""
                        UPDATE mission_progress SET completed_question_count = ?
                        WHERE learner_id = ? AND level_id = ?
                        """, nextQuestionCount, learnerId, levelId);
            }
        } else if (!correct && !alreadyCompleted) {
            lives = Math.max(0, learner.lives() - 1);
        }

        int nextLevelId = missionCompleted && !alreadyCompleted
                ? Math.min(levelId + 1, curriculum.levels().size())
                : learner.activeLevelId();
        int nextQuestionIndex = missionCompleted && !alreadyCompleted
                ? 0
                : correct && !alreadyCompleted ? nextQuestionCount : learner.currentQuestionIndex();

        jdbc.update("""
                UPDATE learners
                SET xp = xp + ?, gems = gems + ?, lives = ?, streak = ?,
                    active_level_id = ?, current_question_index = ?,
                    updated_at = now(), row_version = row_version + 1
                WHERE id = ?
                """, xpAwarded, gemsAwarded, lives, streak, nextLevelId, nextQuestionIndex, learnerId);

        String feedback = correct ? question.correctFeedback() : question.incorrectFeedback();
        AnswerResponse response = new AnswerResponse(question.id(), correct, question.concept(), feedback,
                xpAwarded, gemsAwarded, lives, streak, nextQuestionCount, missionCompleted,
                rewardedBefore || alreadyCompleted);
        saveAnswerRequest(learnerId, request, response);
        return response;
    }

    @Transactional
    public RestartMissionResponse restartMission(UUID learnerId, int levelId) {
        curriculum.level(levelId);
        LearnerState learner = loadLearner(learnerId, true);
        MissionState mission = loadMission(learnerId, levelId);
        if (mission.completed()) {
            throw new ApiException(HttpStatus.CONFLICT, "MISSION_ALREADY_COMPLETED", "Uma missão concluída pode ser revisada, mas não reiniciada.");
        }
        if (learner.activeLevelId() != levelId) {
            throw new ApiException(HttpStatus.CONFLICT, "MISSION_NOT_ACTIVE", "Esta não é a missão ativa do perfil.");
        }
        if (learner.lives() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "LIVES_REMAINING", "Só é possível reiniciar a missão quando as vidas acabarem.");
        }

        jdbc.update("""
                UPDATE mission_progress SET completed_question_count = 0
                WHERE learner_id = ? AND level_id = ?
                """, learnerId, levelId);
        jdbc.update("""
                UPDATE learners
                SET lives = ?, streak = 0, current_question_index = 0,
                    updated_at = now(), row_version = row_version + 1
                WHERE id = ?
                """, STARTER_LIVES, learnerId);
        return new RestartMissionResponse(learnerId, levelId, 0, STARTER_LIVES, 0);
    }

    private LearnerState loadLearner(UUID learnerId, boolean lock) {
        String sql = "SELECT * FROM learners WHERE id = ?" + (lock ? " FOR UPDATE" : "");
        return jdbc.query(sql, LEARNER_MAPPER, learnerId).stream()
                .findFirst()
                .orElseThrow(LearnerService::learnerNotFound);
    }

    @Transactional(readOnly = true)
    public void assertCanAccess(UUID learnerId, UUID userId) {
        UUID ownerId = jdbc.query("""
                SELECT user_id FROM learners WHERE id = ?
                """, (result, rowNumber) -> result.getObject("user_id", UUID.class), learnerId)
                .stream().findFirst().orElseThrow(LearnerService::learnerNotFound);
        if (ownerId != null && !ownerId.equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Este progresso pertence a outra conta.");
        }
    }

    private MissionState loadMission(UUID learnerId, int levelId) {
        return jdbc.query("""
                SELECT completed_question_count, completed
                FROM mission_progress
                WHERE learner_id = ? AND level_id = ?
                """, (result, rowNumber) -> new MissionState(
                result.getInt("completed_question_count"), result.getBoolean("completed")), learnerId, levelId)
                .stream().findFirst().orElseThrow(LearnerService::learnerNotFound);
    }

    private boolean hasRewardedQuestion(UUID learnerId, String questionId) {
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM rewarded_questions WHERE learner_id = ? AND question_id = ?
                """, Integer.class, learnerId, questionId);
        return count != null && count > 0;
    }

    private AnswerResponse findPreviousAnswer(UUID learnerId, SubmitAnswerRequest request) {
        return jdbc.query("""
                SELECT question_id, selected_option_id, is_correct, feedback, xp_awarded, gems_awarded,
                      rewards_already_claimed, lives_after, streak_after, mission_completed, completed_question_count
                FROM answer_requests WHERE learner_id = ? AND request_id = ?
                """, (result, rowNumber) -> {
                    if (!result.getString("question_id").equals(request.questionId())
                            || !result.getString("selected_option_id").equals(request.selectedOptionId())) {
                        throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                                "requestId já foi usado com outra resposta.");
                    }
                    Question question = curriculum.question(result.getString("question_id"));
                    return new AnswerResponse(
                            question.id(), result.getBoolean("is_correct"), question.concept(),
                            result.getString("feedback"), result.getInt("xp_awarded"), result.getInt("gems_awarded"),
                            result.getInt("lives_after"), result.getInt("streak_after"),
                            result.getInt("completed_question_count"), result.getBoolean("mission_completed"),
                            result.getBoolean("rewards_already_claimed"));
                }, learnerId, request.requestId()).stream().findFirst().orElse(null);
    }

    private void saveAnswerRequest(UUID learnerId, SubmitAnswerRequest request, AnswerResponse response) {
        jdbc.update("""
                INSERT INTO answer_requests (
                    learner_id, request_id, question_id, selected_option_id, is_correct, feedback,
                    xp_awarded, gems_awarded, rewards_already_claimed, lives_after, streak_after,
                    mission_completed, completed_question_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, learnerId, request.requestId(), request.questionId(), request.selectedOptionId(), response.correct(),
                response.feedback(), response.xpAwarded(), response.gemsAwarded(), response.rewardsAlreadyClaimed(),
                response.lives(), response.streak(), response.missionCompleted(), response.completedQuestionCount());
    }

    private int questionIndex(Level level, String questionId) {
        for (int index = 0; index < level.questions().size(); index++) {
            if (level.questions().get(index).id().equals(questionId)) return index;
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", "Questão não pertence a este nível.");
    }

    private LearnerResponse response(LearnerState learner) {
        return new LearnerResponse(learner.id(), learner.displayName(), learner.objective(), learner.xp(),
                learner.gems(), learner.lives(), learner.streak(), learner.activeLevelId(),
                learner.currentQuestionIndex(), learner.createdAt());
    }

    private static ApiException learnerNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "LEARNER_NOT_FOUND", "Perfil não encontrado.");
    }

    private record LearnerState(UUID id, String displayName, String objective, int xp, int gems, int lives,
                                int streak, int activeLevelId, int currentQuestionIndex, Instant createdAt) {}

    private record MissionState(int completedQuestionCount, boolean completed) {}
}
