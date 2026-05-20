-- v2.1 2-1 EMA 숙달 지수: dialogue_stat_summary 컬럼 추가
ALTER TABLE dialogue_stat_summary
    ADD COLUMN IF NOT EXISTS ema_value        FLOAT,
    ADD COLUMN IF NOT EXISTS ema_alpha        FLOAT,
    ADD COLUMN IF NOT EXISTS consistency_std  FLOAT;
