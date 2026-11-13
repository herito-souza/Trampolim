CREATE TABLE career_mentor_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    public_id UUID NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    biography VARCHAR(1200) NOT NULL DEFAULT '',
    experience_summary VARCHAR(1200) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED','WITHDRAWN')),
    review_note VARCHAR(500) NOT NULL DEFAULT '',
    reviewed_by BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    adult_confirmed_at TIMESTAMPTZ NOT NULL,
    public_profile_consent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_mentor_public ON career_mentor_profiles(status,public_id) WHERE status='APPROVED';

CREATE TABLE career_mentor_specialties (
    mentor_user_id BIGINT NOT NULL REFERENCES career_mentor_profiles(user_id) ON DELETE CASCADE,
    specialty VARCHAR(80) NOT NULL,
    PRIMARY KEY (mentor_user_id,specialty)
);
CREATE INDEX idx_career_mentor_specialty ON career_mentor_specialties(specialty,mentor_user_id);

CREATE TABLE career_mentor_availability (
    id BIGSERIAL PRIMARY KEY,
    mentor_user_id BIGINT NOT NULL REFERENCES career_mentor_profiles(user_id) ON DELETE CASCADE,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    modality VARCHAR(16) NOT NULL CHECK (modality IN ('ONLINE','IN_PERSON')),
    public_area VARCHAR(120) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','BOOKED','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (ends_at>starts_at),
    UNIQUE (mentor_user_id,starts_at,ends_at)
);
CREATE INDEX idx_career_mentor_slots ON career_mentor_availability(mentor_user_id,starts_at) WHERE status='OPEN';
CREATE INDEX idx_career_public_slots ON career_mentor_availability(starts_at) WHERE status='OPEN';

CREATE TABLE career_mentoring_sessions (
    id UUID PRIMARY KEY,
    availability_id BIGINT NOT NULL REFERENCES career_mentor_availability(id),
    mentor_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    mentee_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    topic VARCHAR(120) NOT NULL,
    request_message VARCHAR(1000) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED','CONFIRMED','DECLINED','CANCELLED','COMPLETED')),
    mentee_adult_confirmed_at TIMESTAMPTZ NOT NULL,
    meeting_details VARCHAR(500) NOT NULL DEFAULT '',
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (mentor_user_id<>mentee_user_id)
);
CREATE INDEX idx_career_mentoring_mentee ON career_mentoring_sessions(mentee_user_id,created_at DESC);
CREATE INDEX idx_career_mentoring_mentor ON career_mentoring_sessions(mentor_user_id,created_at DESC);
CREATE INDEX idx_career_mentoring_slot ON career_mentoring_sessions(availability_id,status);
CREATE UNIQUE INDEX uq_career_mentoring_active_slot ON career_mentoring_sessions(availability_id)
    WHERE status IN ('REQUESTED','CONFIRMED');

-- If an attendee closes their account, release a future slot that was reserved by a deleted request.
CREATE FUNCTION release_deleted_mentoring_session_slot() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    UPDATE career_mentor_availability
       SET status=CASE WHEN starts_at>CURRENT_TIMESTAMP THEN 'OPEN' ELSE 'CLOSED' END
     WHERE id=OLD.availability_id AND status='BOOKED';
    RETURN OLD;
END;
$$;
CREATE TRIGGER trg_release_deleted_mentoring_session_slot
    AFTER DELETE ON career_mentoring_sessions
    FOR EACH ROW EXECUTE FUNCTION release_deleted_mentoring_session_slot();

CREATE TABLE career_mentoring_events (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES career_mentoring_sessions(id) ON DELETE CASCADE,
    actor_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    event_type VARCHAR(24) NOT NULL CHECK (event_type IN ('REQUESTED','CONFIRMED','DECLINED','CANCELLED','COMPLETED','DETAILS_UPDATED')),
    details VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_mentoring_events ON career_mentoring_events(session_id,created_at);

CREATE TABLE career_mentoring_ratings (
    session_id UUID PRIMARY KEY REFERENCES career_mentoring_sessions(id) ON DELETE CASCADE,
    mentee_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE career_mentoring_reports (
    id UUID PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    target_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    session_id UUID REFERENCES career_mentoring_sessions(id) ON DELETE SET NULL,
    reason VARCHAR(24) NOT NULL CHECK (reason IN ('SAFETY','HARASSMENT','MISCONDUCT','PRIVACY','OTHER')),
    details VARCHAR(1500) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','REVIEWING','RESOLVED','DISMISSED')),
    decision VARCHAR(20) NOT NULL DEFAULT '',
    review_note VARCHAR(1000) NOT NULL DEFAULT '',
    reviewed_by BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_career_mentoring_reports_queue ON career_mentoring_reports(status,created_at) WHERE status IN ('PENDING','REVIEWING');
CREATE INDEX idx_career_mentoring_reports_reporter ON career_mentoring_reports(reporter_user_id,created_at DESC);
