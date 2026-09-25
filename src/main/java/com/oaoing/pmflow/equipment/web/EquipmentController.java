package com.oaoing.pmflow.equipment.web;

import com.oaoing.pmflow.equipment.EquipmentRepository;
import com.oaoing.pmflow.equipment.EquipmentService;
import com.oaoing.pmflow.equipment.domain.Equipment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentResponse> list() {
        return equipmentService.getAllEquipment().stream()
                .map(EquipmentResponse::from)
                .toList();
    }

    @GetMapping("/{code}")
    public EquipmentResponse get(@PathVariable String code) {
        List<Equipment> equipmentList = equipmentService.getEquipmentByCode(code);
        if  (equipmentList.isEmpty()) {
            return null;
        }else{
            return EquipmentResponse.from(equipmentList.getFirst());
        }
    }
}