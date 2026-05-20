UPDATE dialogue_sessions
SET theme = 'FINDING_COMMON_GROUND'
WHERE theme = '怨듯넻??李얘린';

UPDATE dialogue_stat_summary
SET theme = 'FINDING_COMMON_GROUND'
WHERE theme = '怨듯넻??李얘린';

UPDATE dialogue_error_pattern_summary
SET theme = 'FINDING_COMMON_GROUND'
WHERE theme = '怨듯넻??李얘린';


UPDATE dialogue_sessions
SET theme = 'STARTING_CONVERSATION'
WHERE scenario_id LIKE 'W04_DEMO_START_%';

UPDATE dialogue_stat_summary
SET theme = 'STARTING_CONVERSATION'
WHERE id = '12121212-1212-1212-1212-121212121201';

UPDATE dialogue_error_pattern_summary
SET theme = 'STARTING_CONVERSATION'
WHERE id = '14141414-1414-1414-1414-141414141401';

