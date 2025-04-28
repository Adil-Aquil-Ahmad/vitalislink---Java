package com.vitalislink.repository;

import com.vitalislink.model.Booking;
import com.vitalislink.model.User;
import com.vitalislink.util.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CSVBookingRepository {
    
    private static final String[] HEADERS = {"id", "userId", "donorName", "donorEmail", "donorPhone", 
                                           "bloodGroup", "appointmentDate", "appointmentTime", 
                                           "appointmentLocation", "qrCodeContent", "status", "notes", 
                                           "createdAt", "updatedAt"};
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private CSVUtils csvUtils;
    
    @Autowired
    private CSVUserRepository userRepository;
    
    private String getBookingsFilePath() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return uploadDir + "/bookings.csv";
    }
    
    public List<Booking> findAll() {
        try {
            List<String[]> csvData = csvUtils.readCSV(getBookingsFilePath());
            
            // Skip header row if it exists
            if (!csvData.isEmpty() && csvData.get(0)[0].equals("id")) {
                csvData.remove(0);
            }
            
            return csvData.stream()
                    .map(this::mapToBooking)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bookings from CSV", e);
        }
    }
    
    public Optional<Booking> findById(Long id) {
        return findAll().stream()
                .filter(booking -> booking.getId().equals(id))
                .findFirst();
    }
    
    public List<Booking> findByUser(User user) {
        return findAll().stream()
                .filter(booking -> booking.getUser() != null && 
                        booking.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());
    }
    
    public List<Booking> findByStatus(String status) {
        return findAll().stream()
                .filter(booking -> booking.getStatus().equals(status))
                .collect(Collectors.toList());
    }
    
    public Booking save(Booking booking) {
        List<Booking> bookings = findAll();
        
        // Assign ID if not present
        if (booking.getId() == null) {
            long maxId = bookings.stream()
                    .mapToLong(Booking::getId)
                    .max()
                    .orElse(0);
            booking.setId(maxId + 1);
            booking.setCreatedAt(LocalDateTime.now());
        } else {
            // Remove existing booking if updating
            bookings = bookings.stream()
                    .filter(b -> !b.getId().equals(booking.getId()))
                    .collect(Collectors.toList());
        }
        
        booking.setUpdatedAt(LocalDateTime.now());
        bookings.add(booking);
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (Booking b : bookings) {
                csvData.add(mapToCsvRow(b));
            }
            
            csvUtils.writeCSV(getBookingsFilePath(), csvData);
            return booking;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save booking to CSV", e);
        }
    }
    
    public void deleteById(Long id) {
        List<Booking> bookings = findAll().stream()
                .filter(booking -> !booking.getId().equals(id))
                .collect(Collectors.toList());
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (Booking b : bookings) {
                csvData.add(mapToCsvRow(b));
            }
            
            csvUtils.writeCSV(getBookingsFilePath(), csvData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete booking from CSV", e);
        }
    }
    
    private Booking mapToBooking(String[] row) {
        Booking booking = new Booking();
        
        try {
            // Check if row has enough elements
            if (row.length < HEADERS.length) {
                System.err.println("Row has fewer columns than expected: " + row.length + " vs " + HEADERS.length);
                return booking;
            }
            
            booking.setId(Long.parseLong(row[0]));
            
            // Handle User reference
            if (row[1] != null && !row[1].isEmpty()) {
                try {
                    Long userId = Long.parseLong(row[1]);
                    Optional<User> userOpt = userRepository.findById(userId);
                    userOpt.ifPresent(booking::setUser);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing user ID: " + row[1]);
                }
            }
            
            booking.setDonorName(row[2]);
            booking.setDonorEmail(row[3]);
            booking.setDonorPhone(row[4]);
            booking.setBloodGroup(row[5]);
            
            // Handle date and time with better error handling
            if (row[6] != null && !row[6].isEmpty()) {
                try {
                    booking.setAppointmentDate(LocalDate.parse(row[6]));
                } catch (Exception e) {
                    System.err.println("Error parsing appointment date: " + row[6] + " - " + e.getMessage());
                }
            }
            
            if (row[7] != null && !row[7].isEmpty()) {
                try {
                    booking.setAppointmentTime(LocalTime.parse(row[7]));
                } catch (Exception e) {
                    System.err.println("Error parsing appointment time: " + row[7] + " - " + e.getMessage());
                }
            }
            
            booking.setAppointmentLocation(row[8]);
            booking.setQrCodeContent(row[9]); // Store the QR code content instead of path
            booking.setStatus(row[10]);
            booking.setNotes(row[11]);
            
            // Handle timestamps with better error handling
            if (row[12] != null && !row[12].isEmpty()) {
                try {
                    booking.setCreatedAt(LocalDateTime.parse(row[12]));
                } catch (Exception e) {
                    System.err.println("Error parsing createdAt date: " + row[12] + " - " + e.getMessage());
                }
            }
            
            if (row[13] != null && !row[13].isEmpty()) {
                try {
                    booking.setUpdatedAt(LocalDateTime.parse(row[13]));
                } catch (Exception e) {
                    System.err.println("Error parsing updatedAt date: " + row[13] + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing booking data: " + e.getMessage());
        }
        
        return booking;
    }
    
    // In the mapToCsvRow method, properly escape the QR code content
    private String[] mapToCsvRow(Booking booking) {
        return new String[] {
            booking.getId().toString(),
            booking.getUser() != null ? booking.getUser().getId().toString() : "",
            booking.getDonorName(),
            booking.getDonorEmail(),
            booking.getDonorPhone(),
            booking.getBloodGroup(),
            booking.getAppointmentDate() != null ? booking.getAppointmentDate().toString() : "",
            booking.getAppointmentTime() != null ? booking.getAppointmentTime().toString() : "",
            booking.getAppointmentLocation(),
            booking.getQrCodeContent(), // This will be automatically quoted by CSVWriter
            booking.getStatus(),
            booking.getNotes(),
            booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : "",
            booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : ""
        };
    }

    // Add the saveAll method and fix the saveToCSV issue

    public List<Booking> saveAll(List<Booking> bookings) {
        // Get current bookings to determine next ID
        List<Booking> existingBookings = findAll();
        
        // Find highest ID
        long maxId = 0;
        for (Booking booking : existingBookings) {
            if (booking.getId() > maxId) {
                maxId = booking.getId();
            }
        }
        
        // Assign IDs to new bookings if needed
        for (Booking booking : bookings) {
            if (booking.getId() == null || booking.getId() <= 0) {
                booking.setId(++maxId);
            }
            
            // Update timestamps if needed
            if (booking.getCreatedAt() == null) {
                booking.setCreatedAt(LocalDateTime.now());
            }
            booking.setUpdatedAt(LocalDateTime.now());
        }
        
        // Create a combined list (replace existing bookings with updated ones)
        List<Booking> combinedList = new ArrayList<>();
        
        // Add all bookings from input list
        combinedList.addAll(bookings);
        
        // Add existing bookings that are not in the input list
        for (Booking existingBooking : existingBookings) {
            boolean exists = false;
            for (Booking newBooking : bookings) {
                if (existingBooking.getId().equals(newBooking.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                combinedList.add(existingBooking);
            }
        }
        
        // Save combined list to CSV - Fixed this line
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (Booking b : combinedList) {
                csvData.add(mapToCsvRow(b));
            }
            
            csvUtils.writeCSV(getBookingsFilePath(), csvData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bookings to CSV", e);
        }
        
        return bookings;
    }
}