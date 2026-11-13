ALTER TABLE career_first_job_steps
    DROP CONSTRAINT IF EXISTS career_first_job_steps_step_check;

ALTER TABLE career_first_job_steps
    ADD CONSTRAINT career_first_job_steps_step_check
        CHECK (step IN ('FIRST_JOB_GOAL', 'RESUME_PLAN', 'TRAINING_PLAN', 'APPLICATION_PLAN'));
