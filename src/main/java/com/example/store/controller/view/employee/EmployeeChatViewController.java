package com.example.store.controller.view.employee;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee/chat")
public class EmployeeChatViewController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("active", "chat");
        return "employee/chat";
    }
}
