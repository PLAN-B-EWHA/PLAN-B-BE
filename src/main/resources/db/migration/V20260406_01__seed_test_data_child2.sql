-- =============================================================
-- TEST SEED DATA: game results + stat summaries (2nd child)
-- Uses the second non-deleted child found in the DB.
-- =============================================================
DO $$
DECLARE
    v_child UUID;

    d_t1_s1 UUID; d_t1_s2 UUID; d_t1_s3 UUID; d_t1_s4 UUID;
    d_t2_s1 UUID; d_t2_s2 UUID; d_t2_s3 UUID; d_t2_s4 UUID;
    d_t3_s1 UUID; d_t3_s2 UUID; d_t3_s3 UUID;
    d_t4_s1 UUID; d_t4_s2 UUID; d_t4_s3 UUID;
    d_t5_s1 UUID; d_t5_s2 UUID; d_t5_s3 UUID;

    e_h1 UUID; e_h2 UUID; e_h3 UUID;
    e_s1 UUID; e_s2 UUID; e_s3 UUID; e_s4 UUID;
    e_a1 UUID; e_a2 UUID;
    e_d1 UUID; e_d2 UUID; e_d3 UUID;
    e_p1 UUID; e_p2 UUID;

BEGIN
    -- 두 번째 아동 선택 (생성일 기준 정렬)
    SELECT child_id INTO v_child
    FROM children
    WHERE is_deleted = false
    ORDER BY created_at
    LIMIT 1 OFFSET 1;

    IF v_child IS NULL THEN
        RAISE EXCEPTION 'No second child found. Create at least two child records first.';
    END IF;

    d_t1_s1:=gen_random_uuid(); d_t1_s2:=gen_random_uuid(); d_t1_s3:=gen_random_uuid(); d_t1_s4:=gen_random_uuid();
    d_t2_s1:=gen_random_uuid(); d_t2_s2:=gen_random_uuid(); d_t2_s3:=gen_random_uuid(); d_t2_s4:=gen_random_uuid();
    d_t3_s1:=gen_random_uuid(); d_t3_s2:=gen_random_uuid(); d_t3_s3:=gen_random_uuid();
    d_t4_s1:=gen_random_uuid(); d_t4_s2:=gen_random_uuid(); d_t4_s3:=gen_random_uuid();
    d_t5_s1:=gen_random_uuid(); d_t5_s2:=gen_random_uuid(); d_t5_s3:=gen_random_uuid();
    e_h1:=gen_random_uuid(); e_h2:=gen_random_uuid(); e_h3:=gen_random_uuid();
    e_s1:=gen_random_uuid(); e_s2:=gen_random_uuid(); e_s3:=gen_random_uuid(); e_s4:=gen_random_uuid();
    e_a1:=gen_random_uuid(); e_a2:=gen_random_uuid();
    e_d1:=gen_random_uuid(); e_d2:=gen_random_uuid(); e_d3:=gen_random_uuid();
    e_p1:=gen_random_uuid(); e_p2:=gen_random_uuid();

    -- ===========================================================
    -- DIALOGUE SESSIONS (1번 아동과 다른 패턴: 초반 저조 → 후반 급성장)
    -- ===========================================================
    INSERT INTO dialogue_sessions
        (session_id, child_id, scenario_id, theme, total_score, max_score, score_rate, started_at, ended_at, created_at, retry_count, week_number)
    VALUES
        (d_t1_s1, v_child, 'seed-sc-001', '정보 교환하기', 1, 8, 0.125, NOW()-INTERVAL'28 days', NOW()-INTERVAL'28 days'+INTERVAL'11 min', CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s2, v_child, 'seed-sc-002', '정보 교환하기', 2, 8, 0.250, NOW()-INTERVAL'21 days', NOW()-INTERVAL'21 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s3, v_child, 'seed-sc-001', '정보 교환하기', 4, 8, 0.500, NOW()-INTERVAL'10 days', NOW()-INTERVAL'10 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s4, v_child, 'seed-sc-002', '정보 교환하기', 7, 8, 0.875, NOW()-INTERVAL'2 days',  NOW()-INTERVAL'2 days' +INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 1),

        (d_t2_s1, v_child, 'seed-sc-003', '대화 유지하기', 2, 8, 0.250, NOW()-INTERVAL'26 days', NOW()-INTERVAL'26 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s2, v_child, 'seed-sc-004', '대화 유지하기', 3, 8, 0.375, NOW()-INTERVAL'19 days', NOW()-INTERVAL'19 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s3, v_child, 'seed-sc-003', '대화 유지하기', 5, 8, 0.625, NOW()-INTERVAL'9 days',  NOW()-INTERVAL'9 days' +INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s4, v_child, 'seed-sc-004', '대화 유지하기', 6, 8, 0.750, NOW()-INTERVAL'3 days',  NOW()-INTERVAL'3 days' +INTERVAL'7 min',  CURRENT_TIMESTAMP, 0, 2),

        (d_t3_s1, v_child, 'seed-sc-005', '공통점 찾기', 1, 8, 0.125, NOW()-INTERVAL'23 days', NOW()-INTERVAL'23 days'+INTERVAL'12 min', CURRENT_TIMESTAMP, 0, 3),
        (d_t3_s2, v_child, 'seed-sc-006', '공통점 찾기', 3, 8, 0.375, NOW()-INTERVAL'14 days', NOW()-INTERVAL'14 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 3),
        (d_t3_s3, v_child, 'seed-sc-005', '공통점 찾기', 6, 8, 0.750, NOW()-INTERVAL'5 days',  NOW()-INTERVAL'5 days' +INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 3),

        (d_t4_s1, v_child, 'seed-sc-007', '대화 시작하기', 2, 8, 0.250, NOW()-INTERVAL'20 days', NOW()-INTERVAL'20 days'+INTERVAL'11 min', CURRENT_TIMESTAMP, 0, 4),
        (d_t4_s2, v_child, 'seed-sc-008', '대화 시작하기', 4, 8, 0.500, NOW()-INTERVAL'12 days', NOW()-INTERVAL'12 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 4),
        (d_t4_s3, v_child, 'seed-sc-007', '대화 시작하기', 6, 8, 0.750, NOW()-INTERVAL'4 days',  NOW()-INTERVAL'4 days' +INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 4),

        (d_t5_s1, v_child, 'seed-sc-009', '갈등 해결하기', 1, 8, 0.125, NOW()-INTERVAL'17 days', NOW()-INTERVAL'17 days'+INTERVAL'13 min', CURRENT_TIMESTAMP, 0, 11),
        (d_t5_s2, v_child, 'seed-sc-010', '갈등 해결하기', 2, 8, 0.250, NOW()-INTERVAL'8 days',  NOW()-INTERVAL'8 days' +INTERVAL'12 min', CURRENT_TIMESTAMP, 0, 11),
        (d_t5_s3, v_child, 'seed-sc-009', '갈등 해결하기', 4, 8, 0.500, NOW()-INTERVAL'1 days',  NOW()-INTERVAL'1 days' +INTERVAL'11 min', CURRENT_TIMESTAMP, 0, 11);

    -- ===========================================================
    -- DIALOGUE TURNS
    -- ===========================================================
    INSERT INTO dialogue_turns
        (turn_id, session_id, child_id, turn_number, selected_option_order, selected_score, npc_reaction_expression, created_at, attempt_number)
    VALUES
        (gen_random_uuid(), d_t1_s1, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 2, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 3, 3, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s2, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 2, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 3, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 4, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s3, v_child, 1, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 2, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 3, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 4, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s4, v_child, 1, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 2, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 3, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s1, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 2, 3, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 3, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s2, v_child, 1, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 2, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 3, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s3, v_child, 1, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 2, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 3, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s4, v_child, 1, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 2, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 3, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s1, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 2, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 3, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s2, v_child, 1, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 2, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 3, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s3, v_child, 1, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 2, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 3, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s1, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 2, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 3, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s2, v_child, 1, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 2, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 3, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s3, v_child, 1, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 2, 1, 2, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 3, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 4, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t5_s1, v_child, 1, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 2, 3, 0, 'Sad',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 3, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t5_s2, v_child, 1, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 2, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 3, 2, 0, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 4, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t5_s3, v_child, 1, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s3, v_child, 2, 1, 1, 'Neutral', CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s3, v_child, 3, 1, 1, 'Joy',    CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s3, v_child, 4, 2, 1, 'Neutral', CURRENT_TIMESTAMP, 0);

    -- ===========================================================
    -- EXPRESSION SESSIONS
    -- ===========================================================
    INSERT INTO expression_sessions
        (session_id, child_id, emotion_target, final_accuracy, is_success, total_tries, started_at, ended_at, created_at)
    VALUES
        (e_h1, v_child, 'happy', 0.55, false, 4, NOW()-INTERVAL'30 days', NOW()-INTERVAL'30 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_h2, v_child, 'happy', 0.70, false, 3, NOW()-INTERVAL'18 days', NOW()-INTERVAL'18 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_h3, v_child, 'happy', 0.88, true,  2, NOW()-INTERVAL'6 days',  NOW()-INTERVAL'6 days' +INTERVAL'4 min', CURRENT_TIMESTAMP),

        (e_s1, v_child, 'sad', 0.50, false, 4, NOW()-INTERVAL'27 days', NOW()-INTERVAL'27 days'+INTERVAL'7 min', CURRENT_TIMESTAMP),
        (e_s2, v_child, 'sad', 0.62, false, 3, NOW()-INTERVAL'20 days', NOW()-INTERVAL'20 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_s3, v_child, 'sad', 0.75, true,  3, NOW()-INTERVAL'12 days', NOW()-INTERVAL'12 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_s4, v_child, 'sad', 0.85, true,  2, NOW()-INTERVAL'4 days',  NOW()-INTERVAL'4 days' +INTERVAL'4 min', CURRENT_TIMESTAMP),

        (e_a1, v_child, 'angry', 0.48, false, 4, NOW()-INTERVAL'24 days', NOW()-INTERVAL'24 days'+INTERVAL'8 min', CURRENT_TIMESTAMP),
        (e_a2, v_child, 'angry', 0.72, true,  3, NOW()-INTERVAL'9 days',  NOW()-INTERVAL'9 days' +INTERVAL'6 min', CURRENT_TIMESTAMP),

        (e_d1, v_child, 'disgust', 0.52, false, 4, NOW()-INTERVAL'22 days', NOW()-INTERVAL'22 days'+INTERVAL'8 min', CURRENT_TIMESTAMP),
        (e_d2, v_child, 'disgust', 0.68, false, 3, NOW()-INTERVAL'13 days', NOW()-INTERVAL'13 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_d3, v_child, 'disgust', 0.80, true,  2, NOW()-INTERVAL'3 days',  NOW()-INTERVAL'3 days' +INTERVAL'5 min', CURRENT_TIMESTAMP),

        (e_p1, v_child, 'surprise', 0.60, false, 3, NOW()-INTERVAL'16 days', NOW()-INTERVAL'16 days'+INTERVAL'7 min', CURRENT_TIMESTAMP),
        (e_p2, v_child, 'surprise', 0.82, true,  2, NOW()-INTERVAL'5 days',  NOW()-INTERVAL'5 days' +INTERVAL'5 min', CURRENT_TIMESTAMP);

    -- ===========================================================
    -- EXPRESSION TRIES
    -- ===========================================================
    INSERT INTO expression_tries
        (try_id, session_id, child_id, try_number, accuracy_score, duration_ms, is_success, created_at)
    VALUES
        (gen_random_uuid(), e_h1, v_child, 1, 0.38, 9500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h1, v_child, 2, 0.45, 9000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h1, v_child, 3, 0.50, 8800, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h1, v_child, 4, 0.55, 8500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 1, 0.52, 8200, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 2, 0.62, 7800, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 3, 0.70, 7500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h3, v_child, 1, 0.72, 6000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h3, v_child, 2, 0.88, 5500, true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_s1, v_child, 1, 0.35, 10000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s1, v_child, 2, 0.42, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s1, v_child, 3, 0.48, 9200,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s1, v_child, 4, 0.50, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 1, 0.48, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 2, 0.55, 8500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 3, 0.62, 8200,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s3, v_child, 1, 0.60, 7800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s3, v_child, 2, 0.70, 7500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s3, v_child, 3, 0.75, 7200,  true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s4, v_child, 1, 0.78, 6800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s4, v_child, 2, 0.85, 6500,  true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_a1, v_child, 1, 0.32, 10500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 2, 0.40, 10000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 3, 0.45, 9800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 4, 0.48, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 1, 0.58, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 2, 0.65, 8500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 3, 0.72, 8200,  true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_d1, v_child, 1, 0.38, 10000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d1, v_child, 2, 0.45, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d1, v_child, 3, 0.50, 9200,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d1, v_child, 4, 0.52, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 1, 0.55, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 2, 0.62, 8500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 3, 0.68, 8200,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d3, v_child, 1, 0.72, 7800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d3, v_child, 2, 0.80, 7500,  true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_p1, v_child, 1, 0.45, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p1, v_child, 2, 0.55, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p1, v_child, 3, 0.60, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p2, v_child, 1, 0.70, 7500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p2, v_child, 2, 0.82, 7200,  true,  CURRENT_TIMESTAMP);

    -- ===========================================================
    -- DIALOGUE STAT SUMMARY
    -- rapport_index = Joy turns / total turns
    --   정보 교환하기: 7/16=0.44, 대화 유지하기: 8/16=0.50
    --   공통점 찾기: 5/12=0.42, 대화 시작하기: 6/12=0.50
    --   갈등 해결하기: 4/12=0.33
    -- ===========================================================
    INSERT INTO dialogue_stat_summary
        (id, child_id, theme, score_rate, rapport_index, turn_fatigue,
         score0_rate, score1_rate, score2_rate, session_count, updated_at)
    VALUES
        (gen_random_uuid(), v_child, '정보 교환하기', 0.438, 0.438, 0.18, 0.38, 0.38, 0.25, 4, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '대화 유지하기', 0.500, 0.500, 0.14, 0.25, 0.50, 0.25, 4, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '공통점 찾기',   0.417, 0.417, 0.20, 0.33, 0.42, 0.25, 3, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '대화 시작하기', 0.500, 0.500, 0.12, 0.25, 0.50, 0.25, 3, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '갈등 해결하기', 0.292, 0.333, 0.25, 0.42, 0.50, 0.08, 3, CURRENT_TIMESTAMP);

    -- ===========================================================
    -- EXPRESSION STAT SUMMARY
    -- ===========================================================
    INSERT INTO expression_stat_summary
        (id, child_id, emotion_target, success_rate, fluency_index, avg_retry,
         session_count, duration_decrease_rate, updated_at)
    VALUES
        (gen_random_uuid(), v_child, 'happy',    0.33, 74.3, 3.00, 3, 0.37, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'sad',      0.50, 78.5, 3.00, 4, 0.33, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'angry',    0.50, 60.5, 3.50, 2, 0.29, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'disgust',  0.33, 68.0, 3.00, 3, 0.25, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'surprise', 0.50, 80.2, 2.50, 2, 0.24, CURRENT_TIMESTAMP);

END $$;
