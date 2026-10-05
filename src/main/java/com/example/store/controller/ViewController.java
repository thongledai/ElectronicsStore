package com.example.store.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

	// Trang chủ: chuyển hướng về /customer/index
	@GetMapping("/")
	public String home() {
		return "redirect:/customer/index";
	}

	// Các trang của Customer: /customer/index, /customer/products, /customer/cart,
	// v.v.
	@GetMapping("/customer/{page}")
	public String customerPages(@PathVariable String page) {
		return "customer/" + page;
	}

	// Các trang thông tin: /pages/about, /pages/contact
	@GetMapping("/pages/{page}")
	public String staticPages(@PathVariable String page) {
		return "pages/" + page;
	}

	// Trang đăng nhập: /auth/login
	@GetMapping("/auth/{page}")
	public String authPages(@PathVariable String page) {
		return "auth/" + page;
	}

	// Dashboard Quản lý: /manager/{page}
	@GetMapping("/manager/{page}")
	public String managerPages(@PathVariable String page) {
		return "manager/" + page;
	}

	// Dashboard Nhân viên: /employee/{page}
	@GetMapping("/employee/{page}")
	public String employeePages(@PathVariable String page) {
		return "employee/" + page;
	}

	// Dashboard Shipper: /shipper/{page}
	@GetMapping("/shipper/{page}")
	public String shipperPages(@PathVariable String page) {
		return "shipper/" + page;
	}
}
