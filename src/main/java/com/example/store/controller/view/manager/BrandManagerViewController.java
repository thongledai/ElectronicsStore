package com.example.store.controller.view.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BrandManagerViewController {

    @GetMapping("/manager/brands")
    public String brandManagerPage() {
        return "manager/brands";
    }
}
