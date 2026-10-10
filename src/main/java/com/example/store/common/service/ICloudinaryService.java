package com.example.store.common.service;

import org.springframework.web.multipart.MultipartFile;

public interface ICloudinaryService {
    String uploadImage(MultipartFile file, String folder);
    boolean deleteImageByPublicId(String publicId);
    boolean deleteImageByUrl(String imageUrl);
}
