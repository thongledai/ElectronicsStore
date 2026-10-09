package com.example.store.service.brand;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.dto.brand.BrandOptionDTO;
import com.example.store.dto.brand.BrandRequestDTO;
import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.dto.common.PageResponse;

public interface IBrandService {

    PageResponse<BrandResponseDTO> getAllBrands(String search, Boolean isActive, Pageable pageable);

    List<BrandResponseDTO> getActiveBrands();

    List<BrandOptionDTO> getBrandOptions();

    BrandResponseDTO getBrandById(Long id);

    BrandResponseDTO getBrandBySlug(String slug);

    BrandResponseDTO createBrand(BrandRequestDTO requestDTO, MultipartFile logoFile);

    BrandResponseDTO updateBrand(Long id, BrandRequestDTO requestDTO, MultipartFile logoFile);

    void deleteBrand(Long id);

    void restoreBrand(Long id);
}
