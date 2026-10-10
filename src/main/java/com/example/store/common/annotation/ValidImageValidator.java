
package com.example.store.common.annotation;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/jpg");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private boolean required;

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return !required;
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Dung lượng file vượt quá giới hạn tối đa 10MB")
                    .addConstraintViolation();
            return false;
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Định dạng file không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
