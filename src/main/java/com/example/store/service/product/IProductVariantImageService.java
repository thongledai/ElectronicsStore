package com.example.store.service.product;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface IProductVariantImageService {

    List<String> uploadVariantImages(UUID productId, UUID variantId, List<MultipartFile> files);

    void deleteVariantImage(UUID productId, UUID variantId, String imageUrl);

    List<String> getVariantImageUrls(UUID variantId);
}
