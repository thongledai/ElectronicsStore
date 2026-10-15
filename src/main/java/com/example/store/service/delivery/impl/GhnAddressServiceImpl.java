package com.example.store.service.delivery.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.example.store.dto.ghn.GhnAddressData;
import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.dto.ghn.GhnApiResponse;
import com.example.store.exception.BusinessException;
import com.example.store.enums.DeliveryMessage;
import com.example.store.service.delivery.IGhnAddressService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GhnAddressServiceImpl implements IGhnAddressService {
    private static final String PROVINCES = "/shiip/public-api/v3/master-data/province/all?offset=0&limit=200";
    private static final String WARDS = "/shiip/public-api/v3/master-data/ward/all-by-province-id";
    private static final ParameterizedTypeReference<GhnApiResponse<List<GhnAddressData>>> TYPE = new ParameterizedTypeReference<>() {};
    private final RestClient restClient;

    public GhnAddressServiceImpl(@Qualifier("ghnRestClient") RestClient restClient) { this.restClient = restClient; }

    @Override public List<GhnAddressOptionDTO> getProvinces() { return getCatalog(PROVINCES); }

    @Override public List<GhnAddressOptionDTO> getWards(Integer provinceId) {
        if (provinceId == null || provinceId <= 0) throw new BusinessException(DeliveryMessage.ADDRESS_PROVINCE_REQUIRED.getMessage());
        return getCatalog(WARDS + "?province_id=" + provinceId + "&offset=0&limit=200");
    }

    private List<GhnAddressOptionDTO> getCatalog(String uri) {
        try {
            GhnApiResponse<List<GhnAddressData>> response = restClient.get().uri(uri).retrieve().body(TYPE);
            if (response == null || response.getCode() == null || response.getCode() != 200 || response.getData() == null) {
                throw new BusinessException(DeliveryMessage.ADDRESS_CATALOG_INVALID.getMessage());
            }
            return response.getData().stream()
                    .filter(item -> item.getStatus() == null || item.getStatus() == 1)
                    .filter(item -> item.getId() != null && StringUtils.hasText(item.getName()))
                    .map(item -> GhnAddressOptionDTO.builder().id(item.getId()).name(item.getName()).extensionNames(item.getExtensionNames()).build())
                    .toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("GHN address catalog failed at {}: {}", uri, ex.getMessage());
            throw new BusinessException(DeliveryMessage.ADDRESS_CATALOG_LOAD_FAILED.getMessage());
        }
    }
}
