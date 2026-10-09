package com.example.store.controller.view.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoryManagerViewController {

    @GetMapping("/manager/categories")
    public String categoryManagerPage() {
        return "manager/categories";
    }
}
