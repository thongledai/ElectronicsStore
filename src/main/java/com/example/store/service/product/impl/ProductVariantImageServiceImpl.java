package com.example.store.service.product.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.store.common.service.ICloudinaryService;
import com.example.store.common.util.SlugUtils;
import com.example.store.enums.CloudinaryMessage;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.ProductVariantImage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantImageRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.service.product.IProductVariantImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductVariantImageServiceImpl implements IProductVariantImageService {

    private static final int MAX_IMAGES_PER_VARIANT = 10;

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantImageRepository productVariantImageRepository;
    private final ICloudinaryService cloudinaryService;

    @Override
    @Transactional
    public List<String> uploadVariantImages(UUID productId, UUID variantId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(CloudinaryMessage.IMAGE_FILE_LIST_EMPTY.getMessage());
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(
                CloudinaryMessage.PRODUCT_NOT_FOUND.getMessage(), productId)));

        ProductVariant variant = productVariantRepository.findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException(String.format(
                CloudinaryMessage.VARIANT_NOT_FOUND.getMessage(), variantId)));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(CloudinaryMessage.VARIANT_NOT_IN_PRODUCT.getMessage());
        }

        long currentCount = productVariantImageRepository.countByProductVariantId(variantId);
        if (currentCount + files.size() > MAX_IMAGES_PER_VARIANT) {
            throw new BusinessException(String.format(CloudinaryMessage.IMAGE_LIMIT_EXCEEDED.getMessage(),
                    MAX_IMAGES_PER_VARIANT, currentCount));
        }

        String folderSlug = SlugUtils.limitSlug(product.getSlug(), 40);
        String folder = "electronics-store/products/variants/" + folderSlug + "/";

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String url = cloudinaryService.uploadImage(file, folder);
                if (url != null) {
                    ProductVariantImage img = ProductVariantImage.builder()
                            .productVariant(variant)
                            .imageUrl(url)
                            .build();
                    productVariantImageRepository.save(img);
                    uploadedUrls.add(url);
                }
            }
        }

        log.info("Tải lên {} ảnh thành công cho biến thể id={}", uploadedUrls.size(), variantId);
        return uploadedUrls;
    }

    @Override
    @Transactional
    public void deleteVariantImage(UUID productId, UUID variantId, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BusinessException(CloudinaryMessage.IMAGE_URL_EMPTY.getMessage());
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        CloudinaryMessage.VARIANT_NOT_FOUND.getMessage(), variantId)));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(CloudinaryMessage.VARIANT_NOT_IN_PRODUCT.getMessage());
        }

        ProductVariantImage image = productVariantImageRepository.findByProductVariantIdAndImageUrl(variantId, imageUrl.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        CloudinaryMessage.VARIANT_IMAGE_NOT_FOUND.getMessage()));

        productVariantImageRepository.delete(image);
        // Xóa ảnh trên Cloudinary
        cloudinaryService.deleteImageByUrl(imageUrl.trim());
        log.info("Đã xóa ảnh {} của biến thể id={}", imageUrl, variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getVariantImageUrls(UUID variantId) {
        return productVariantImageRepository.findByProductVariantIdOrderByImageUrlAsc(variantId).stream()
                .map(ProductVariantImage::getImageUrl)
                .toList();
    }
}
