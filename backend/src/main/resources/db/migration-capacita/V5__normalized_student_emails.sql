-- Registration normalizes email addresses to lowercase. This index also protects
-- against concurrent requests and legacy rows that differ only by letter case.
CREATE UNIQUE INDEX uq_student_users_email_normalized ON student_users (LOWER(email));
