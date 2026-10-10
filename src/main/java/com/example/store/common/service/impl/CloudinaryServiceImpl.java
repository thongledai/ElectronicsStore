package com.example.store.common.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.store.common.service.ICloudinaryService;
import com.example.store.common.util.SlugUtils;
import com.example.store.enums.CloudinaryMessage;
import com.example.store.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements ICloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            // Đặt tên file ngắn theo quy tắc: {epochMillis}-{uuid không dấu gạch}
            String uniqueName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "");
            
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", uniqueName,
                            "overwrite", true,
                            "resource_type", "image"
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary: ", e);
            throw new BusinessException(CloudinaryMessage.UPLOAD_FAILED.getMessage());
        }
    }

    @Override
    public boolean deleteImageByPublicId(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            return false;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String res = (String) result.get("result");
            log.info("Xóa ảnh Cloudinary public_id={}: kết quả={}", publicId, res);
            return "ok".equalsIgnoreCase(res);
        } catch (IOException e) {
            log.warn("Không thể xóa ảnh trên Cloudinary với public_id={}: {}", publicId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        String publicId = SlugUtils.extractPublicId(imageUrl);
        if (publicId != null && !publicId.trim().isEmpty()) {
            return deleteImageByPublicId(publicId);
        }
        return false;
    }
}
