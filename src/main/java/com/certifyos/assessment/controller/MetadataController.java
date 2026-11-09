package com.certifyos.assessment.controller;

import com.certifyos.assessment.dto.MetadataRequest;
import com.certifyos.assessment.dto.MetadataResponse;
import com.certifyos.assessment.service.MetadataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping
    public MetadataResponse fetchMetadata(@Valid @RequestBody MetadataRequest request) {
        return metadataService.fetchMetadata(request.url());
    }
}
