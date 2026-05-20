-- Dev seed for an existing child.
-- Paste this into IntelliJ's PostgreSQL console connected to the Docker pgvector DB.
--
-- Change only this one value:
--   target_child_id := 'PUT_EXISTING_CHILD_UUID_HERE'
--
-- This script does NOT create users, does NOT create a child, and does NOT change permissions.
-- It replaces only fixed demo gameplay/statistics/homework rows for the selected existing child.

BEGIN;

DO $$
DECLARE
    target_child_id uuid := '09fe2a07-f517-444a-b9d5-0e09014af639';
    reporter_user_id uuid;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM children WHERE child_id = target_child_id) THEN
        RAISE EXCEPTION 'Child does not exist: %', target_child_id;
    END IF;

    SELECT cau.user_id
    INTO reporter_user_id
    FROM children_authorized_users cau
    WHERE cau.child_id = target_child_id
      AND cau.is_active = true
    ORDER BY cau.is_primary DESC, cau.authorized_at ASC
    LIMIT 1;

    IF reporter_user_id IS NULL THEN
        SELECT user_id
        INTO reporter_user_id
        FROM users
        ORDER BY created_at ASC
        LIMIT 1;
    END IF;

    IF reporter_user_id IS NULL THEN
        RAISE EXCEPTION 'No user exists for homework_reports.reported_by';
    END IF;

    DELETE FROM homework_reports
    WHERE report_id BETWEEN '16161616-1616-1616-1616-161616161601'::uuid
                        AND '16161616-1616-1616-1616-161616161603'::uuid;

    DELETE FROM homework_assignments
    WHERE child_id = target_child_id
      AND homework_id BETWEEN '15151515-1515-1515-1515-151515151501'::uuid
                          AND '15151515-1515-1515-1515-151515151503'::uuid;

    DELETE FROM expression_tries
    WHERE child_id = target_child_id
      AND try_id BETWEEN 'ffffffff-ffff-ffff-ffff-ffffffffff01'::uuid
                     AND 'ffffffff-ffff-ffff-ffff-ffffffffff18'::uuid;

    DELETE FROM expression_sessions
    WHERE child_id = target_child_id
      AND session_id BETWEEN 'cccccccc-cccc-cccc-cccc-cccccccccc01'::uuid
                         AND 'cccccccc-cccc-cccc-cccc-cccccccccc06'::uuid;

    DELETE FROM dialogue_turns
    WHERE child_id = target_child_id
      AND turn_id BETWEEN 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01'::uuid
                      AND 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee18'::uuid;

    DELETE FROM dialogue_sessions
    WHERE child_id = target_child_id
      AND session_id BETWEEN 'dddddddd-dddd-dddd-dddd-dddddddddd01'::uuid
                         AND 'dddddddd-dddd-dddd-dddd-dddddddddd06'::uuid;

    DELETE FROM dialogue_stat_summary
    WHERE child_id = target_child_id
      AND id BETWEEN '12121212-1212-1212-1212-121212121201'::uuid
                 AND '12121212-1212-1212-1212-121212121204'::uuid;

    DELETE FROM expression_stat_summary
    WHERE child_id = target_child_id
      AND id BETWEEN '13131313-1313-1313-1313-131313131301'::uuid
                 AND '13131313-1313-1313-1313-131313131304'::uuid;

    DELETE FROM dialogue_error_pattern_summary
    WHERE child_id = target_child_id
      AND id BETWEEN '14141414-1414-1414-1414-141414141401'::uuid
                 AND '14141414-1414-1414-1414-141414141404'::uuid;

    -- Dialogue play history: 6 sessions, 18 turns.
    INSERT INTO dialogue_sessions (
        session_id, child_id, scenario_id, scenario_source, theme,
        total_score, max_score, score_rate, started_at, ended_at, created_at
    )
    VALUES
        ('dddddddd-dddd-dddd-dddd-dddddddddd01', target_child_id, 'W04_DEMO_START_001', 'SERVER_LLM', 'STARTING_CONVERSATION',
         3, 6, 0.5000, now() - interval '14 days', now() - interval '14 days' + interval '8 minutes', now() - interval '14 days'),
        ('dddddddd-dddd-dddd-dddd-dddddddddd02', target_child_id, 'W02_DEMO_MAINTAIN_001', 'SERVER_LLM', 'MAINTAINING_CONVERSATION',
         4, 6, 0.6667, now() - interval '11 days', now() - interval '11 days' + interval '9 minutes', now() - interval '11 days'),
        ('dddddddd-dddd-dddd-dddd-dddddddddd03', target_child_id, 'W03_DEMO_COMMON_001', 'SERVER_LLM', 'FINDING_COMMON_GROUND',
         5, 6, 0.8333, now() - interval '8 days', now() - interval '8 days' + interval '7 minutes', now() - interval '8 days'),
        ('dddddddd-dddd-dddd-dddd-dddddddddd04', target_child_id, 'W04_DEMO_START_002', 'SERVER_LLM', 'STARTING_CONVERSATION',
         4, 6, 0.6667, now() - interval '5 days', now() - interval '5 days' + interval '8 minutes', now() - interval '5 days'),
        ('dddddddd-dddd-dddd-dddd-dddddddddd05', target_child_id, 'W11_DEMO_CONFLICT_001', 'SERVER_LLM', 'RESOLVING_CONFLICT',
         3, 6, 0.5000, now() - interval '3 days', now() - interval '3 days' + interval '10 minutes', now() - interval '3 days'),
        ('dddddddd-dddd-dddd-dddd-dddddddddd06', target_child_id, 'W04_DEMO_START_003', 'SERVER_LLM', 'STARTING_CONVERSATION',
         5, 6, 0.8333, now() - interval '1 day', now() - interval '1 day' + interval '8 minutes', now() - interval '1 day');

    INSERT INTO dialogue_turns (
        turn_id, session_id, child_id, turn_number, selected_option_order,
        selected_score, npc_reaction_expression, created_at
    )
    VALUES
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01', 'dddddddd-dddd-dddd-dddd-dddddddddd01', target_child_id, 1, 2, 0, 'sad', now() - interval '14 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02', 'dddddddd-dddd-dddd-dddd-dddddddddd01', target_child_id, 2, 1, 1, 'neutral', now() - interval '14 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee03', 'dddddddd-dddd-dddd-dddd-dddddddddd01', target_child_id, 3, 3, 2, 'happy', now() - interval '14 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee04', 'dddddddd-dddd-dddd-dddd-dddddddddd02', target_child_id, 1, 2, 1, 'neutral', now() - interval '11 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee05', 'dddddddd-dddd-dddd-dddd-dddddddddd02', target_child_id, 2, 3, 1, 'neutral', now() - interval '11 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee06', 'dddddddd-dddd-dddd-dddd-dddddddddd02', target_child_id, 3, 1, 2, 'happy', now() - interval '11 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee07', 'dddddddd-dddd-dddd-dddd-dddddddddd03', target_child_id, 1, 1, 2, 'happy', now() - interval '8 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee08', 'dddddddd-dddd-dddd-dddd-dddddddddd03', target_child_id, 2, 2, 1, 'neutral', now() - interval '8 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee09', 'dddddddd-dddd-dddd-dddd-dddddddddd03', target_child_id, 3, 3, 2, 'happy', now() - interval '8 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee10', 'dddddddd-dddd-dddd-dddd-dddddddddd04', target_child_id, 1, 1, 1, 'neutral', now() - interval '5 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee11', 'dddddddd-dddd-dddd-dddd-dddddddddd04', target_child_id, 2, 2, 1, 'neutral', now() - interval '5 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee12', 'dddddddd-dddd-dddd-dddd-dddddddddd04', target_child_id, 3, 1, 2, 'happy', now() - interval '5 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee13', 'dddddddd-dddd-dddd-dddd-dddddddddd05', target_child_id, 1, 3, 0, 'angry', now() - interval '3 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee14', 'dddddddd-dddd-dddd-dddd-dddddddddd05', target_child_id, 2, 2, 1, 'neutral', now() - interval '3 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee15', 'dddddddd-dddd-dddd-dddd-dddddddddd05', target_child_id, 3, 1, 2, 'happy', now() - interval '3 days'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee16', 'dddddddd-dddd-dddd-dddd-dddddddddd06', target_child_id, 1, 1, 2, 'happy', now() - interval '1 day'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee17', 'dddddddd-dddd-dddd-dddd-dddddddddd06', target_child_id, 2, 2, 1, 'neutral', now() - interval '1 day'),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeee18', 'dddddddd-dddd-dddd-dddd-dddddddddd06', target_child_id, 3, 1, 2, 'happy', now() - interval '1 day');

    -- Expression play history: 6 sessions, 18 tries.
    INSERT INTO expression_sessions (
        session_id, child_id, emotion_target, final_accuracy, is_success,
        total_tries, started_at, ended_at, created_at
    )
    VALUES
        ('cccccccc-cccc-cccc-cccc-cccccccccc01', target_child_id, 'happy', 0.82, true, 3, now() - interval '13 days', now() - interval '13 days' + interval '4 minutes', now() - interval '13 days'),
        ('cccccccc-cccc-cccc-cccc-cccccccccc02', target_child_id, 'sad', 0.61, false, 4, now() - interval '9 days', now() - interval '9 days' + interval '5 minutes', now() - interval '9 days'),
        ('cccccccc-cccc-cccc-cccc-cccccccccc03', target_child_id, 'surprise', 0.74, true, 3, now() - interval '7 days', now() - interval '7 days' + interval '4 minutes', now() - interval '7 days'),
        ('cccccccc-cccc-cccc-cccc-cccccccccc04', target_child_id, 'angry', 0.57, false, 4, now() - interval '4 days', now() - interval '4 days' + interval '5 minutes', now() - interval '4 days'),
        ('cccccccc-cccc-cccc-cccc-cccccccccc05', target_child_id, 'happy', 0.88, true, 2, now() - interval '2 days', now() - interval '2 days' + interval '3 minutes', now() - interval '2 days'),
        ('cccccccc-cccc-cccc-cccc-cccccccccc06', target_child_id, 'sad', 0.69, true, 3, now() - interval '12 hours', now() - interval '12 hours' + interval '4 minutes', now() - interval '12 hours');

    INSERT INTO expression_tries (
        try_id, session_id, child_id, try_number, accuracy_score,
        duration_ms, is_success, created_at
    )
    VALUES
        ('ffffffff-ffff-ffff-ffff-ffffffffff01', 'cccccccc-cccc-cccc-cccc-cccccccccc01', target_child_id, 1, 0.65, 6200, false, now() - interval '13 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff02', 'cccccccc-cccc-cccc-cccc-cccccccccc01', target_child_id, 2, 0.78, 5400, true, now() - interval '13 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff03', 'cccccccc-cccc-cccc-cccc-cccccccccc01', target_child_id, 3, 0.82, 4700, true, now() - interval '13 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff04', 'cccccccc-cccc-cccc-cccc-cccccccccc02', target_child_id, 1, 0.42, 7600, false, now() - interval '9 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff05', 'cccccccc-cccc-cccc-cccc-cccccccccc02', target_child_id, 2, 0.55, 7100, false, now() - interval '9 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff06', 'cccccccc-cccc-cccc-cccc-cccccccccc02', target_child_id, 3, 0.61, 6900, false, now() - interval '9 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff07', 'cccccccc-cccc-cccc-cccc-cccccccccc03', target_child_id, 1, 0.62, 6100, false, now() - interval '7 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff08', 'cccccccc-cccc-cccc-cccc-cccccccccc03', target_child_id, 2, 0.71, 5400, true, now() - interval '7 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff09', 'cccccccc-cccc-cccc-cccc-cccccccccc03', target_child_id, 3, 0.74, 4900, true, now() - interval '7 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff10', 'cccccccc-cccc-cccc-cccc-cccccccccc04', target_child_id, 1, 0.39, 7800, false, now() - interval '4 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff11', 'cccccccc-cccc-cccc-cccc-cccccccccc04', target_child_id, 2, 0.48, 7300, false, now() - interval '4 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff12', 'cccccccc-cccc-cccc-cccc-cccccccccc04', target_child_id, 3, 0.55, 7000, false, now() - interval '4 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff13', 'cccccccc-cccc-cccc-cccc-cccccccccc04', target_child_id, 4, 0.57, 6800, false, now() - interval '4 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff14', 'cccccccc-cccc-cccc-cccc-cccccccccc05', target_child_id, 1, 0.80, 4500, true, now() - interval '2 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff15', 'cccccccc-cccc-cccc-cccc-cccccccccc05', target_child_id, 2, 0.88, 3900, true, now() - interval '2 days'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff16', 'cccccccc-cccc-cccc-cccc-cccccccccc06', target_child_id, 1, 0.58, 6700, false, now() - interval '12 hours'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff17', 'cccccccc-cccc-cccc-cccc-cccccccccc06', target_child_id, 2, 0.66, 6100, true, now() - interval '12 hours'),
        ('ffffffff-ffff-ffff-ffff-ffffffffff18', 'cccccccc-cccc-cccc-cccc-cccccccccc06', target_child_id, 3, 0.69, 5600, true, now() - interval '12 hours');

    -- Dashboard/statistics summaries.
    INSERT INTO dialogue_stat_summary (
        id, child_id, theme, score_rate, rapport_index, turn_fatigue,
        score0_rate, score1_rate, score2_rate, session_count,
        ema_value, ema_alpha, consistency_std, option_bias_detected,
        biased_option_order, retry_reduction_rate, updated_at
    )
    VALUES
        ('12121212-1212-1212-1212-121212121201', target_child_id, 'STARTING_CONVERSATION', 0.67, 0.68, 0.16, 0.11, 0.44, 0.45, 3, 0.66, 0.30, 0.13, false, null, 0.30, now()),
        ('12121212-1212-1212-1212-121212121202', target_child_id, 'FINDING_COMMON_GROUND', 0.83, 0.78, 0.10, 0.00, 0.33, 0.67, 1, 0.80, 0.30, 0.08, false, null, 0.35, now()),
        ('12121212-1212-1212-1212-121212121203', target_child_id, 'MAINTAINING_CONVERSATION', 0.67, 0.70, 0.14, 0.00, 0.67, 0.33, 1, 0.68, 0.30, 0.09, false, null, 0.18, now()),
        ('12121212-1212-1212-1212-121212121204', target_child_id, 'RESOLVING_CONFLICT', 0.50, 0.55, 0.22, 0.33, 0.33, 0.34, 1, 0.52, 0.30, 0.16, false, null, 0.10, now());

    INSERT INTO expression_stat_summary (
        id, child_id, emotion_target, success_rate, fluency_index,
        avg_retry, session_count, duration_decrease_rate,
        ci_lower, ci_upper, convergence_speed, valid_session_rate,
        avg_session_duration_sec, retry_reduction_rate, retry_baseline_status,
        updated_at
    )
    VALUES
        ('13131313-1313-1313-1313-131313131301', target_child_id, 'happy', 1.00, 0.84, 1.50, 2, 0.28, 0.73, 0.92, 0.31, 1.00, 210.0, 0.42, 'STABLE', now()),
        ('13131313-1313-1313-1313-131313131302', target_child_id, 'sad', 0.50, 0.64, 3.50, 2, 0.16, 0.44, 0.72, 0.18, 1.00, 285.0, 0.18, 'NEEDS_BASELINE', now()),
        ('13131313-1313-1313-1313-131313131303', target_child_id, 'surprise', 1.00, 0.72, 2.00, 1, 0.19, 0.60, 0.82, 0.22, 1.00, 240.0, 0.25, 'STABLE', now()),
        ('13131313-1313-1313-1313-131313131304', target_child_id, 'angry', 0.00, 0.52, 4.00, 1, 0.06, 0.35, 0.65, 0.10, 1.00, 310.0, 0.05, 'NEEDS_BASELINE', now());

    INSERT INTO dialogue_error_pattern_summary (
        id, child_id, theme, analyzed_turn_count,
        lecturing_rate, criticism_rate, topic_ignore_rate, rejection_rate,
        unclassified_rate, reliability_rate, model_name, last_refreshed_at, updated_at
    )
    VALUES
        ('14141414-1414-1414-1414-141414141401', target_child_id, 'STARTING_CONVERSATION', 9, 0.08, 0.04, 0.38, 0.22, 0.28, 0.72, 'gemini-3-flash-preview', now(), now()),
        ('14141414-1414-1414-1414-141414141402', target_child_id, 'FINDING_COMMON_GROUND', 3, 0.05, 0.05, 0.20, 0.10, 0.60, 0.65, 'gemini-3-flash-preview', now(), now()),
        ('14141414-1414-1414-1414-141414141403', target_child_id, 'MAINTAINING_CONVERSATION', 3, 0.10, 0.05, 0.30, 0.10, 0.45, 0.68, 'gemini-3-flash-preview', now(), now()),
        ('14141414-1414-1414-1414-141414141404', target_child_id, 'RESOLVING_CONFLICT', 3, 0.05, 0.15, 0.15, 0.40, 0.25, 0.70, 'gemini-3-flash-preview', now(), now());

    -- Homework/offline mission records.
    INSERT INTO homework_assignments (
        homework_id, child_id, weekly_progress_id, week, strategy_focus,
        instruction, strategy_tip, strategy_tip_source, due_date, status, created_at
    )
    VALUES
        ('15151515-1515-1515-1515-151515151501', target_child_id, null, 4, 'CONVERSATION_INITIATION',
         '가정에서 보호자와 함께 공통 관심사를 이용해 짧은 질문으로 대화를 시작하는 연습을 한다.',
         '오늘은 가족에게 먼저 좋아하는 주제를 하나 묻고, 상대의 답을 한 번 더 따라 물어보는 연습을 해보세요.',
         'LLM_FLASH', current_date + interval '3 days', 'REVIEWED', now() - interval '7 days'),
        ('15151515-1515-1515-1515-151515151502', target_child_id, null, 3, 'FINDING_COMMON_GROUND',
         '친구 또는 가족과 같은 관심사를 찾고 한 문장으로 이어 말하기를 연습한다.',
         '상대가 좋아하는 것을 하나 듣고, 나도 비슷하게 좋아하는 것을 말해보세요.',
         'LLM_FLASH', current_date + interval '5 days', 'SUBMITTED', now() - interval '4 days'),
        ('15151515-1515-1515-1515-151515151503', target_child_id, null, 11, 'CONFLICT_RESOLUTION',
         '갈등 상황에서 바로 거절하거나 피하기보다 감정을 짧게 말하고 선택지를 제안하는 연습을 한다.',
         '속상한 일이 생기면 "나는 지금 속상해. 잠깐 쉬고 다시 말할래"처럼 감정과 다음 행동을 함께 말해보세요.',
         'LLM_FLASH', current_date + interval '7 days', 'PENDING', now() - interval '1 day');

    INSERT INTO homework_reports (
        report_id, homework_id, reported_by, completed, initiated_by,
        strategy_applied, parent_observation, peer_response_observed,
        spontaneous_flag, reported_at
    )
    VALUES
        ('16161616-1616-1616-1616-161616161601', '15151515-1515-1515-1515-151515151501', reporter_user_id, 'PARTIAL', 'HINT',
         'CONVERSATION_INITIATION',
         '처음에는 질문을 시작하지 못하고 기다렸지만, 보호자가 "무엇을 좋아하는지 물어볼까?"라고 힌트를 주자 "오늘 뭐 하고 놀았어?"라고 질문했다. 두 번째 시도에서는 조금 더 자연스럽게 이어갔다.',
         '가족은 웃으며 답했고, 아이가 한 번 더 "그거 재미있었어?"라고 묻자 대화가 약 1분 정도 이어졌다.',
         false, now() - interval '3 days'),
        ('16161616-1616-1616-1616-161616161602', '15151515-1515-1515-1515-151515151502', reporter_user_id, 'DONE', 'SELF',
         'FINDING_COMMON_GROUND',
         '기차 이야기가 나오자 스스로 "나도 기차 좋아해"라고 말했고, 상대가 좋아하는 색을 묻는 모습이 있었다.',
         '상대가 대답한 뒤 아이가 자신의 경험을 짧게 덧붙이며 대화를 이어갔다.',
         true, now() - interval '1 day');

    RAISE NOTICE 'Seed inserted for child_id=% and reporter_user_id=%', target_child_id, reporter_user_id;
END $$;

COMMIT;

SELECT 'dialogue_sessions' AS table_name, count(*) FROM dialogue_sessions WHERE child_id = '09fe2a07-f517-444a-b9d5-0e09014af639'
UNION ALL
SELECT 'expression_sessions', count(*) FROM expression_sessions WHERE child_id = '09fe2a07-f517-444a-b9d5-0e09014af639'
UNION ALL
SELECT 'homework_assignments', count(*) FROM homework_assignments WHERE child_id = '09fe2a07-f517-444a-b9d5-0e09014af639';
