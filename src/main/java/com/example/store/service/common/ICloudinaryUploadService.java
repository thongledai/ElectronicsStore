package com.example.store.service.common;

import org.springframework.web.multipart.MultipartFile;

public interface ICloudinaryUploadService {
    String uploadImage(MultipartFile file, String folder);
}
