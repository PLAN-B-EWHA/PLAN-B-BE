package myexpressionfriend_api.player.repository;

import myexpressionfriend_api.player.domain.GamePlayerSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GamePlayerSelectionRepository extends JpaRepository<GamePlayerSelection, UUID> {

    Optional<GamePlayerSelection> findByUser_UserId(UUID userId);

    @Query("""
        SELECT gps FROM GamePlayerSelection gps
        JOIN FETCH gps.child c
        WHERE gps.user.userId = :userId
        """)
    Optional<GamePlayerSelection> findByUserIdWithChild(@Param("userId") UUID userId);
}