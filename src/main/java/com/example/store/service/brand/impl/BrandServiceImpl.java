package com.example.store.service.brand.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.service.ICloudinaryService;
import com.example.store.common.util.SlugUtils;
import com.example.store.dto.brand.BrandOptionDTO;
import com.example.store.dto.brand.BrandRequestDTO;
import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.dto.common.PageResponse;
import com.example.store.entity.Brand;
import com.example.store.exception.BusinessException;
import com.example.store.exception.DuplicateResourceException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.BrandMapper;
import com.example.store.repository.BrandRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.service.brand.IBrandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandServiceImpl implements IBrandService {

    private static final String CLOUDINARY_FOLDER = "electronics-store/brands/";

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;
    private final ICloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponseDTO> getAllBrands(String search, Boolean isActive, Pageable pageable) {
        Page<Brand> page = brandRepository.search(search, isActive, pageable);
        List<BrandResponseDTO> dtoList = page.getContent().stream()
                .map(brandMapper::toResponseDTO)
                .toList();
        return PageResponse.of(page, dtoList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponseDTO> getActiveBrands() {
        return brandRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(brandMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandOptionDTO> getBrandOptions() {
        return brandRepository.findByIsActiveTrue().stream()
                .map(b -> BrandOptionDTO.builder()
                        .id(b.getId())
                        .name(b.getName())
                        .slug(b.getSlug())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));
        return brandMapper.toResponseDTO(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponseDTO getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với slug: " + slug));
        return brandMapper.toResponseDTO(brand);
    }

    @Override
    @Transactional
    public BrandResponseDTO createBrand(BrandRequestDTO requestDTO, MultipartFile logoFile) {
        String slug = generateAndValidateSlug(requestDTO.getName(), null);

        if (brandRepository.existsByName(requestDTO.getName())) {
            throw new DuplicateResourceException("Tên thương hiệu đã tồn tại: " + requestDTO.getName());
        }

        String logoUrl = null;
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = cloudinaryService.uploadImage(logoFile, CLOUDINARY_FOLDER);
        } else if (requestDTO.getLogoUrl() != null && !requestDTO.getLogoUrl().trim().isEmpty()) {
            logoUrl = requestDTO.getLogoUrl().trim();
        }

        Brand brand = Brand.builder()
                .name(requestDTO.getName().trim())
                .slug(slug)
                .logoUrl(logoUrl)
                .description(requestDTO.getDescription())
                .isActive(requestDTO.getIsActive() != null ? requestDTO.getIsActive() : true)
                .build();

        Brand saved = brandRepository.save(brand);
        log.info("Tạo mới thương hiệu thành công: id={}, name={}", saved.getId(), saved.getName());
        return brandMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public BrandResponseDTO updateBrand(Long id, BrandRequestDTO requestDTO, MultipartFile logoFile) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));

        if (brandRepository.existsByNameAndIdNot(requestDTO.getName(), id)) {
            throw new DuplicateResourceException("Tên thương hiệu đã tồn tại: " + requestDTO.getName());
        }

        // Chỉ tạo lại slug khi đổi tên, tránh làm hỏng đường dẫn cũ
        String slug = (requestDTO.getName().trim().equals(brand.getName())
                && brand.getSlug() != null && !brand.getSlug().isBlank())
                        ? brand.getSlug()
                        : generateAndValidateSlug(requestDTO.getName(), id);

        if (logoFile != null && !logoFile.isEmpty()) {
            // Xóa logo cũ trên Cloudinary nếu có
            if (brand.getLogoUrl() != null) {
                cloudinaryService.deleteImageByUrl(brand.getLogoUrl());
            }
            brand.setLogoUrl(cloudinaryService.uploadImage(logoFile, CLOUDINARY_FOLDER));
        } else if (requestDTO.getLogoUrl() != null) {
            brand.setLogoUrl(requestDTO.getLogoUrl().trim());
        }

        brand.setName(requestDTO.getName().trim());
        brand.setSlug(slug);
        brand.setDescription(requestDTO.getDescription());
        if (requestDTO.getIsActive() != null) {
            if (!requestDTO.getIsActive() && !Boolean.FALSE.equals(brand.getIsActive())
                    && productRepository.existsByBrandId(id)) {
                throw new BusinessException("Không thể xóa mềm thương hiệu vì vẫn còn sản phẩm tham chiếu!");
            }
            brand.setIsActive(requestDTO.getIsActive());
        }

        Brand updated = brandRepository.save(brand);
        log.info("Cập nhật thương hiệu thành công: id={}, name={}", updated.getId(), updated.getName());
        return brandMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));

        if (productRepository.existsByBrandId(id)) {
            throw new BusinessException("Không thể xóa vĩnh viễn thương hiệu vì vẫn còn sản phẩm tham chiếu!");
        }

        brandRepository.delete(brand);
        log.info("Xóa vĩnh viễn thương hiệu id={}", id);
    }

    @Override
    @Transactional
    public void restoreBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));

        brand.setIsActive(true);
        brandRepository.save(brand);
        log.info("Kích hoạt lại thương hiệu id={}", id);
    }

    // Slug luôn được tự tạo từ tên (không còn nhập tay). Trùng thì thêm hậu tố -1,
    // -2...
    private String generateAndValidateSlug(String name, Long currentId) {
        String baseSlug = SlugUtils.toSlug(name);
        if (baseSlug.isEmpty()) {
            baseSlug = "item";
        }

        String slug = baseSlug;
        int count = 1;
        while (currentId == null ? brandRepository.existsBySlug(slug)
                : brandRepository.existsBySlugAndIdNot(slug, currentId)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }
}
