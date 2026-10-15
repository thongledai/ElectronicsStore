package com.example.store.service.delivery;

import java.util.List;

import com.example.store.dto.ghn.GhnAddressOptionDTO;

public interface IGhnAddressService {
    List<GhnAddressOptionDTO> getProvinces();
    List<GhnAddressOptionDTO> getWards(Integer provinceId);
}
