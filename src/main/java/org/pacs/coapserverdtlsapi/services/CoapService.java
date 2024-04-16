package org.pacs.coapserverdtlsapi.services;

import lombok.RequiredArgsConstructor;
import org.pacs.coapserverdtlsapi.models.AccessResponseModel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoapService {

    private final ExternalApiService apiService;

    public AccessResponseModel sendAccessRequest(String endpoint, String requestPayload) {
        return apiService.sendAccessRequest(endpoint, requestPayload);
    }
}
