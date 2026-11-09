package com.certifyos.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record MetadataRequest(
        @NotBlank(message = "URL is required")
        String url
) {
}
