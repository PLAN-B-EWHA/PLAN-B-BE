package myexpressionfriend_api.unity.repository;

import myexpressionfriend_api.unity.domain.UnityGameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnityGameResultRepository extends JpaRepository<UnityGameResult, Long> {

    /** 아동의 최근 Unity 게임 결과 조회 (프롬프트 컨텍스트 구성용) */
    List<UnityGameResult> findTop5ByGameSession_Child_ChildIdOrderByCreatedAtDesc(UUID childId);
}
