package com.vitalislink.repository;

import com.vitalislink.model.Donor;
import com.vitalislink.util.CSVUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CSVDonorRepository {
    
    private static final String[] HEADERS = {"id", "fullName", "email", "phone", "address", "city", "state", "zipCode", 
                                           "bloodGroup", "lastDonationDate", "weight", "age", "gender", 
                                           "hasMedicalConditions", "medicalConditionsDetails", "isEligible", 
                                           "eligibilityReason", "createdAt", "updatedAt"};
    
    @Value("${app.upload.dir}")
    private String uploadDir;
    
    @Autowired
    private CSVUtils csvUtils;
    
    private String getDonorsFilePath() {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return uploadDir + "/donors.csv";
    }
    
    public List<Donor> findAll() {
        try {
            List<String[]> csvData = csvUtils.readCSV(getDonorsFilePath());
            
            // Skip header row if it exists
            if (!csvData.isEmpty() && csvData.get(0)[0].equals("id")) {
                csvData.remove(0);
            }
            
            return csvData.stream()
                    .map(this::mapToDonor)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read donors from CSV", e);
        }
    }
    
    public Optional<Donor> findById(Long id) {
        return findAll().stream()
                .filter(donor -> donor.getId().equals(id))
                .findFirst();
    }
    
    public Optional<Donor> findByEmail(String email) {
        return findAll().stream()
                .filter(donor -> donor.getEmail() != null && donor.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }
    
    public List<Donor> findByBloodGroup(String bloodGroup) {
        return findAll().stream()
                .filter(donor -> donor.getBloodGroup().equals(bloodGroup))
                .collect(Collectors.toList());
    }
    
    public List<Donor> findAllEligibleDonors() {
        return findAll().stream()
                .filter(donor -> donor.getIsEligible())
                .collect(Collectors.toList());
    }
    
    public List<Donor> findEligibleDonorsByBloodGroup(String bloodGroup) {
        return findAll().stream()
                .filter(donor -> donor.getBloodGroup().equals(bloodGroup) && donor.getIsEligible())
                .collect(Collectors.toList());
    }
    
    public Donor save(Donor donor) {
        List<Donor> donors = findAll();
        
        // Assign ID if not present
        if (donor.getId() == null) {
            long maxId = donors.stream()
                    .mapToLong(Donor::getId)
                    .max()
                    .orElse(0);
            donor.setId(maxId + 1);
            donor.setCreatedAt(LocalDateTime.now());
        } else {
            // Remove existing donor if updating
            donors = donors.stream()
                    .filter(d -> !d.getId().equals(donor.getId()))
                    .collect(Collectors.toList());
        }
        
        donor.setUpdatedAt(LocalDateTime.now());
        donors.add(donor);
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (Donor d : donors) {
                csvData.add(mapToCsvRow(d));
            }
            
            csvUtils.writeCSV(getDonorsFilePath(), csvData);
            return donor;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save donor to CSV", e);
        }
    }
    
    public List<Donor> saveAll(List<Donor> donors) {
        List<Donor> result = new ArrayList<>();
        for (Donor donor : donors) {
            result.add(save(donor));
        }
        return result;
    }
    
    public void deleteById(Long id) {
        List<Donor> donors = findAll().stream()
                .filter(donor -> !donor.getId().equals(id))
                .collect(Collectors.toList());
        
        // Save to CSV
        try {
            List<String[]> csvData = new ArrayList<>();
            csvData.add(HEADERS);
            
            for (Donor d : donors) {
                csvData.add(mapToCsvRow(d));
            }
            
            csvUtils.writeCSV(getDonorsFilePath(), csvData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete donor from CSV", e);
        }
    }
    
    private Donor mapToDonor(String[] row) {
        Donor donor = new Donor();
        
        try {
            donor.setId(Long.parseLong(row[0]));
            donor.setFullName(row[1]);
            donor.setEmail(row[2]);
            donor.setPhone(row[3]);
            donor.setAddress(row[4]);
            donor.setCity(row[5]);
            donor.setState(row[6]);
            donor.setZipCode(row[7]);
            donor.setBloodGroup(row[8]);
            
            // Handle date values
            if (row[9] != null && !row[9].isEmpty()) {
                donor.setLastDonationDate(LocalDate.parse(row[9]));
            }
            
            if (row[10] != null && !row[10].isEmpty()) {
                donor.setWeight(Double.parseDouble(row[10]));
            }
            
            if (row[11] != null && !row[11].isEmpty()) {
                donor.setAge(Integer.parseInt(row[11]));
            }
            
            donor.setGender(row[12]);
            
            if (row[13] != null && !row[13].isEmpty()) {
                donor.setHasMedicalConditions(Boolean.parseBoolean(row[13]));
            }
            
            donor.setMedicalConditionsDetails(row[14]);
            
            if (row[15] != null && !row[15].isEmpty()) {
                donor.setIsEligible(Boolean.parseBoolean(row[15]));
            } else {
                donor.setIsEligible(true);
            }
            
            donor.setEligibilityReason(row[16]);
            
            // Handle timestamps
            if (row[17] != null && !row[17].isEmpty()) {
                donor.setCreatedAt(LocalDateTime.parse(row[17]));
            }
            if (row[18] != null && !row[18].isEmpty()) {
                donor.setUpdatedAt(LocalDateTime.parse(row[18]));
            }
        } catch (Exception e) {
            System.err.println("Error parsing donor data: " + e.getMessage());
        }
        
        return donor;
    }
    
    private String[] mapToCsvRow(Donor donor) {
        return new String[] {
            donor.getId().toString(),
            donor.getFullName(),
            donor.getEmail(),
            donor.getPhone(),
            donor.getAddress(),
            donor.getCity(),
            donor.getState(),
            donor.getZipCode(),
            donor.getBloodGroup(),
            donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : "",
            donor.getWeight() != null ? donor.getWeight().toString() : "",
            donor.getAge() != null ? donor.getAge().toString() : "",
            donor.getGender(),
            donor.getHasMedicalConditions() != null ? donor.getHasMedicalConditions().toString() : "",
            donor.getMedicalConditionsDetails(),
            donor.getIsEligible() != null ? donor.getIsEligible().toString() : "",
            donor.getEligibilityReason(),
            donor.getCreatedAt() != null ? donor.getCreatedAt().toString() : "",
            donor.getUpdatedAt() != null ? donor.getUpdatedAt().toString() : ""
        };
    }
}