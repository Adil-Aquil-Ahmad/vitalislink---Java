package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Controller
public class DonorBookingController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/donor/book")
    public String showBookingForm(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("booking", new Booking());
        return "donor_booking";
    }

    @PostMapping("/book")
    public String bookAppointment(
            @ModelAttribute("booking") Booking booking,
            @RequestParam("bookingDate") String date,
            @RequestParam("bookingTime") String time,
            HttpSession session, Model model) {
        
        try {
            // Get logged in user
            String email = (String) session.getAttribute("userEmail");
            Optional<User> userOpt = userService.getUserByEmail(email);
            
            if (userOpt.isEmpty()) {
                model.addAttribute("error", "You must be logged in to book an appointment");
                return "redirect:/login";
            }
            
            User user = userOpt.get();
            booking.setUser(user);
            
            // Set appointment date and time
            booking.setAppointmentDate(LocalDate.parse(date));
            booking.setAppointmentTime(LocalTime.parse(time));
            
            // Set location to user's address
            String userAddress = user.getAddress();
            if (userAddress != null && !userAddress.isEmpty()) {
                booking.setAppointmentLocation(userAddress);
            } else {
                // Fallback to city/state if address is not available
                String location = "";
                if (user.getCity() != null && !user.getCity().isEmpty()) {
                    location += user.getCity();
                }
                if (user.getState() != null && !user.getState().isEmpty()) {
                    if (!location.isEmpty()) location += ", ";
                    location += user.getState();
                }
                if (!location.isEmpty()) {
                    booking.setAppointmentLocation(location);
                } else {
                    booking.setAppointmentLocation("Please update your address in profile");
                }
            }
            
            // Create booking - no need to manually call emailService.sendBookingConfirmation() here
            // The BookingService.createBooking method will handle sending the email
            Booking savedBooking = bookingService.createBooking(booking);
            
            model.addAttribute("success", "Appointment booked successfully!");
            return "redirect:/booking/" + savedBooking.getId();
            
        } catch (Exception e) {
            model.addAttribute("error", "Failed to book appointment: " + e.getMessage());
            return "donor_booking";
        }
    }
}