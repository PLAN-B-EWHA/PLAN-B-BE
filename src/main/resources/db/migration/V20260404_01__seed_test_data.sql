-- =============================================================
-- TEST SEED DATA: game results + stat summaries
-- Uses the first non-deleted child found in the DB.
-- =============================================================
DO $$
DECLARE
    v_child UUID;

    d_t1_s1 UUID; d_t1_s2 UUID; d_t1_s3 UUID; d_t1_s4 UUID; d_t1_s5 UUID;
    d_t2_s1 UUID; d_t2_s2 UUID; d_t2_s3 UUID; d_t2_s4 UUID; d_t2_s5 UUID;
    d_t3_s1 UUID; d_t3_s2 UUID; d_t3_s3 UUID;
    d_t4_s1 UUID; d_t4_s2 UUID; d_t4_s3 UUID; d_t4_s4 UUID;
    d_t5_s1 UUID; d_t5_s2 UUID;

    e_h1 UUID; e_h2 UUID; e_h3 UUID; e_h4 UUID;
    e_s1 UUID; e_s2 UUID; e_s3 UUID;
    e_a1 UUID; e_a2 UUID; e_a3 UUID; e_a4 UUID;
    e_d1 UUID; e_d2 UUID;
    e_p1 UUID; e_p2 UUID; e_p3 UUID;

BEGIN
    SELECT child_id INTO v_child FROM children WHERE is_deleted = false LIMIT 1;
    IF v_child IS NULL THEN
        RAISE EXCEPTION 'No child found. Create a child record first.';
    END IF;

    d_t1_s1:=gen_random_uuid(); d_t1_s2:=gen_random_uuid(); d_t1_s3:=gen_random_uuid(); d_t1_s4:=gen_random_uuid(); d_t1_s5:=gen_random_uuid();
    d_t2_s1:=gen_random_uuid(); d_t2_s2:=gen_random_uuid(); d_t2_s3:=gen_random_uuid(); d_t2_s4:=gen_random_uuid(); d_t2_s5:=gen_random_uuid();
    d_t3_s1:=gen_random_uuid(); d_t3_s2:=gen_random_uuid(); d_t3_s3:=gen_random_uuid();
    d_t4_s1:=gen_random_uuid(); d_t4_s2:=gen_random_uuid(); d_t4_s3:=gen_random_uuid(); d_t4_s4:=gen_random_uuid();
    d_t5_s1:=gen_random_uuid(); d_t5_s2:=gen_random_uuid();
    e_h1:=gen_random_uuid(); e_h2:=gen_random_uuid(); e_h3:=gen_random_uuid(); e_h4:=gen_random_uuid();
    e_s1:=gen_random_uuid(); e_s2:=gen_random_uuid(); e_s3:=gen_random_uuid();
    e_a1:=gen_random_uuid(); e_a2:=gen_random_uuid(); e_a3:=gen_random_uuid(); e_a4:=gen_random_uuid();
    e_d1:=gen_random_uuid(); e_d2:=gen_random_uuid();
    e_p1:=gen_random_uuid(); e_p2:=gen_random_uuid(); e_p3:=gen_random_uuid();

    -- ===========================================================
    -- DIALOGUE SESSIONS
    -- ===========================================================
    INSERT INTO dialogue_sessions
        (session_id, child_id, scenario_id, theme, total_score, max_score, score_rate, started_at, ended_at, created_at, retry_count, week_number)
    VALUES
        (d_t1_s1, v_child, 'seed-sc-001', '정보 교환하기', 1, 8, 0.125, NOW()-INTERVAL'30 days', NOW()-INTERVAL'30 days'+INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s2, v_child, 'seed-sc-002', '정보 교환하기', 3, 8, 0.375, NOW()-INTERVAL'25 days', NOW()-INTERVAL'25 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s3, v_child, 'seed-sc-001', '정보 교환하기', 5, 8, 0.625, NOW()-INTERVAL'20 days', NOW()-INTERVAL'20 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s4, v_child, 'seed-sc-002', '정보 교환하기', 6, 8, 0.750, NOW()-INTERVAL'14 days', NOW()-INTERVAL'14 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 1),
        (d_t1_s5, v_child, 'seed-sc-001', '정보 교환하기', 7, 8, 0.875, NOW()-INTERVAL'7 days',  NOW()-INTERVAL'7 days' +INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 1),

        (d_t2_s1, v_child, 'seed-sc-003', '대화 유지하기', 2, 8, 0.250, NOW()-INTERVAL'28 days', NOW()-INTERVAL'28 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s2, v_child, 'seed-sc-004', '대화 유지하기', 3, 8, 0.375, NOW()-INTERVAL'22 days', NOW()-INTERVAL'22 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s3, v_child, 'seed-sc-003', '대화 유지하기', 4, 8, 0.500, NOW()-INTERVAL'16 days', NOW()-INTERVAL'16 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s4, v_child, 'seed-sc-004', '대화 유지하기', 6, 8, 0.750, NOW()-INTERVAL'10 days', NOW()-INTERVAL'10 days'+INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 2),
        (d_t2_s5, v_child, 'seed-sc-003', '대화 유지하기', 7, 8, 0.875, NOW()-INTERVAL'4 days',  NOW()-INTERVAL'4 days' +INTERVAL'7 min',  CURRENT_TIMESTAMP, 0, 2),

        (d_t3_s1, v_child, 'seed-sc-005', '공통점 찾기', 2, 8, 0.250, NOW()-INTERVAL'21 days', NOW()-INTERVAL'21 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 3),
        (d_t3_s2, v_child, 'seed-sc-006', '공통점 찾기', 4, 8, 0.500, NOW()-INTERVAL'13 days', NOW()-INTERVAL'13 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 3),
        (d_t3_s3, v_child, 'seed-sc-005', '공통점 찾기', 6, 8, 0.750, NOW()-INTERVAL'6 days',  NOW()-INTERVAL'6 days' +INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 3),

        (d_t4_s1, v_child, 'seed-sc-007', '대화 시작하기', 1, 8, 0.125, NOW()-INTERVAL'27 days', NOW()-INTERVAL'27 days'+INTERVAL'11 min', CURRENT_TIMESTAMP, 0, 4),
        (d_t4_s2, v_child, 'seed-sc-008', '대화 시작하기', 3, 8, 0.375, NOW()-INTERVAL'19 days', NOW()-INTERVAL'19 days'+INTERVAL'10 min', CURRENT_TIMESTAMP, 0, 4),
        (d_t4_s3, v_child, 'seed-sc-007', '대화 시작하기', 5, 8, 0.625, NOW()-INTERVAL'11 days', NOW()-INTERVAL'11 days'+INTERVAL'9 min',  CURRENT_TIMESTAMP, 0, 4),
        (d_t4_s4, v_child, 'seed-sc-008', '대화 시작하기', 7, 8, 0.875, NOW()-INTERVAL'3 days',  NOW()-INTERVAL'3 days' +INTERVAL'8 min',  CURRENT_TIMESTAMP, 0, 4),

        (d_t5_s1, v_child, 'seed-sc-009', '갈등 해결하기', 2, 8, 0.250, NOW()-INTERVAL'5 days', NOW()-INTERVAL'5 days'+INTERVAL'12 min', CURRENT_TIMESTAMP, 0, 11),
        (d_t5_s2, v_child, 'seed-sc-010', '갈등 해결하기', 3, 8, 0.375, NOW()-INTERVAL'1 days', NOW()-INTERVAL'1 days'+INTERVAL'11 min', CURRENT_TIMESTAMP, 0, 11);

    -- ===========================================================
    -- DIALOGUE TURNS
    -- ===========================================================
    INSERT INTO dialogue_turns
        (turn_id, session_id, child_id, turn_number, selected_option_order, selected_score, npc_reaction_expression, created_at, attempt_number)
    VALUES
        (gen_random_uuid(), d_t1_s1, v_child, 1, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 2, 2, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 3, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s1, v_child, 4, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s2, v_child, 1, 2, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 2, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 3, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s2, v_child, 4, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s3, v_child, 1, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 2, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 3, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s3, v_child, 4, 1, 2, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s4, v_child, 1, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 2, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 3, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s4, v_child, 4, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t1_s5, v_child, 1, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s5, v_child, 2, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s5, v_child, 3, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t1_s5, v_child, 4, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s1, v_child, 1, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 2, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 3, 3, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s1, v_child, 4, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s2, v_child, 1, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 2, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 3, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s2, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s3, v_child, 1, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 2, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 3, 1, 2, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s3, v_child, 4, 3, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s4, v_child, 1, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 2, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 3, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s4, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t2_s5, v_child, 1, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s5, v_child, 2, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s5, v_child, 3, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t2_s5, v_child, 4, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s1, v_child, 1, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 2, 2, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 3, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s1, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s2, v_child, 1, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 2, 1, 2, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 3, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s2, v_child, 4, 3, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t3_s3, v_child, 1, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 2, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 3, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t3_s3, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s1, v_child, 1, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 2, 2, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 3, 3, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s1, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s2, v_child, 1, 2, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 2, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 3, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s2, v_child, 4, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s3, v_child, 1, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 2, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 3, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s3, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t4_s4, v_child, 1, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s4, v_child, 2, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s4, v_child, 3, 1, 2, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t4_s4, v_child, 4, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t5_s1, v_child, 1, 3, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 2, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 3, 2, 0, 'Sad',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s1, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),

        (gen_random_uuid(), d_t5_s2, v_child, 1, 3, 0, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 2, 1, 1, 'Joy',     CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 3, 2, 1, 'Neutral',  CURRENT_TIMESTAMP, 0),
        (gen_random_uuid(), d_t5_s2, v_child, 4, 1, 1, 'Neutral',  CURRENT_TIMESTAMP, 0);

    -- ===========================================================
    -- EXPRESSION SESSIONS
    -- ===========================================================
    INSERT INTO expression_sessions
        (session_id, child_id, emotion_target, final_accuracy, is_success, total_tries, started_at, ended_at, created_at)
    VALUES
        (e_h1, v_child, 'happy', 0.65, false, 3, NOW()-INTERVAL'35 days', NOW()-INTERVAL'35 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_h2, v_child, 'happy', 0.78, true,  3, NOW()-INTERVAL'24 days', NOW()-INTERVAL'24 days'+INTERVAL'4 min', CURRENT_TIMESTAMP),
        (e_h3, v_child, 'happy', 0.83, true,  2, NOW()-INTERVAL'15 days', NOW()-INTERVAL'15 days'+INTERVAL'3 min', CURRENT_TIMESTAMP),
        (e_h4, v_child, 'happy', 0.92, true,  2, NOW()-INTERVAL'5 days',  NOW()-INTERVAL'5 days' +INTERVAL'3 min', CURRENT_TIMESTAMP),

        (e_s1, v_child, 'sad', 0.60, false, 3, NOW()-INTERVAL'30 days', NOW()-INTERVAL'30 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_s2, v_child, 'sad', 0.75, true,  4, NOW()-INTERVAL'18 days', NOW()-INTERVAL'18 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_s3, v_child, 'sad', 0.82, true,  2, NOW()-INTERVAL'7 days',  NOW()-INTERVAL'7 days' +INTERVAL'4 min', CURRENT_TIMESTAMP),

        (e_a1, v_child, 'angry', 0.55, false, 4, NOW()-INTERVAL'28 days', NOW()-INTERVAL'28 days'+INTERVAL'7 min', CURRENT_TIMESTAMP),
        (e_a2, v_child, 'angry', 0.68, false, 3, NOW()-INTERVAL'20 days', NOW()-INTERVAL'20 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_a3, v_child, 'angry', 0.78, true,  3, NOW()-INTERVAL'12 days', NOW()-INTERVAL'12 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_a4, v_child, 'angry', 0.80, true,  2, NOW()-INTERVAL'3 days',  NOW()-INTERVAL'3 days' +INTERVAL'4 min', CURRENT_TIMESTAMP),

        (e_d1, v_child, 'disgust', 0.58, false, 3, NOW()-INTERVAL'22 days', NOW()-INTERVAL'22 days'+INTERVAL'7 min', CURRENT_TIMESTAMP),
        (e_d2, v_child, 'disgust', 0.75, true,  3, NOW()-INTERVAL'8 days',  NOW()-INTERVAL'8 days' +INTERVAL'5 min', CURRENT_TIMESTAMP),

        (e_p1, v_child, 'surprise', 0.62, false, 3, NOW()-INTERVAL'25 days', NOW()-INTERVAL'25 days'+INTERVAL'6 min', CURRENT_TIMESTAMP),
        (e_p2, v_child, 'surprise', 0.78, true,  3, NOW()-INTERVAL'15 days', NOW()-INTERVAL'15 days'+INTERVAL'5 min', CURRENT_TIMESTAMP),
        (e_p3, v_child, 'surprise', 0.88, true,  2, NOW()-INTERVAL'4 days',  NOW()-INTERVAL'4 days' +INTERVAL'3 min', CURRENT_TIMESTAMP);

    -- ===========================================================
    -- EXPRESSION TRIES
    -- ===========================================================
    INSERT INTO expression_tries
        (try_id, session_id, child_id, try_number, accuracy_score, duration_ms, is_success, created_at)
    VALUES
        (gen_random_uuid(), e_h1, v_child, 1, 0.45, 8500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h1, v_child, 2, 0.55, 8000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h1, v_child, 3, 0.65, 7800, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 1, 0.55, 7500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 2, 0.68, 7000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h2, v_child, 3, 0.78, 6500, true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h3, v_child, 1, 0.72, 5500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h3, v_child, 2, 0.83, 5200, true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h4, v_child, 1, 0.80, 4800, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_h4, v_child, 2, 0.92, 4500, true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_s1, v_child, 1, 0.40, 9000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s1, v_child, 2, 0.52, 8500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s1, v_child, 3, 0.60, 8200, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 1, 0.45, 8000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 2, 0.55, 7800, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 3, 0.65, 7500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s2, v_child, 4, 0.75, 7200, true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s3, v_child, 1, 0.70, 6500, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_s3, v_child, 2, 0.82, 6200, true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_a1, v_child, 1, 0.35, 10000, false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 2, 0.42, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 3, 0.50, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a1, v_child, 4, 0.55, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 1, 0.48, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 2, 0.58, 8500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a2, v_child, 3, 0.68, 8200,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a3, v_child, 1, 0.60, 7800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a3, v_child, 2, 0.70, 7500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a3, v_child, 3, 0.78, 7200,  true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a4, v_child, 1, 0.72, 7000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_a4, v_child, 2, 0.80, 6800,  true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_d1, v_child, 1, 0.42, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d1, v_child, 2, 0.50, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d1, v_child, 3, 0.58, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 1, 0.55, 8500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 2, 0.65, 8000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_d2, v_child, 3, 0.75, 7800,  true,  CURRENT_TIMESTAMP),

        (gen_random_uuid(), e_p1, v_child, 1, 0.42, 9500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p1, v_child, 2, 0.52, 9000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p1, v_child, 3, 0.62, 8800,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p2, v_child, 1, 0.58, 8000,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p2, v_child, 2, 0.70, 7500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p2, v_child, 3, 0.78, 7200,  true,  CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p3, v_child, 1, 0.78, 6500,  false, CURRENT_TIMESTAMP),
        (gen_random_uuid(), e_p3, v_child, 2, 0.88, 6200,  true,  CURRENT_TIMESTAMP);

    -- ===========================================================
    -- DIALOGUE STAT SUMMARY
    -- rapport_index = Joy turns / total turns per theme
    --   정보 교환하기: 10/20=0.50, 대화 유지하기: 7/20=0.35
    --   공통점 찾기: 3/12=0.25, 대화 시작하기: 6/16=0.375
    --   갈등 해결하기: 1/8=0.125
    -- ===========================================================
    INSERT INTO dialogue_stat_summary
        (id, child_id, theme, score_rate, rapport_index, turn_fatigue,
         score0_rate, score1_rate, score2_rate, session_count, updated_at)
    VALUES
        (gen_random_uuid(), v_child, '정보 교환하기', 0.550, 0.500, 0.08, 0.25, 0.45, 0.30, 5, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '대화 유지하기', 0.550, 0.350, 0.12, 0.20, 0.50, 0.30, 5, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '공통점 찾기',   0.500, 0.250, 0.15, 0.25, 0.50, 0.25, 3, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '대화 시작하기', 0.500, 0.375, 0.10, 0.25, 0.50, 0.25, 4, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, '갈등 해결하기', 0.313, 0.125, 0.20, 0.38, 0.62, 0.00, 2, CURRENT_TIMESTAMP);

    -- ===========================================================
    -- EXPRESSION STAT SUMMARY
    -- fluency_index = finalAccuracy × (baseline_ms / lastTry_ms) × 100
    --   happy:   0.92×(4000/4500)×100 ≈ 81.8
    --   sad:     0.82×(5500/6200)×100 ≈ 72.7
    --   angry:   0.80×(6000/6800)×100 ≈ 70.6
    --   disgust: 0.75×(6000/7800)×100 ≈ 57.7
    --   surprise:0.88×(6500/6200)×100 ≈ 92.3
    -- ===========================================================
    INSERT INTO expression_stat_summary
        (id, child_id, emotion_target, success_rate, fluency_index, avg_retry,
         session_count, duration_decrease_rate, updated_at)
    VALUES
        (gen_random_uuid(), v_child, 'happy',    0.75, 81.8, 2.50, 4, 0.42, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'sad',      0.67, 72.7, 3.00, 3, 0.24, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'angry',    0.50, 70.6, 3.00, 4, 0.23, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'disgust',  0.50, 57.7, 3.00, 2, 0.11, CURRENT_TIMESTAMP),
        (gen_random_uuid(), v_child, 'surprise', 0.67, 92.3, 2.67, 3, 0.30, CURRENT_TIMESTAMP);

END $$;
