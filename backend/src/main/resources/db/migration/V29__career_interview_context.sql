ALTER TABLE career_interview_sessions
    ADD COLUMN experience_level VARCHAR(16) NOT NULL DEFAULT 'ENTRY'
        CHECK (experience_level IN ('ENTRY','JUNIOR','MID','SENIOR')),
    ADD COLUMN interview_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL'
        CHECK (interview_type IN ('GENERAL','FIRST_JOB','BEHAVIORAL','TECHNICAL')),
    ADD COLUMN save_history BOOLEAN NOT NULL DEFAULT TRUE;
