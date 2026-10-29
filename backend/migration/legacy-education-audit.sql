-- Read-only inventory for the legacy Oportunidades database. No personal identifiers are returned.
SELECT 'education_tracks' AS entity, COUNT(*)::BIGINT AS total FROM education_tracks
UNION ALL
SELECT 'education_categories', COUNT(*)::BIGINT FROM education_categories
UNION ALL
SELECT 'education_contents', COUNT(*)::BIGINT FROM education_contents
UNION ALL
SELECT 'published_contents', COUNT(*)::BIGINT FROM education_contents WHERE status = 'PUBLISHED'
UNION ALL
SELECT 'user_education_progress', COUNT(*)::BIGINT FROM user_education_progress
ORDER BY entity;
