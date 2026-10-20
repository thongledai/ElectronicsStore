package com.example.store.controller;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.service.delivery.IGhnAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/address")
public class AddressController {

    private final IGhnAddressService ghnAddressService;

    @GetMapping("/provinces")
    public ApiResponse<List<GhnAddressOptionDTO>> provinces() {
        return ApiResponse.success("OK", ghnAddressService.getProvinces());
    }

    @GetMapping("/wards")
    public ApiResponse<List<GhnAddressOptionDTO>> wards(@RequestParam Integer provinceId) {
        return ApiResponse.success("OK", ghnAddressService.getWards(provinceId));
    }
}
