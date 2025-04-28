package com.vitalislink.util;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.repository.CSVBookingRepository;
import com.vitalislink.repository.CSVDonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataMigrationUtil implements CommandLineRunner {

    @Autowired
    private CSVBookingRepository bookingRepository;
    
    @Autowired
    private CSVDonorRepository donorRepository;

    @Override
    public void run(String... args) throws Exception {
        migrateCompletedBookingsToDonors();
    }
    
    private void migrateCompletedBookingsToDonors() {
        // Get all bookings
        List<Booking> allBookings = bookingRepository.findAll();
        List<Booking> completedBookings = new ArrayList<>();
        List<Booking> pendingConfirmedBookings = new ArrayList<>();
        
        // Separate completed bookings from pending/confirmed ones
        for (Booking booking : allBookings) {
            if ("Completed".equals(booking.getStatus())) {
                completedBookings.add(booking);
            } else if ("Pending".equals(booking.getStatus()) || "Confirmed".equals(booking.getStatus())) {
                pendingConfirmedBookings.add(booking);
            }
        }
        
        // Move completed bookings to donors
        for (Booking completedBooking : completedBookings) {
            // Create donor from completed booking
            Donor donor = new Donor();
            donor.setFullName(completedBooking.getDonorName());
            donor.setEmail(completedBooking.getDonorEmail());
            donor.setPhone(completedBooking.getDonorPhone());
            donor.setBloodGroup(completedBooking.getBloodGroup());
            
            // Extract address components if possible
            String[] addressParts = completedBooking.getAppointmentLocation().split(",\\s*");
            if (addressParts.length >= 3) {
                donor.setAddress(completedBooking.getAppointmentLocation());
                donor.setCity(addressParts[addressParts.length - 3].trim());
                donor.setState(addressParts[addressParts.length - 2].trim());
                donor.setZipCode(addressParts[addressParts.length - 1].trim());
            } else {
                donor.setAddress(completedBooking.getAppointmentLocation());
            }
            
            // Set donation date as the appointment date
            donor.setLastDonationDate(completedBooking.getAppointmentDate());
            
            // Set eligibility
            donor.setIsEligible(true);
            
            // Set timestamps
            donor.setCreatedAt(completedBooking.getCreatedAt() != null ? 
                    completedBooking.getCreatedAt() : LocalDateTime.now());
            donor.setUpdatedAt(LocalDateTime.now());
            
            // Save to donors repository
            donorRepository.save(donor);
        }
        
        System.out.println("Migrated " + completedBookings.size() + " completed bookings to donors");
        
        // Update bookings repository to only contain pending/confirmed
        for (Booking booking : pendingConfirmedBookings) {
            bookingRepository.save(booking);
        }

        System.out.println("Kept " + pendingConfirmedBookings.size() + " pending/confirmed bookings");
        
        System.out.println("Data migration completed successfully");
    }
}