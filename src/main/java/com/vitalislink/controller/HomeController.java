package com.vitalislink.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("userEmail", email);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        
        return "home";
    }
    
    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email != null) {
            model.addAttribute("isLoggedIn", true);
            model.addAttribute("userEmail", email);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        
        return "about";
    }
}