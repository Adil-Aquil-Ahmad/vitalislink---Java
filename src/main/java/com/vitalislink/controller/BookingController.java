package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class BookingController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/booking/{id}")
    public String viewBooking(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get booking
        Optional<Booking> bookingOpt = bookingService.getBookingById(id);
        if (bookingOpt.isEmpty()) {
            model.addAttribute("error", "Booking not found");
            return "redirect:/my-bookings";
        }
        
        Booking booking = bookingOpt.get();
        
        // Get user
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Check if user has access to this booking (if it's their booking or they're an admin)
        if (!user.isAdmin() && (booking.getUser() == null || !booking.getUser().getId().equals(user.getId()))) {
            model.addAttribute("error", "You don't have permission to view this booking");
            return "redirect:/my-bookings";
        }
        
        // Add data to model
        model.addAttribute("booking", booking);
        
        return "view_booking";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get user
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // Get bookings for user - debugging
        List<Booking> bookings = bookingService.getBookingsByUser(user);
        System.out.println("Found " + bookings.size() + " bookings for user " + user.getEmail());
        
        // Add data to model
        model.addAttribute("bookings", bookings);
        
        return "my_bookings";
    }
}