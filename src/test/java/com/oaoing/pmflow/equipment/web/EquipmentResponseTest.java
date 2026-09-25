package com.oaoing.pmflow.equipment.web;

import com.oaoing.pmflow.common.domain.Unit;
import com.oaoing.pmflow.equipment.domain.Equipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

public class EquipmentResponseTest {
    @Test
    @DisplayName("Equipment의 모든 필드가 응답으로 복사된다")
    void maps_all_fields() {
        // given: 테스트에 쓸 입력을 준비한다
        Equipment equipment = new Equipment();
        equipment.setCode("EQ-001");
        equipment.setName("공조기 1호");
        equipment.setPeriod(3);
        equipment.setUnit(Unit.MONTH);
        equipment.setDescription("3층 기계실");

        // when: 테스트 대상을 딱 한 번 호출한다
        EquipmentResponse response = EquipmentResponse.from(equipment);

        // then: 결과가 기대와 같은지 확인한다
        assertThat(response.code()).isEqualTo("EQ-001");
        assertThat(response.name()).isEqualTo("공조기 1호");
        assertThat(response.period()).isEqualTo(3);
        assertThat(response.unit()).isEqualTo("MONTH");
        assertThat(response.unitLabel()).isEqualTo("월");
        assertThat(response.description()).isEqualTo("3층 기계실");
    }

    @ParameterizedTest
    @DisplayName("Unit은 name과 label로 나뉘어 응답에 담긴다")
    @EnumSource(value = Unit.class)
    void unit_is_exposed_as_name_and_label(Unit unit) {
        // given: 테스트에 쓸 입력을 준비한다
        Equipment equipment = new Equipment();
        equipment.setUnit(unit);

        // when: 테스트 대상을 딱 한 번 호출한다
        EquipmentResponse response = EquipmentResponse.from(equipment);

        // then: 결과가 기대와 같은지 확인한다
        assertThat(response.unit()).isEqualTo(unit.name());
        assertThat(response.unitLabel()).isEqualTo(unit.getLabel());
    }

    @Test
    @DisplayName("name, description이 null이어도 예외 없이 null로 매핑")
    void nullable_fields() {
        // given: 테스트에 쓸 입력을 준비한다
        Equipment equipment = new Equipment();
        equipment.setCode("EQ-001");
        equipment.setPeriod(3);
        equipment.setUnit(Unit.MONTH);

        // when: 테스트 대상을 딱 한 번 호출한다
        EquipmentResponse response = EquipmentResponse.from(equipment);

        // then: 결과가 기대와 같은지 확인한다
        assertThat(response.code()).isEqualTo("EQ-001");
        assertThat(response.name()).isNull();
        assertThat(response.period()).isEqualTo(3);
        assertThat(response.unit()).isEqualTo("MONTH");
        assertThat(response.unitLabel()).isEqualTo("월");
        assertThat(response.description()).isNull();
    }
}
