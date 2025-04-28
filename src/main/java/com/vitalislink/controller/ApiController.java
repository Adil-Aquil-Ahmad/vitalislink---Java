package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private EmailService emailService;

    @GetMapping("/booking/{id}/send-email")
    public ResponseEntity<?> sendBookingEmail(@PathVariable Long id, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return ResponseEntity.status(401).body("You must be logged in to send emails");
        }
        
        try {
            // Get booking
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(404).body("Booking not found");
            }
            
            Booking booking = bookingOpt.get();
            
            // Use the same method that works in TestController
            emailService.sendBookingConfirmation(booking);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email sent successfully with PDF attachment");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error sending email: " + e.getMessage());
        }
    }
}