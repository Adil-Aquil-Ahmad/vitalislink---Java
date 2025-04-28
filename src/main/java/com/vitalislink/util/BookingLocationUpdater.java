package com.vitalislink.util;

import com.vitalislink.model.Booking;
import com.vitalislink.model.User;
import com.vitalislink.service.BookingService;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BookingLocationUpdater implements CommandLineRunner {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        updateBookingLocations();
    }
    
    private void updateBookingLocations() {
        List<Booking> bookings = bookingService.getAllBookings();
        int updatedCount = 0;
        
        for (Booking booking : bookings) {
            if ("Main Blood Bank Center".equals(booking.getAppointmentLocation()) && booking.getUser() != null) {
                User user = booking.getUser();
                
                if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                    booking.setAppointmentLocation(user.getAddress());
                    bookingService.saveBooking(booking);
                    updatedCount++;
                }
            }
        }
        
        System.out.println("Updated location for " + updatedCount + " bookings");
    }
}