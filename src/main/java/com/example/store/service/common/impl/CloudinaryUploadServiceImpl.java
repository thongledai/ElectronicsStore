package com.example.store.service.common.impl;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.store.enums.CloudinaryMessage;
import com.example.store.service.common.ICloudinaryUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUploadServiceImpl implements ICloudinaryUploadService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(CloudinaryMessage.IMAGE_FILE_EMPTY.getMessage());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder != null && !folder.trim().isEmpty() ? folder : "reviews",
                            "resource_type", "image"
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException(CloudinaryMessage.UPLOAD_FAILED_MSG.format(e.getMessage()), e);
        }
    }
}
