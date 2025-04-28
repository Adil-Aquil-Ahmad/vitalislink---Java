package com.vitalislink.service;

import com.vitalislink.model.Donor;
import com.vitalislink.repository.CSVDonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DonorService {

    @Autowired
    private CSVDonorRepository donorRepository;
    
    public List<Donor> getAllDonors() {
        return donorRepository.findAll();
    }
    
    public Optional<Donor> getDonorById(Long id) {
        return donorRepository.findById(id);
    }
    
    public Optional<Donor> getDonorByEmail(String email) {
        return donorRepository.findByEmail(email);
    }
    
    public List<Donor> getDonorsByBloodGroup(String bloodGroup) {
        return donorRepository.findByBloodGroup(bloodGroup);
    }
    
    public List<Donor> getEligibleDonors() {
        return donorRepository.findAllEligibleDonors();
    }
    
    public List<Donor> getEligibleDonorsByBloodGroup(String bloodGroup) {
        return donorRepository.findEligibleDonorsByBloodGroup(bloodGroup);
    }
    
    /**
     * Register a new donor with eligibility check.
     *
     * @param donor The donor to register
     * @return The registered donor
     */
    public Donor registerDonor(Donor donor) {
        // Validate donor data
        if (donor.getFullName() == null || donor.getFullName().isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }
        
        if (donor.getBloodGroup() == null || donor.getBloodGroup().isEmpty()) {
            throw new IllegalArgumentException("Blood group is required");
        }
        
        // Check eligibility
        boolean isEligible = checkEligibility(donor);
        donor.setIsEligible(isEligible);
        
        if (!isEligible && (donor.getEligibilityReason() == null || donor.getEligibilityReason().isEmpty())) {
            donor.setEligibilityReason("Failed eligibility check");
        }
        
        return donorRepository.save(donor);
    }
    
    /**
     * Update an existing donor.
     *
     * @param donor The donor with updated information
     * @return The updated donor
     */
    public Donor updateDonor(Donor donor) {
        // Validate donor exists
        Optional<Donor> existingDonorOpt = getDonorById(donor.getId());
        if (existingDonorOpt.isEmpty()) {
            throw new RuntimeException("Donor not found: " + donor.getId());
        }
        
        // Re-check eligibility
        boolean isEligible = checkEligibility(donor);
        donor.setIsEligible(isEligible);
        
        if (!isEligible && (donor.getEligibilityReason() == null || donor.getEligibilityReason().isEmpty())) {
            donor.setEligibilityReason("Failed eligibility check");
        }
        
        return donorRepository.save(donor);
    }
    
    /**
     * Check donor eligibility based on age, weight, and last donation date.
     *
     * @param donor The donor to check
     * @return true if the donor is eligible, false otherwise
     */
    private boolean checkEligibility(Donor donor) {
        // Check age
        if (donor.getAge() != null && donor.getAge() < 18) {
            donor.setEligibilityReason("Must be at least 18 years old");
            return false;
        }
        
        // Check weight
        if (donor.getWeight() != null && donor.getWeight() < 50.0) {
            donor.setEligibilityReason("Must weigh at least 50 kg");
            return false;
        }
        
        // Check last donation date (must be at least 3 months ago)
        if (donor.getLastDonationDate() != null) {
            LocalDate minDate = donor.getLastDonationDate().plusMonths(3);
            if (minDate.isAfter(LocalDate.now())) {
                donor.setEligibilityReason("Must wait at least 3 months between donations");
                return false;
            }
        }
        
        // Check medical conditions
        if (donor.getHasMedicalConditions() != null && donor.getHasMedicalConditions()) {
            donor.setEligibilityReason("Has medical conditions that may affect eligibility");
            return false;
        }
        
        return true;
    }
    
    public Donor saveDonor(Donor donor) {
        return donorRepository.save(donor);
    }
    
    public void deleteDonor(Long id) {
        donorRepository.deleteById(id);
    }
}