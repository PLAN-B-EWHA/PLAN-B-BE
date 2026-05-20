DO $$
    DECLARE
        v_child_id UUID := '568dd993-269a-473c-91d8-e45b99efb0db'; -- 대상 아동
        v_theme TEXT;
        v_week INT;
        i INT;
        t INT;
        v_session_id UUID;
        v_turn_id UUID;
        v_total_score INT;
        v_selected_score INT;
        v_score_rate FLOAT;
        v_started_at TIMESTAMP;
        v_ended_at TIMESTAMP;
    BEGIN
        FOREACH v_theme IN ARRAY ARRAY[
            '정보 교환하기',
            '대화 유지하기',
            '공통점 찾기',
            '대화 시작하기',
            '갈등 해결하기'
            ]
            LOOP
                v_week := CASE v_theme
                              WHEN '정보 교환하기' THEN 1
                              WHEN '대화 유지하기' THEN 2
                              WHEN '공통점 찾기' THEN 3
                              WHEN '대화 시작하기' THEN 4
                              WHEN '갈등 해결하기' THEN 11
                              ELSE 1
                    END;

                -- 각 주제당 4세션 강제 추가
                FOR i IN 1..4 LOOP
                        v_session_id := gen_random_uuid();
                        v_started_at := now() - ((random() * 168)::int || ' hours')::interval; -- 최근 7일
                        v_ended_at := v_started_at + interval '3 minutes';

                        -- 1) session 먼저 insert
                        INSERT INTO dialogue_sessions (
                            session_id, child_id, scenario_id, theme,
                            total_score, max_score, score_rate,
                            started_at, ended_at, created_at,
                            retry_count, week_number
                        ) VALUES (
                                     v_session_id,
                                     v_child_id,
                                     format('W%02s_FORCE_%s', v_week, substr(replace(v_session_id::text,'-',''),1,8)),
                                     v_theme,
                                     0, 8, 0,
                                     v_started_at, v_ended_at, v_started_at,
                                     (floor(random() * 2))::int,
                                     v_week
                                 );

                        -- 2) turn insert
                        v_total_score := 0;
                        FOR t IN 1..4 LOOP
                                v_turn_id := gen_random_uuid();

                                -- 오답 비율 높게: 0점 70%
                                IF random() < 0.70 THEN
                                    v_selected_score := 0;
                                ELSIF random() < 0.85 THEN
                                    v_selected_score := 1;
                                ELSE
                                    v_selected_score := 2;
                                END IF;

                                v_total_score := v_total_score + v_selected_score;

                                INSERT INTO dialogue_turns (
                                    turn_id, session_id, child_id,
                                    turn_number, attempt_number, selected_option_order, selected_score,
                                    npc_reaction_expression, created_at
                                ) VALUES (
                                             v_turn_id, v_session_id, v_child_id,
                                             t, 1, (floor(random() * 3) + 1)::int, v_selected_score,
                                             CASE WHEN random() < 0.4 THEN 'Joy' ELSE 'Neutral' END,
                                             v_started_at + (t || ' seconds')::interval
                                         );
                            END LOOP;

                        -- 3) 점수 업데이트
                        v_score_rate := v_total_score::float / 8.0;
                        UPDATE dialogue_sessions
                        SET total_score = v_total_score,
                            score_rate = v_score_rate
                        WHERE session_id = v_session_id;
                    END LOOP;
            END LOOP;
    END $$;
