package com.certifyos.assessment.service;

import com.certifyos.assessment.dto.MetadataResponse;

public interface MetadataService {

    MetadataResponse fetchMetadata(String url);
}
