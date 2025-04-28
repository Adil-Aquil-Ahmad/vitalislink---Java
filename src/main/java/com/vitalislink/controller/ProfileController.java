package com.vitalislink.controller;

import com.vitalislink.model.User;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get user
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Add user to model
        model.addAttribute("user", user);
        
        return "myprofile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute User updatedUser, 
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get original user
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        User originalUser = userOpt.get();
        
        // Update fields (keeping original ID and admin status)
        updatedUser.setId(originalUser.getId());
        updatedUser.setAdmin(originalUser.isAdmin());
        updatedUser.setPassword(originalUser.getPassword()); // Keep original password
        
        // Save updated user
        userService.updateUser(updatedUser);
        
        // Update session if email changed
        if (!email.equals(updatedUser.getEmail())) {
            session.setAttribute("userEmail", updatedUser.getEmail());
        }
        
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully");
        return "redirect:/profile";
    }
    
    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        return "change_password";
    }
    
    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get user
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (!userOpt.isPresent()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Current password is incorrect");
            return "redirect:/profile/change-password";
        }
        
        // Validate new password
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New passwords do not match");
            return "redirect:/profile/change-password";
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);
        
        redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        return "redirect:/profile";
    }
}