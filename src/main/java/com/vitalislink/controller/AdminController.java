package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.DonorService;
import com.vitalislink.service.UserService;
import com.vitalislink.util.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private DonorService donorService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private CSVUtils csvUtils;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get donors (completed donations)
        List<Donor> donors = donorService.getAllDonors();
        model.addAttribute("donors", donors);
        
        // Get pending and confirmed bookings
        List<Booking> bookings = bookingService.getPendingAndConfirmedBookings();
        model.addAttribute("bookings", bookings);
        
        return "admin_dashboard";
    }
    
    @PostMapping("/admin/booking/{id}/confirm")
    public String confirmBooking(@PathVariable Long id, HttpSession session) {
        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            System.out.println("Admin confirming booking ID: " + id);
            bookingService.updateBookingStatus(id, "Confirmed");
            return "redirect:/admin/dashboard?success=Booking confirmed successfully";
        } catch (Exception e) {
            System.err.println("Error confirming booking: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/dashboard?error=Failed to confirm booking: " + e.getMessage();
        }
    }
    
    @PostMapping("/admin/booking/{id}/complete")
    public String completeBooking(@PathVariable Long id, HttpSession session) {
        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            bookingService.updateBookingStatus(id, "Completed");
            return "redirect:/admin/dashboard?success=Booking marked as completed";
        } catch (Exception e) {
            return "redirect:/admin/dashboard?error=Failed to complete booking: " + e.getMessage();
        }
    }
    
    @PostMapping("/admin/booking/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, HttpSession session) {
        // Check if user is admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            bookingService.updateBookingStatus(id, "Cancelled");
            return "redirect:/admin/dashboard?success=Booking cancelled successfully";
        } catch (Exception e) {
            return "redirect:/admin/dashboard?error=Failed to cancel booking: " + e.getMessage();
        }
    }
    
    @GetMapping("/admin/users")
    public String listUsers(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get all users
        List<User> users = userService.getAllUsers();
        
        model.addAttribute("users", users);
        return "admin_users";
    }
    
    @GetMapping("/admin/users/export")
    public String exportUsers(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            List<User> users = userService.getAllUsers();
            String fileName = "users_export_" + LocalDate.now() + ".csv";
            String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
            
            csvUtils.exportUsersToCsv(users, filePath);
            
            redirectAttributes.addFlashAttribute("success", "Users exported successfully to " + filePath);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to export users: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/admin/bookings")
    public String listBookings(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get all bookings
        List<Booking> bookings = bookingService.getAllBookings();
        
        model.addAttribute("bookings", bookings);
        return "admin_bookings";
    }
    
    @GetMapping("/admin/bookings/by-date")
    public String getBookingsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model,
            HttpSession session) {
        
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get bookings for the selected date
        List<Booking> bookings = bookingService.getBookingsByDate(date);
        
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedDate", date);
        
        return "admin_bookings_by_date";
    }
    
    @GetMapping("/admin/bookings/by-status")
    public String getBookingsByStatus(
            @RequestParam String status,
            Model model,
            HttpSession session) {
        
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get bookings for the selected status
        List<Booking> bookings = bookingService.getBookingsByStatus(status);
        
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedStatus", status);
        
        return "admin_bookings_by_status";
    }
    
    @GetMapping("/admin/bookings/export")
    public String exportBookings(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            List<Booking> bookings = bookingService.getAllBookings();
            String fileName = "bookings_export_" + LocalDate.now() + ".csv";
            String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
            
            csvUtils.exportBookingsToCsv(bookings, filePath);
            
            redirectAttributes.addFlashAttribute("success", "Bookings exported successfully to " + filePath);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to export bookings: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    @PostMapping("/admin/booking/update-status")
    public String updateBookingStatus(
            @RequestParam Long bookingId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        
        try {
            Booking updatedBooking = bookingService.updateBookingStatus(bookingId, status);
            redirectAttributes.addFlashAttribute("success", 
                    "Booking #" + updatedBooking.getId() + " status updated to " + status);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update booking: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings";
    }
    
    @GetMapping("/admin/donors")
    public String listDonors(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get all donors
        List<Donor> donors = donorService.getAllDonors();
        
        model.addAttribute("donors", donors);
        return "admin_donors";
    }
    
    @GetMapping("/admin/donors/eligible")
    public String listEligibleDonors(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get eligible donors
        List<Donor> donors = donorService.getEligibleDonors();
        
        model.addAttribute("donors", donors);
        model.addAttribute("eligibleOnly", true);
        
        return "admin_donors";
    }
    
    @GetMapping("/admin/donors/export")
    public String exportDonors(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        try {
            List<Donor> donors = donorService.getAllDonors();
            String fileName = "donors_export_" + LocalDate.now() + ".csv";
            String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
            
            csvUtils.exportDonorsToCsv(donors, filePath);
            
            redirectAttributes.addFlashAttribute("success", "Donors exported successfully to " + filePath);
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to export donors: " + e.getMessage());
        }
        
        return "redirect:/admin/donors";
    }
    
    @GetMapping("/admin/donor/{id}")
    public String viewDonor(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        String email = (String) session.getAttribute("userEmail");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (email == null || isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        // Get donor
        Optional<Donor> donorOpt = donorService.getDonorById(id);
        if (!donorOpt.isPresent()) {
            return "redirect:/admin/donors";
        }
        
        model.addAttribute("donor", donorOpt.get());
        
        return "admin_view_donor";
    }
}