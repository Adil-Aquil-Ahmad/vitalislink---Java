package com.vitalislink.controller;

import com.vitalislink.model.User;
import com.vitalislink.service.EmailService;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        // Check if user is already logged in
        if (session.getAttribute("userEmail") != null) {
            return "redirect:/dashboard";
        }
        
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, 
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        
        System.out.println("Login attempt for: " + email);
        
        // Authenticate user
        if (userService.authenticateUser(email, password)) {
            // Set user in session
            session.setAttribute("userEmail", email);
            
            // Check if admin
            boolean isAdmin = userService.isAdmin(email);
            session.setAttribute("isAdmin", isAdmin);
            
            System.out.println("Login successful for: " + email + ", isAdmin: " + isAdmin);
            
            if (isAdmin) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/dashboard";
            }
        } else {
            // Authentication failed
            System.out.println("Login failed for: " + email);
            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpSession session) {
        // Check if user is already logged in
        if (session.getAttribute("userEmail") != null) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, 
                          RedirectAttributes redirectAttributes) {
        try {
            // Register user
            User registeredUser = userService.registerUser(user);
            
            // Send welcome email
            emailService.sendWelcomeEmail(registeredUser);
            
            // Success message
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
            
        } catch (Exception e) {
            // Error message
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Invalidate session
        session.invalidate();
        return "redirect:/";
    }
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot_password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, 
                                       RedirectAttributes redirectAttributes) {
        
        // Check if email exists
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            // Send password reset email (in a real app, this would generate a token)
            emailService.sendSimpleMessage(email, 
                "Password Reset Request", 
                "Please reset your password by clicking on this link: http://localhost:8080/reset-password");
            
            redirectAttributes.addFlashAttribute("success", "Password reset instructions sent to your email");
        } else {
            redirectAttributes.addFlashAttribute("error", "Email not found");
        }
        
        return "redirect:/forgot-password";
    }
}