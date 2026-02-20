package com.planB.myexpressionfriend.common.repository;

import com.planB.myexpressionfriend.common.domain.user.RoleChangeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleChangeHistoryRepository extends JpaRepository<RoleChangeHistory, UUID> {
}
