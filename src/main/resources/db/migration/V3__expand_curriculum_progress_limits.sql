ALTER TABLE learners DROP CONSTRAINT IF EXISTS learners_current_question_index_check;
ALTER TABLE learners
    ADD CONSTRAINT learners_current_question_index_check
    CHECK (current_question_index BETWEEN 0 AND 19);

ALTER TABLE mission_progress DROP CONSTRAINT IF EXISTS mission_progress_completed_question_count_check;
ALTER TABLE mission_progress DROP CONSTRAINT IF EXISTS mission_progress_check;
UPDATE mission_progress
SET completed = FALSE, completed_at = NULL
WHERE completed = TRUE AND completed_question_count < 20;
ALTER TABLE mission_progress
    ADD CONSTRAINT mission_progress_completed_question_count_check
    CHECK (completed_question_count BETWEEN 0 AND 20);
ALTER TABLE mission_progress
    ADD CONSTRAINT mission_progress_check
    CHECK ((completed = FALSE) OR (completed_question_count = 20 AND completed_at IS NOT NULL));
