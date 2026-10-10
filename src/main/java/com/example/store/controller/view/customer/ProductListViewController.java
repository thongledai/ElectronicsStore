package com.example.store.controller.view.customer;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.dto.category.CategoryResponseDTO;
import com.example.store.dto.common.PageResponse;
import com.example.store.dto.product.ProductFilterDTO;
import com.example.store.dto.product.ProductResponseDTO;
import com.example.store.service.brand.IBrandService;
import com.example.store.service.category.ICategoryService;
import com.example.store.service.product.IProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductListViewController {

    private final IProductService productService;
    private final ICategoryService categoryService;
    private final IBrandService brandService;

    @GetMapping("/customer/products")
    public String productListPage(
            @ModelAttribute ProductFilterDTO filterDTO,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        // Chuyển đổi page từ 1-indexed sang 0-indexed cho service
        int zeroIndexedPage = Math.max(0, page - 1);
        filterDTO.setPage(zeroIndexedPage);
        filterDTO.setSize(12);

        PageResponse<ProductResponseDTO> productPage = productService.getPublicProducts(filterDTO);
        List<CategoryResponseDTO> activeCategories = categoryService.getActiveCategories();
        List<BrandResponseDTO> activeBrands = brandService.getActiveBrands();
        BigDecimal maxPriceDb = productService.getMaxEffectivePrice();

        model.addAttribute("products", productPage);
        model.addAttribute("categories", activeCategories);
        model.addAttribute("brands", activeBrands);
        model.addAttribute("maxPriceDb", maxPriceDb);
        model.addAttribute("filter", filterDTO);
        model.addAttribute("currentPage", page);

        return "customer/products";
    }
}
