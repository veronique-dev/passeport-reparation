package com.passeportreparation.repairer.service;

import com.passeportreparation.common.dto.RepairerDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.repairer.entity.Repairer;
import com.passeportreparation.repairer.repository.RepairerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepairerService {

    private final RepairerRepository repository;

    @Transactional(readOnly = true)
    public List<RepairerDto> findNearby(ApplianceCategory category, String city, Double lat, Double lng) {
        List<Repairer> repairers = repository.search(category, city);
        return repairers.stream()
                .map(r -> toDto(r, lat, lng))
                .sorted(Comparator.comparing(RepairerDto::getDistanceKm, Comparator.nullsLast(Double::compareTo)))
                .toList();
    }

    private static RepairerDto toDto(Repairer r, Double lat, Double lng) {
        Double distance = null;
        if (lat != null && lng != null && r.getLatitude() != null && r.getLongitude() != null) {
            distance = haversineKm(lat, lng, r.getLatitude(), r.getLongitude());
        }
        return RepairerDto.builder()
                .id(r.getId())
                .name(r.getName())
                .city(r.getCity())
                .phone(r.getPhone())
                .email(r.getEmail())
                .whatsapp(r.getWhatsapp())
                .categories(r.getCategories().stream().sorted().toList())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .distanceKm(distance)
                .build();
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }
}
