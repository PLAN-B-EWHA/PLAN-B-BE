-- v2.1 통계 지표 개선: expression_stat_summary 컬럼 추가
ALTER TABLE expression_stat_summary
    ADD COLUMN IF NOT EXISTS ci_lower             FLOAT,
    ADD COLUMN IF NOT EXISTS ci_upper             FLOAT,
    ADD COLUMN IF NOT EXISTS convergence_speed    FLOAT,
    ADD COLUMN IF NOT EXISTS valid_session_rate   FLOAT,
    ADD COLUMN IF NOT EXISTS avg_session_duration_sec FLOAT;

-- v2.1: turn_fatigue는 5턴 미만 세션에서 null 저장 허용
ALTER TABLE dialogue_stat_summary
    ALTER COLUMN turn_fatigue DROP NOT NULL;
