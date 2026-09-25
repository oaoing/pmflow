package com.oaoing.pmflow.common.config;

import com.oaoing.pmflow.BaseIT;
import com.oaoing.pmflow.common.domain.Unit;
import com.oaoing.pmflow.equipment.EquipmentRepository;
import com.oaoing.pmflow.equipment.domain.Equipment;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class AuditingIT extends BaseIT {
    private final EquipmentRepository equipmentRepository;
    private final EntityManager entityManager;

    @Autowired
    public AuditingIT(EquipmentRepository equipmentRepository,  EntityManager entityManager) {
        this.equipmentRepository = equipmentRepository;
        this.entityManager = entityManager;
    }

    @Test
    @DisplayName("Audit으로 생성/수정 날짜와 사용자 검증")
    void checkAudit() {
        // give
        Equipment equipment = new Equipment();
        equipment.setCode("EQ-TEST-01");
        equipment.setName("테스트설비01");
        equipment.setPeriod(2);
        equipment.setUnit(Unit.MONTH);

        // when
        Equipment response1 = equipmentRepository.saveAndFlush(equipment);
        entityManager.clear();

        Equipment response2 = equipmentRepository.findById(response1.getId()).orElseThrow();

        // then
        assertThat(response1.getCreatedAt()).isNotNull();
        assertThat(response1.getModifiedAt()).isNotNull();
        assertThat(response1.getCreatedBy()).isEqualTo("system");
        assertThat(response1.getModifiedBy()).isEqualTo("system");

        assertThat(response2.getCreatedAt()).isNotNull();
        assertThat(response2.getModifiedAt()).isNotNull();
        assertThat(response2.getCreatedBy()).isEqualTo("system");
        assertThat(response2.getModifiedBy()).isEqualTo("system");

        // give
        response2.setName("테스트설비01(수정)");
        Instant createdAt = response2.getCreatedAt();
        Instant modifiedAt = response2.getModifiedAt();

        // when
        Equipment response3 = equipmentRepository.saveAndFlush(response2);
        entityManager.clear();
        Equipment response4 = equipmentRepository.findById(response3.getId()).orElseThrow();

        // then
        assertThat(response4.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response4.getModifiedAt()).isNotEqualTo(modifiedAt);
        assertThat(response4.getCreatedBy()).isEqualTo("system");
        assertThat(response4.getModifiedBy()).isEqualTo("system");
    }
}
