package myexpressionfriend_api.game.repository;

import myexpressionfriend_api.game.domain.ExpressionTry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpressionTryRepository extends JpaRepository<ExpressionTry, UUID> {

    List<ExpressionTry> findBySession_SessionIdInOrderByTryNumber(List<UUID> sessionIds);
}
