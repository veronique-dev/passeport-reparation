package com.passeportreparation.diagnosis.controller;

import com.passeportreparation.common.dto.DiagnosisRequest;
import com.passeportreparation.common.dto.DiagnosisResponse;
import com.passeportreparation.common.dto.IssueOptionDto;
import com.passeportreparation.common.enums.ApplianceCategory;
import com.passeportreparation.diagnosis.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @GetMapping("/issues")
    public List<IssueOptionDto> issues(@RequestParam ApplianceCategory category) {
        return diagnosisService.listIssues(category);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse create(@Valid @RequestBody DiagnosisRequest request) {
        return diagnosisService.diagnose(request);
    }

    @GetMapping("/{id}")
    public DiagnosisResponse get(@PathVariable UUID id) {
        return diagnosisService.getById(id);
    }
}
