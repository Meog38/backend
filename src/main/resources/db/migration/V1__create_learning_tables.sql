CREATE TABLE learners (
    id UUID PRIMARY KEY,
    display_name VARCHAR(60) NOT NULL,
    objective VARCHAR(100) NOT NULL,
    xp INTEGER NOT NULL DEFAULT 0 CHECK (xp >= 0),
    gems INTEGER NOT NULL DEFAULT 0 CHECK (gems >= 0),
    lives SMALLINT NOT NULL DEFAULT 3 CHECK (lives BETWEEN 0 AND 3),
    streak INTEGER NOT NULL DEFAULT 0 CHECK (streak >= 0),
    active_level_id SMALLINT NOT NULL DEFAULT 1 CHECK (active_level_id BETWEEN 1 AND 5),
    current_question_index SMALLINT NOT NULL DEFAULT 0 CHECK (current_question_index BETWEEN 0 AND 4),
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE mission_progress (
    learner_id UUID NOT NULL REFERENCES learners(id) ON DELETE CASCADE,
    level_id SMALLINT NOT NULL CHECK (level_id BETWEEN 1 AND 5),
    completed_question_count SMALLINT NOT NULL DEFAULT 0 CHECK (completed_question_count BETWEEN 0 AND 5),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (learner_id, level_id),
    CHECK ((completed = FALSE) OR (completed_question_count = 5 AND completed_at IS NOT NULL))
);

CREATE TABLE rewarded_questions (
    learner_id UUID NOT NULL REFERENCES learners(id) ON DELETE CASCADE,
    question_id VARCHAR(20) NOT NULL,
    xp_awarded SMALLINT NOT NULL CHECK (xp_awarded >= 0),
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (learner_id, question_id)
);

CREATE TABLE answer_requests (
    learner_id UUID NOT NULL REFERENCES learners(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    question_id VARCHAR(20) NOT NULL,
    selected_option_id CHAR(1) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    feedback TEXT NOT NULL,
    xp_awarded SMALLINT NOT NULL DEFAULT 0,
    gems_awarded SMALLINT NOT NULL DEFAULT 0,
    rewards_already_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    lives_after SMALLINT NOT NULL,
    streak_after INTEGER NOT NULL,
    mission_completed BOOLEAN NOT NULL,
    completed_question_count SMALLINT NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (learner_id, request_id)
);

CREATE INDEX idx_mission_progress_learner ON mission_progress (learner_id, level_id);