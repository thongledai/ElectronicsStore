package com.example.store.controller.view.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StyleManagerViewController {

    @GetMapping("/manager/styles")
    public String styleManagerPage() {
        return "manager/styles";
    }
}
