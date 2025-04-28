package com.vitalislink.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVUtils {
    
    private static final String TYPE = "text/csv";
    private static final String DELIMITER = ",";
    private static final String NEW_LINE = "\n";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * Check if the uploaded file is a CSV file.
     *
     * @param file The uploaded file
     * @return true if the file is a CSV file, false otherwise
     */
    public boolean isCSVFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals(TYPE) || 
               file.getOriginalFilename() != null && 
               file.getOriginalFilename().endsWith(".csv"));
    }
    
    /**
     * Parse a CSV file into a list of string arrays.
     *
     * @param inputStream The input stream of the CSV file
     * @return A list of string arrays, each representing a row in the CSV file
     */
    public List<String[]> readCSV(InputStream inputStream) throws IOException {
        List<String[]> csvData = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER, -1); // -1 preserves empty fields
                csvData.add(values);
            }
        }
        
        return csvData;
    }
    
    /**
     * Read a CSV file into a list of string arrays.
     *
     * @param filePath The path to the CSV file
     * @return A list of string arrays, each representing a row in the CSV file
     */
    public List<String[]> readCSV(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            // Create file with headers if it doesn't exist
            return new ArrayList<>();
        }
        
        List<String[]> csvData = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                csvData.add(nextLine);
            }
        } catch (CsvValidationException e) {
            throw new IOException("Error validating CSV: " + e.getMessage(), e);
        }
        
        return csvData;
    }
    
    /**
     * Write data to a CSV file.
     *
     * @param filePath The path of the CSV file to write
     * @param data A list of string arrays, each representing a row in the CSV file
     */
    public void writeCSV(String filePath, List<String[]> data) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            writer.writeAll(data);
        }
    }
    
    /**
     * Export users to a CSV file.
     *
     * @param users The list of users to export
     * @param filePath The path to save the CSV file
     */
    public void exportUsersToCsv(List<User> users, String filePath) throws IOException {
        List<String[]> csvData = new ArrayList<>();
        
        // Add header row
        csvData.add(new String[] {
            "ID", "Email", "Full Name", "Phone", "Address", "City", "State", "Zip Code", 
            "Blood Group", "Is Admin", "Created At", "Updated At"
        });
        
        // Add data rows
        for (User user : users) {
            csvData.add(new String[] {
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone() != null ? user.getPhone() : "",
                user.getAddress() != null ? user.getAddress() : "",
                user.getCity() != null ? user.getCity() : "",
                user.getState() != null ? user.getState() : "",
                user.getZipCode() != null ? user.getZipCode() : "",
                user.getBloodGroup() != null ? user.getBloodGroup() : "",
                String.valueOf(user.isAdmin()),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : ""
            });
        }
        
        // Write to file
        writeCSV(filePath, csvData);
    }
    
    /**
     * Export donors to a CSV file.
     *
     * @param donors The list of donors to export
     * @param filePath The path to save the CSV file
     */
    public void exportDonorsToCsv(List<Donor> donors, String filePath) throws IOException {
        List<String[]> csvData = new ArrayList<>();
        
        // Add header row
        csvData.add(new String[] {
            "ID", "Full Name", "Email", "Phone", "Address", "City", "State", "Zip Code", 
            "Blood Group", "Last Donation Date", "Weight", "Age", "Gender", 
            "Medical Conditions", "Medical Details", "Eligible", "Eligibility Reason",
            "Created At", "Updated At"
        });
        
        // Add data rows
        for (Donor donor : donors) {
            csvData.add(new String[] {
                donor.getId().toString(),
                donor.getFullName(),
                donor.getEmail() != null ? donor.getEmail() : "",
                donor.getPhone() != null ? donor.getPhone() : "",
                donor.getAddress() != null ? donor.getAddress() : "",
                donor.getCity() != null ? donor.getCity() : "",
                donor.getState() != null ? donor.getState() : "",
                donor.getZipCode() != null ? donor.getZipCode() : "",
                donor.getBloodGroup() != null ? donor.getBloodGroup() : "",
                donor.getLastDonationDate() != null ? donor.getLastDonationDate().format(DATE_FORMATTER) : "",
                donor.getWeight() != null ? donor.getWeight().toString() : "",
                donor.getAge() != null ? donor.getAge().toString() : "",
                donor.getGender() != null ? donor.getGender() : "",
                donor.getHasMedicalConditions() != null ? donor.getHasMedicalConditions().toString() : "",
                donor.getMedicalConditionsDetails() != null ? donor.getMedicalConditionsDetails() : "",
                donor.getIsEligible() != null ? donor.getIsEligible().toString() : "",
                donor.getEligibilityReason() != null ? donor.getEligibilityReason() : "",
                donor.getCreatedAt() != null ? donor.getCreatedAt().toString() : "",
                donor.getUpdatedAt() != null ? donor.getUpdatedAt().toString() : ""
            });
        }
        
        // Write to file
        writeCSV(filePath, csvData);
    }
    
    /**
     * Export bookings to a CSV file.
     *
     * @param bookings The list of bookings to export
     * @param filePath The path to save the CSV file
     */
    public void exportBookingsToCsv(List<Booking> bookings, String filePath) throws IOException {
        List<String[]> csvData = new ArrayList<>();
        
        // Add header row
        csvData.add(new String[] {
            "ID", "User ID", "Donor Name", "Donor Email", "Donor Phone", "Blood Group",
            "Appointment Date", "Appointment Time", "Appointment Location", "Status", "Notes",
            "Created At", "Updated At"
        });
        
        // Add data rows
        for (Booking booking : bookings) {
            csvData.add(new String[] {
                booking.getId().toString(),
                booking.getUser() != null ? booking.getUser().getId().toString() : "",
                booking.getDonorName() != null ? booking.getDonorName() : "",
                booking.getDonorEmail() != null ? booking.getDonorEmail() : "",
                booking.getDonorPhone() != null ? booking.getDonorPhone() : "",
                booking.getBloodGroup() != null ? booking.getBloodGroup() : "",
                booking.getAppointmentDate() != null ? booking.getAppointmentDate().format(DATE_FORMATTER) : "",
                booking.getAppointmentTime() != null ? booking.getAppointmentTime().format(TIME_FORMATTER) : "",
                booking.getAppointmentLocation() != null ? booking.getAppointmentLocation() : "",
                booking.getStatus() != null ? booking.getStatus() : "",
                booking.getNotes() != null ? booking.getNotes() : "",
                booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : "",
                booking.getUpdatedAt() != null ? booking.getUpdatedAt().toString() : ""
            });
        }
        
        // Write to file
        writeCSV(filePath, csvData);
    }
}