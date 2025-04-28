package com.vitalislink.controller;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.DonorService;
import com.vitalislink.service.EmailService;
import com.vitalislink.service.PDFService;
import com.vitalislink.service.UserService;
import com.vitalislink.util.GeoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
public class DonorController {

    @Autowired
    private DonorService donorService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private PDFService pdfService;
    
    @Autowired
    private GeoUtils geoUtils;
    
    @Autowired
    private EmailService emailService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping("/donors")
    public String listDonors(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Check if admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/dashboard";
        }
        
        // Get all donors
        List<Donor> donors = donorService.getAllDonors();
        
        // Add data to model
        model.addAttribute("donors", donors);
        
        return "donors";
    }
    
    @GetMapping("/donor/register")
    public String showDonorRegistrationForm(Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("donor", new Donor());
        
        return "donor_register";
    }
    
    @PostMapping("/donor/register")
    public String registerDonor(@ModelAttribute Donor donor, 
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        try {
            // Register donor
            Donor registeredDonor = donorService.registerDonor(donor);
            
            // Create booking if donor is eligible
            if (registeredDonor.getIsEligible()) {
                Optional<User> userOpt = userService.getUserByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    Booking booking = new Booking();
                    booking.setUser(user);
                    booking.setDonorName(registeredDonor.getFullName());
                    booking.setDonorEmail(registeredDonor.getEmail());
                    booking.setDonorPhone(registeredDonor.getPhone());
                    booking.setBloodGroup(registeredDonor.getBloodGroup());
                    booking.setAppointmentDate(LocalDate.now().plusDays(3)); // Schedule for 3 days later
                    booking.setAppointmentTime(LocalTime.of(10, 0)); // 10:00 AM
                    booking.setAppointmentLocation("Main Blood Bank Center");
                    booking.setStatus("Pending");
                    
                    bookingService.createBooking(booking);
                    
                    redirectAttributes.addFlashAttribute("success", "Donor registered successfully and booking created");
                }
            } else {
                redirectAttributes.addFlashAttribute("warning", "Donor registered but not eligible for donation: " + 
                    registeredDonor.getEligibilityReason());
            }
            
            return "redirect:/dashboard";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/donor/register";
        }
    }
    
    @GetMapping("/donor/{id}")
    public String viewDonor(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get donor
        Optional<Donor> donorOpt = donorService.getDonorById(id);
        if (!donorOpt.isPresent()) {
            return "redirect:/donors";
        }
        
        Donor donor = donorOpt.get();
        
        // Add data to model
        model.addAttribute("donor", donor);
        
        return "view_donor";
    }
    
    @GetMapping("/donor/edit/{id}")
    public String showEditDonorForm(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Check if admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/dashboard";
        }
        
        // Get donor
        Optional<Donor> donorOpt = donorService.getDonorById(id);
        if (!donorOpt.isPresent()) {
            return "redirect:/donors";
        }
        
        Donor donor = donorOpt.get();
        
        // Add data to model
        model.addAttribute("donor", donor);
        
        return "edit_donor";
    }
    
    @PostMapping("/donor/edit/{id}")
    public String updateDonor(@PathVariable Long id, 
                            @ModelAttribute Donor donor,
                            RedirectAttributes redirectAttributes) {
        
        // Ensure ID is set
        donor.setId(id);
        
        // Update donor
        donorService.updateDonor(donor);
        
        redirectAttributes.addFlashAttribute("success", "Donor updated successfully");
        return "redirect:/donors";
    }
    
    @GetMapping("/donor/delete/{id}")
    public String deleteDonor(@PathVariable Long id, 
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Check if admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/dashboard";
        }
        
        // Delete donor
        donorService.deleteDonor(id);
        
        redirectAttributes.addFlashAttribute("success", "Donor deleted successfully");
        return "redirect:/donors";
    }
    
    @GetMapping("/donors/export/pdf")
    public ResponseEntity<InputStreamResource> exportDonorsToPdf(HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        
        // Check if admin
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return ResponseEntity.status(403).build();
        }
        
        // Get all donors
        List<Donor> donors = donorService.getAllDonors();
        
        // Generate PDF
        ByteArrayInputStream bis = pdfService.generateDonorReport(donors);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=donors.pdf");
        
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
    
    @GetMapping("/donors/by-blood-group")
    public String getDonorsByBloodGroup(@RequestParam String bloodGroup, Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get donors by blood group
        List<Donor> donors = donorService.getDonorsByBloodGroup(bloodGroup);
        
        // Add data to model
        model.addAttribute("donors", donors);
        model.addAttribute("bloodGroup", bloodGroup);
        
        return "donors_by_blood_group";
    }

    @GetMapping("/donors/contact/{id}")
    public String contactDonor(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get donor by ID
        Optional<Donor> donorOpt = donorService.getDonorById(id);
        if (donorOpt.isEmpty()) {
            model.addAttribute("error", "Donor not found");
            return "redirect:/dashboard";
        }
        
        Donor donor = donorOpt.get();
        model.addAttribute("donor", donor);
        
        return "donor_contact";
    }

    @PostMapping("/donors/message/{id}")
    public String sendMessageToDonor(@PathVariable Long id, 
                                    @RequestParam("subject") String subject,
                                    @RequestParam("message") String message,
                                    HttpSession session,
                                    Model model) {
        // Check if user is logged in
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            return "redirect:/login";
        }
        
        // Get donor by ID
        Optional<Donor> donorOpt = donorService.getDonorById(id);
        if (donorOpt.isEmpty()) {
            model.addAttribute("error", "Donor not found");
            return "redirect:/dashboard";
        }
        
        Donor donor = donorOpt.get();
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "redirect:/dashboard";
        }
        
        User user = userOpt.get();
        
        try {
            // Create email details
            String messageBody = "From: " + user.getFullName() + " (" + user.getEmail() + ")\n\n" + message;
            
            // Send email to donor
            emailService.sendSimpleMessage(donor.getEmail(), subject, messageBody);
            
            model.addAttribute("success", "Message sent successfully to " + donor.getFullName());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to send message: " + e.getMessage());
            return "donor_contact";
        }
    }
}