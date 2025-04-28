package com.vitalislink.util;

import com.vitalislink.model.Booking;
import com.vitalislink.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("qrfix") // Only run when specific profile is active
public class QRContentFixer implements CommandLineRunner {

    @Autowired
    private BookingService bookingService;

    @Override
    public void run(String... args) throws Exception {
        fixQRContents();
    }
    
    private void fixQRContents() {
        List<Booking> bookings = bookingService.getAllBookings();
        int updatedCount = 0;
        
        for (Booking booking : bookings) {
            // Check if QR content has wrong location or status
            if (booking.getQrCodeContent() != null && 
                (booking.getQrCodeContent().contains("Location: Main Blood Bank Center") ||
                 !booking.getQrCodeContent().contains("Status: " + booking.getStatus()))) {
                
                // Generate corrected QR content
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
                
                // Use actual appointment location
                content.append("Location: ").append(booking.getAppointmentLocation()).append("\n");
                
                // Include current status
                content.append("Status: ").append(booking.getStatus()).append("\n");
                
                if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
                    content.append("Notes: ").append(booking.getNotes()).append("\n");
                }
                
                booking.setQrCodeContent(content.toString());
                bookingService.saveBooking(booking);
                updatedCount++;
            }
        }
        
        System.out.println("Updated QR content for " + updatedCount + " bookings");
    }
}