ALTER TABLE rag_sources
DROP CONSTRAINT IF EXISTS rag_sources_use_case_check;

ALTER TABLE rag_sources
ADD CONSTRAINT rag_sources_use_case_check
CHECK (use_case IN (
    'GENERAL_REFERENCE',
    'REPORT_GENERATION',
    'OFFLINE_MISSION_GENERATION',
    'SCENARIO_GENERATION'
));
