package com.vitalislink.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for handling CSV file uploads and validation.
 */
@Component
public class CSVHelper {
    
    private static final String TYPE = "text/csv";
    private static final String DELIMITER = ",";
    
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
    public List<String[]> parseCsv(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String[]> rows = new ArrayList<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Split by delimiter
                String[] values = line.split(DELIMITER);
                
                // Trim each value
                for (int i = 0; i < values.length; i++) {
                    values[i] = values[i].trim();
                }
                
                rows.add(values);
            }
            
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }
    
    /**
     * Validates that the CSV file has the expected header.
     *
     * @param headerRow The first row of the CSV file
     * @param expectedHeaders The expected headers
     * @return true if the headers match, false otherwise
     */
    public boolean validateHeader(String[] headerRow, String[] expectedHeaders) {
        if (headerRow.length != expectedHeaders.length) {
            return false;
        }
        
        for (int i = 0; i < headerRow.length; i++) {
            if (!headerRow[i].equalsIgnoreCase(expectedHeaders[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets the index of a column by its name in the header row.
     *
     * @param headerRow The header row
     * @param columnName The name of the column
     * @return The index of the column, or -1 if not found
     */
    public int getColumnIndex(String[] headerRow, String columnName) {
        for (int i = 0; i < headerRow.length; i++) {
            if (headerRow[i].equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        
        return -1;
    }
}