package myexpressionfriend_api.statistics.dialogue.errorpattern.repository;

import myexpressionfriend_api.common.domain.PeersTheme;

import java.time.Instant;
import java.util.UUID;

public interface ZeroScoreTurnProjection {
    UUID getTurnId();
    UUID getChildId();
    PeersTheme getTheme();
    String getScenarioId();
    Integer getTurnNumber();
    Integer getSelectedOptionOrder();
    Instant getStartedAt();
}
