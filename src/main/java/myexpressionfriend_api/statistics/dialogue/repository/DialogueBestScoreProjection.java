package myexpressionfriend_api.statistics.dialogue.repository;

import myexpressionfriend_api.common.domain.PeersTheme;

public interface DialogueBestScoreProjection {
    PeersTheme getTheme();
    Float getBestScoreRate();
}
