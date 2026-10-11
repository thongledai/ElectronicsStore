package com.example.store.controller.view.customer;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.store.dto.product.ProductDetailResponseDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.service.product.IProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductDetailViewController {

    private final IProductService productService;

    @GetMapping("/customer/products/{slug}")
    public String productDetailPage(@PathVariable String slug, Model model) {
        ProductDetailResponseDTO product = productService.getPublicProductDetailBySlug(slug);
        List<ProductResponseDTO> relatedProducts = productService.getRelatedProducts(
                product.getId(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                4
        );

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        return "customer/product-details";
    }

    @GetMapping("/customer/product-details")
    public String productDetailLegacyRedirect(@RequestParam(required = false) String slug) {
        if (slug != null && !slug.trim().isEmpty()) {
            return "redirect:/customer/products/" + slug.trim();
        }
        return "redirect:/customer/products";
    }
}
