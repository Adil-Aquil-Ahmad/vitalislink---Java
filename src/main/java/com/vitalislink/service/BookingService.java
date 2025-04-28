package com.vitalislink.service;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.model.User;
import com.vitalislink.repository.CSVBookingRepository;
import com.vitalislink.repository.CSVDonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private CSVBookingRepository bookingRepository;
    
    @Autowired
    private CSVDonorRepository donorRepository;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private EmailService emailService;
    
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    /**
     * Retrieve a booking and generate its QR code base64 data on demand
     */
    public Optional<Booking> getBookingById(Long id) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            
            // Generate the base64 QR code on demand
            if (booking.getQrCodeContent() != null && !booking.getQrCodeContent().isEmpty()) {
                try {
                    String base64QR = qrCodeService.generateQRCodeBase64(booking.getQrCodeContent());
                    booking.setQrCodeBase64(base64QR);
                } catch (Exception e) {
                    System.err.println("Error generating QR code: " + e.getMessage());
                }
            }
        }
        
        return bookingOpt;
    }
    
    public List<Booking> getBookingsByUser(User user) {
        return bookingRepository.findByUser(user);
    }
    
    public List<Booking> getBookingsByStatus(String status) {
        return bookingRepository.findByStatus(status);
    }
    
    /**
     * Get bookings by date.
     *
     * @param date The date to filter bookings by
     * @return A list of bookings on the specified date
     */
    public List<Booking> getBookingsByDate(LocalDate date) {
        return bookingRepository.findAll().stream()
                .filter(booking -> booking.getAppointmentDate() != null && 
                        booking.getAppointmentDate().equals(date))
                .collect(Collectors.toList());
    }
    
    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }
    
    /**
     * Create a new booking, generate QR code, and send confirmation email
     */
    public Booking createBooking(Booking booking) {
        // Validate booking data
        if (booking.getUser() == null) {
            throw new IllegalArgumentException("Booking must have a user");
        }
        
        if (booking.getDonorName() == null || booking.getDonorName().isEmpty()) {
            throw new IllegalArgumentException("Donor name is required");
        }
        
        // Set default status if not provided
        if (booking.getStatus() == null || booking.getStatus().isEmpty()) {
            booking.setStatus("Pending");
        }
        
        // Generate QR code content
        String qrContent = createQRContent(booking);
        booking.setQrCodeContent(qrContent);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        
        // Save to repository
        Booking savedBooking = bookingRepository.save(booking);
        System.out.println("Successfully created booking with ID: " + savedBooking.getId());
        
        // Send confirmation email (with try-catch to prevent failures from blocking booking creation)
        try {
            System.out.println("Attempting to send confirmation email for booking: " + savedBooking.getId());
            emailService.sendBookingConfirmation(savedBooking);
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email, but booking was created: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedBooking;
    }
    
    /**
     * Create QR content from booking information
     */
    private String createQRContent(Booking booking) {
        StringBuilder content = new StringBuilder();
        content.append("VitalisLink Blood Donation Booking\n\n");
        content.append("Name: ").append(booking.getDonorName()).append("\n");
        content.append("Email: ").append(booking.getDonorEmail()).append("\n");
        content.append("Phone: ").append(booking.getDonorPhone()).append("\n");
        content.append("Blood Group: ").append(booking.getBloodGroup()).append("\n");
        
        if (booking.getAppointmentDate() != null) {
            content.append("Date: ").append(booking.getAppointmentDate()).append("\n");
        }
        
        if (booking.getAppointmentTime() != null) {
            content.append("Time: ").append(booking.getAppointmentTime()).append("\n");
        }
        
        // Use the actual appointment location from the booking
        content.append("Location: ").append(booking.getAppointmentLocation()).append("\n");
        
        // Include the current status
        content.append("Status: ").append(booking.getStatus()).append("\n");
        
        if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
            content.append("Notes: ").append(booking.getNotes()).append("\n");
        }
        
        return content.toString();
    }
    
    /**
     * Update booking status and move to donors if completed
     */
    public Booking updateBookingStatus(Long id, String status) {
        Optional<Booking> bookingOpt = getBookingById(id);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("Booking not found: " + id);
        }
        
        Booking booking = bookingOpt.get();
        
        // If status is changing to Completed, move to donors
        if ("Completed".equals(status) && !"Completed".equals(booking.getStatus())) {
            // Create and save donor record
            Donor donor = new Donor();
            donor.setFullName(booking.getDonorName());
            donor.setEmail(booking.getDonorEmail());
            donor.setPhone(booking.getDonorPhone());
            donor.setBloodGroup(booking.getBloodGroup());
            donor.setAddress(booking.getAppointmentLocation());
            
            // Extract city/state if possible
            String[] addressParts = booking.getAppointmentLocation().split(",\\s*");
            if (addressParts.length >= 3) {
                donor.setCity(addressParts[addressParts.length - 3].trim());
                donor.setState(addressParts[addressParts.length - 2].trim());
                donor.setZipCode(addressParts[addressParts.length - 1].trim());
            }
            
            donor.setLastDonationDate(booking.getAppointmentDate());
            donor.setIsEligible(true);
            donor.setCreatedAt(booking.getCreatedAt());
            donor.setUpdatedAt(LocalDateTime.now());
            
            // Save to donor repository
            donorRepository.save(donor);
            
            // Delete from bookings
            bookingRepository.deleteById(booking.getId());
            
            // Update booking for email purposes, but it won't be saved to bookings.csv
            booking.setStatus(status);
            booking.setUpdatedAt(LocalDateTime.now());
            
            // Send email confirmation
            try {
                System.out.println("Sending completion confirmation for booking ID: " + id);
                emailService.sendDonationCompletedConfirmation(booking, donor);
            } catch (Exception e) {
                System.err.println("Failed to send completion email: " + e.getMessage());
                e.printStackTrace();
            }
            
            return booking;
        } 
        // For other status changes
        else {
            booking.setStatus(status);
            booking.setUpdatedAt(LocalDateTime.now());
            
            // Regenerate QR code content with updated status
            String qrContent = createQRContent(booking);
            booking.setQrCodeContent(qrContent);
            
            // Save to repository
            Booking updatedBooking = bookingRepository.save(booking);
            
            // Send updated confirmation email
            try {
                System.out.println("Sending updated booking confirmation for ID: " + id + " with status: " + status);
                emailService.sendBookingConfirmation(updatedBooking);
            } catch (Exception e) {
                System.err.println("Failed to send updated confirmation email: " + e.getMessage());
                e.printStackTrace();
            }
            
            return updatedBooking;
        }
    }
    
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
    
    /**
     * Get only pending and confirmed bookings for admin dashboard
     */
    public List<Booking> getPendingAndConfirmedBookings() {
        return bookingRepository.findAll().stream()
                .filter(booking -> "Pending".equals(booking.getStatus()) || 
                                  "Confirmed".equals(booking.getStatus()))
                .collect(Collectors.toList());
    }
}