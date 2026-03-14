package com.planB.myexpressionfriend.unity.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "unity_expression_mission_details")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnityExpressionMissionDetail {

    @Id
    @Column(name = "unity_mission_id")
    private Long unityMissionId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unity_mission_id")
    private UnityMission mission;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expression_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode expressionData;
}