package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private BookingService bookingService;

    @GetMapping("/email/{id}")
    public String testEmail(@PathVariable Long id) {
        try {
            Optional<Booking> bookingOpt = bookingService.getBookingById(id);
            if (bookingOpt.isEmpty()) {
                return "Booking not found with ID: " + id;
            }
            
            Booking booking = bookingOpt.get();
            emailService.sendBookingConfirmation(booking);
            
            return "Email sent successfully to " + booking.getDonorEmail();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}