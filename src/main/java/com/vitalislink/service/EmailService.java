package com.vitalislink.service;

import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import com.vitalislink.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;
    
    @Autowired
    private PDFService pdfService;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.isEmpty() ? "noreply@vitalislink.com" : fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            logger.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send simple email to: {}", to, e);
        }
    }

    public void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail.isEmpty() ? "noreply@vitalislink.com" : fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            
            File file = new File(pathToAttachment);
            if (file.exists() && file.length() > 0) {
                helper.addAttachment(file.getName(), file);
                logger.info("Added attachment: {} ({} bytes)", file.getName(), file.length());
            } else {
                logger.warn("Attachment file not found or empty: {}", pathToAttachment);
            }
            
            emailSender.send(message);
            logger.info("Email with attachment sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email with attachment to: {}", to, e);
            // Fall back to simple email
            sendSimpleMessage(to, subject, text + "\n\n(Attachment could not be included due to an error)");
        }
    }
    
    public void sendWelcomeEmail(User user) {
        String subject = "Welcome to VitalisLink!";
        String text = "Dear " + user.getFullName() + ",\n\n" +
                "Welcome to VitalisLink! We're thrilled to have you join our community.\n\n" +
                "VitalisLink is a platform that connects blood donors with those in need.\n\n" +
                "Thank you for registering. Together, we can save lives.\n\n" +
                "Best regards,\n" +
                "The VitalisLink Team";
        
        sendSimpleMessage(user.getEmail(), subject, text);
    }
    
    public void sendBookingConfirmation(Booking booking) {
        if (booking == null) {
            logger.error("Cannot send confirmation for null booking");
            return;
        }
        
        if (booking.getDonorEmail() == null || booking.getDonorEmail().isEmpty()) {
            logger.error("Cannot send confirmation for booking ID: {} - no email address", 
                         booking.getId());
            return;
        }
        
        String subject = "Blood Donation Appointment Confirmation";
        String text = "Dear " + booking.getDonorName() + ",\n\n" +
                "Your blood donation appointment has been confirmed.\n\n" +
                "Details:\n" +
                "Date: " + booking.getAppointmentDate() + "\n" +
                "Time: " + booking.getAppointmentTime() + "\n" +
                "Location: " + booking.getAppointmentLocation() + "\n" +
                "Status: " + booking.getStatus() + "\n\n" +
                "Please remember to bring a valid ID and get plenty of rest before your donation.\n\n" +
                "We've attached a PDF confirmation with a QR code that you can show when you arrive.\n\n" +
                "Thank you for your generosity!\n\n" +
                "Best regards,\n" +
                "The VitalisLink Team";
        
        try {
            logger.info("Generating PDF for booking ID: {}", booking.getId());
            
            // Generate a temporary PDF with booking details and QR code
            String pdfPath = pdfService.generateBookingConfirmation(booking);
            
            logger.info("Generated PDF at: {}", pdfPath);
            
            // Verify the file exists before sending
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists() || pdfFile.length() == 0) {
                throw new RuntimeException("Generated PDF file does not exist or is empty: " + pdfPath);
            }
            
            logger.info("Sending email with PDF attachment to: {}", booking.getDonorEmail());
            
            // Create a MimeMessage with attachment
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail.isEmpty() ? "noreply@vitalislink.com" : fromEmail);
            helper.setTo(booking.getDonorEmail());
            helper.setSubject(subject);
            helper.setText(text);
            
            // Add the PDF attachment
            helper.addAttachment("Booking_Confirmation_" + booking.getId() + ".pdf", pdfFile);
            
            // Send the email
            emailSender.send(message);
            logger.info("Email with PDF attachment sent successfully to: {}", booking.getDonorEmail());
            
            // Schedule deletion of the temporary file
            pdfFile.deleteOnExit();
            
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation email with PDF: {}", e.getMessage(), e);
            
            // Fall back to simple email if PDF generation fails
            sendSimpleMessage(booking.getDonorEmail(), subject, text);
        }
    }

    /**
     * Send confirmation email when donation is completed and moved to donors
     */
    public void sendDonationCompletedConfirmation(Booking booking, Donor donor) {
        if (booking == null || donor == null) {
            logger.error("Cannot send completion confirmation for null booking/donor");
            return;
        }
        
        String subject = "Blood Donation Completed - Thank You!";
        String text = "Dear " + donor.getFullName() + ",\n\n" +
                "Thank you for your generous blood donation! Your donation has been completed successfully.\n\n" +
                "Donation Details:\n" +
                "Date: " + booking.getAppointmentDate() + "\n" +
                "Time: " + booking.getAppointmentTime() + "\n" +
                "Location: " + booking.getAppointmentLocation() + "\n\n" +
                "Your donation can help save up to three lives. You are now registered in our donor database, " +
                "and we may contact you in the future for emergency donations.\n\n" +
                "You'll be eligible to donate again after 56 days (8 weeks).\n\n" +
                "Thank you for your life-saving gift!\n\n" +
                "Best regards,\n" +
                "The VitalisLink Team";
        
        try {
            sendSimpleMessage(donor.getEmail(), subject, text);
            logger.info("Donation completion email sent to: {}", donor.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send donation completion email: {}", e.getMessage(), e);
        }
    }
}