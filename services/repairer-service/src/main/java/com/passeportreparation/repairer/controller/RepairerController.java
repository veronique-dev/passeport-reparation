package com.passeportreparation.repairer.controller;

import com.passeportreparation.common.dto.RepairerDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.repairer.service.RepairerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repairers")
@RequiredArgsConstructor
public class RepairerController {

    private final RepairerService repairerService;

    @GetMapping
    public List<RepairerDto> list(
            @RequestParam(required = false) ApplianceCategory category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return repairerService.findNearby(category, city, lat, lng);
    }
}
