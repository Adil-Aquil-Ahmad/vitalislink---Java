package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;  // Add this import
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.DonorService;  // Add this import
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private DonorService donorService;  // Add this service injection

    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // If user is admin, redirect to admin dashboard
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin != null && isAdmin) {
            return "redirect:/admin/dashboard";
        }
        
        // Get only donors with completed donations (those with a lastDonationDate)
        List<Donor> donors = donorService.getAllDonors().stream()
                .filter(donor -> donor.getLastDonationDate() != null)
                .collect(Collectors.toList());
        
        // Get user details for location/blood group matching
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            model.addAttribute("userLatitude", user.getLatitude());
            model.addAttribute("userLongitude", user.getLongitude());
            model.addAttribute("userBloodGroup", user.getBloodGroup());
        }
        
        model.addAttribute("donors", donors);
        return "dashboard";
    }
    
    // REMOVE THIS METHOD to fix the ambiguous mapping
    /* 
    @GetMapping("/my-bookings")
    public String myBookings(Model model, HttpSession session) {
        // This conflicts with the same method in BookingController
    }
    */
}