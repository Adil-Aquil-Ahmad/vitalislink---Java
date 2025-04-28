package com.vitalislink.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataMigration implements CommandLineRunner {

    @Autowired
    private ResourceLoader resourceLoader;
    
    @Override
    public void run(String... args) throws Exception {
        migrateBookingsCSV();
    }
    
    private void migrateBookingsCSV() {
        ClassPathResource resource = new ClassPathResource("static/data/bookings.csv");
        
        try {
            // Read the CSV file
            List<String> lines = new ArrayList<>();
            try (InputStream inputStream = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading bookings CSV file: " + e.getMessage());
                return;
            }
            
            if (lines.isEmpty()) {
                return;
            }
            
            // Check if the header contains qrCodePath
            String header = lines.get(0);
            if (header.contains("qrCodePath") && !header.contains("qrCodeContent")) {
                // Update the header
                header = header.replace("qrCodePath", "qrCodeContent");
                lines.set(0, header);
                
                // Process data rows to convert paths to content
                for (int i = 1; i < lines.size(); i++) {
                    String[] fields = lines.get(i).split(",");
                    if (fields.length > 9) {
                        // If there's a QR code path, generate a meaningful content string
                        if (fields.length >= 11 && fields[9] != null && !fields[9].isEmpty()) {
                            // Create content from booking details
                            StringBuilder content = new StringBuilder();
                            content.append("VitalisLink Blood Donation Booking\n\n");
                            
                            if (fields.length > 2 && fields[2] != null && !fields[2].isEmpty())
                                content.append("Name: ").append(fields[2]).append("\n");
                                
                            if (fields.length > 3 && fields[3] != null && !fields[3].isEmpty())
                                content.append("Email: ").append(fields[3]).append("\n");
                                
                            if (fields.length > 4 && fields[4] != null && !fields[4].isEmpty())
                                content.append("Phone: ").append(fields[4]).append("\n");
                                
                            if (fields.length > 5 && fields[5] != null && !fields[5].isEmpty())
                                content.append("Blood Group: ").append(fields[5]).append("\n");
                                
                            if (fields.length > 6 && fields[6] != null && !fields[6].isEmpty())
                                content.append("Date: ").append(fields[6]).append("\n");
                                
                            if (fields.length > 7 && fields[7] != null && !fields[7].isEmpty())
                                content.append("Time: ").append(fields[7]).append("\n");
                                
                            if (fields.length > 8 && fields[8] != null && !fields[8].isEmpty())
                                content.append("Location: ").append(fields[8]).append("\n");
                                
                            if (fields.length > 10 && fields[10] != null && !fields[10].isEmpty())
                                content.append("Status: ").append(fields[10]).append("\n");
                            
                            // Update the field with content
                            fields[9] = content.toString().replace(",", " ");
                        }
                        
                        // Reconstruct the line
                        lines.set(i, String.join(",", fields));
                    }
                }
                
                // Write back to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(resource.getFile()))) {
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }
                }
                
                System.out.println("Successfully migrated bookings.csv from qrCodePath to qrCodeContent");
            }
        } catch (Exception e) {
            System.err.println("Error during data migration: " + e.getMessage());
        }
    }
}