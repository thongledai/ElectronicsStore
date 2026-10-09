package com.example.store.controller.view.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductManagerViewController {

    @GetMapping("/manager/products")
    public String productManagerPage() {
        return "manager/products";
    }
}
