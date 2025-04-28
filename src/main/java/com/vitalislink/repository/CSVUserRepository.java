package com.vitalislink.repository;

import com.vitalislink.model.User;
import com.vitalislink.util.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CSVUserRepository {

    private static final String[] HEADERS = {"id", "email", "password", "fullName", "phone", "address", 
                                           "city", "state", "zipCode", "bloodGroup", "isAdmin", 
                                           "createdAt", "updatedAt"};
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private CSVUtils csvUtils;
    
    private String getUsersFilePath() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return uploadDir + "/users.csv";
    }
    
    public List<User> findAll() {
        try {
            List<String[]> csvData = csvUtils.readCSV(getUsersFilePath());
            
            // Skip header row if it exists
            if (!csvData.isEmpty() && csvData.get(0)[0].equals("id")) {
                csvData.remove(0);
            }
            
            return csvData.stream()
                    .map(this::mapToUser)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read users from CSV", e);
        }
    }
    
    public Optional<User> findById(Long id) {
        return findAll().stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }
    
    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    
    public boolean existsByEmail(String email) {
        return findAll().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }
    
    public User save(User user) {
        List<User> users = findAll();
        
        // Assign ID if not present
        if (user.getId() == null) {
            long maxId = users.stream()
                    .mapToLong(User::getId)
                    .max()
                    .orElse(0);
            user.setId(maxId + 1);
            user.setCreatedAt(LocalDateTime.now());
        } else {
            // Remove existing user if updating
            users = users.stream()
                    .filter(u -> !u.getId().equals(user.getId()))
                    .collect(Collectors.toList());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        users.add(user);
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (User u : users) {
                csvData.add(mapToCsvRow(u));
            }
            
            csvUtils.writeCSV(getUsersFilePath(), csvData);
            return user;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save user to CSV", e);
        }
    }
    
    public void deleteById(Long id) {
        List<User> users = findAll().stream()
                .filter(user -> !user.getId().equals(id))
                .collect(Collectors.toList());
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (User u : users) {
                csvData.add(mapToCsvRow(u));
            }
            
            csvUtils.writeCSV(getUsersFilePath(), csvData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete user from CSV", e);
        }
    }
    
    private User mapToUser(String[] row) {
        User user = new User();
        
        try {
            // Check if row has enough columns
            if (row.length < HEADERS.length) {
                System.err.println("Row has fewer columns than expected: " + row.length);
                return user;
            }
            
            // Basic fields
            user.setId(Long.parseLong(row[0]));
            user.setEmail(row[1]);
            user.setPassword(row[2]);
            user.setFullName(row[3]);
            user.setPhone(row[4]);
            user.setAddress(row[5]);
            user.setCity(row[6]);
            user.setState(row[7]);
            user.setZipCode(row[8]);
            user.setBloodGroup(row[9]);
            user.setAdmin(Boolean.parseBoolean(row[10]));
            
            // Handle timestamps with better error handling
            if (row[11] != null && !row[11].isEmpty()) {
                try {
                    user.setCreatedAt(LocalDateTime.parse(row[11]));
                } catch (Exception e) {
                    System.err.println("Error parsing createdAt date: " + e.getMessage());
                }
            }
            
            if (row[12] != null && !row[12].isEmpty()) {
                try {
                    user.setUpdatedAt(LocalDateTime.parse(row[12]));
                } catch (Exception e) {
                    System.err.println("Error parsing updatedAt date: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error mapping user data: " + e.getMessage());
        }
        
        return user;
    }
    
    private String[] mapToCsvRow(User user) {
        return new String[] {
            user.getId().toString(),
            user.getEmail(),
            user.getPassword(),
            user.getFullName(),
            user.getPhone(),
            user.getAddress(),
            user.getCity(),
            user.getState(),
            user.getZipCode(),
            user.getBloodGroup(),
            String.valueOf(user.isAdmin()),
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
            user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : ""
        };
    }
}