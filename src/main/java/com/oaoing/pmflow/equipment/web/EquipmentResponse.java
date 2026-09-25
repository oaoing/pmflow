package com.oaoing.pmflow.equipment.web;

import com.oaoing.pmflow.equipment.domain.Equipment;

public record EquipmentResponse(
        String code,
        String name,
        double period,
        String unit,          // enum 이름 그대로: "DAY"
        String unitLabel,
        String description
) {
    public static EquipmentResponse from(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getCode(),
                equipment.getName(),
                equipment.getPeriod(),
                equipment.getUnit().name(),
                equipment.getUnit().getLabel(),
                equipment.getDescription()
        );
    }
}