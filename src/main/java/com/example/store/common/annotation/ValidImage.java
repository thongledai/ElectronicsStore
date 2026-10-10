package com.example.store.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = ValidImageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImage {
    String message() default "File tải lên phải là hình ảnh (JPG, PNG, WEBP) và có dung lượng tối đa 10MB";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean required() default false;
}
