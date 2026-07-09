package com.passeportreparation.common.dto;

import com.passeportreparation.common.enums.ApplianceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairerDto {
    private UUID id;
    private String name;
    private String city;
    private String phone;
    private String email;
    private String whatsapp;
    private List<ApplianceCategory> categories;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
}
