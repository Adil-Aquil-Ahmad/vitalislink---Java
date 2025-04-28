package com.vitalislink.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.vitalislink.model.Booking;
import com.vitalislink.model.Donor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
public class PDFService {

    @Value("${java.io.tmpdir}")
    private String tempDir;
    
    @Autowired
    private QRCodeService qrCodeService;

    public ByteArrayInputStream generateDonorReport(List<Donor> donors) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Donor Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);
            
            // Add generated date
            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Paragraph dateText = new Paragraph("Generated: " + LocalDateTime.now().format(formatter), dateFont);
            dateText.setAlignment(Element.ALIGN_RIGHT);
            document.add(dateText);
            document.add(Chunk.NEWLINE);
            
            // Create table
            PdfPTable table = new PdfPTable(5); // 5 columns
            table.setWidthPercentage(100);
            
            // Set table header
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            String[] headers = {"Name", "Blood Group", "Age", "Last Donation Date", "Eligible"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }
            
            // Set table data
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (Donor donor : donors) {
                table.addCell(new Phrase(donor.getFullName(), dataFont));
                table.addCell(new Phrase(donor.getBloodGroup(), dataFont));
                table.addCell(new Phrase(donor.getAge() != null ? donor.getAge().toString() : "N/A", dataFont));
                table.addCell(new Phrase(donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : "N/A", dataFont));
                table.addCell(new Phrase(donor.getIsEligible() ? "Yes" : "No", dataFont));
            }
            
            document.add(table);
            document.close();
            
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        }
        
        return new ByteArrayInputStream(out.toByteArray());
    }
    
    /**
     * Generate a temporary PDF file with booking details and QR code
     * 
     * @param booking The booking data to include in the PDF
     * @return Path to the generated PDF file
     */
    public String generateBookingConfirmation(Booking booking) {
        Document document = new Document(PageSize.A4);
        String fileName = "booking_" + booking.getId() + "_" + System.currentTimeMillis() + ".pdf";
        
        // Print temp directory for debugging
        System.out.println("Temp directory: " + tempDir);
        
        // If tempDir is null or empty, use system temp directory
        if (tempDir == null || tempDir.trim().isEmpty()) {
            tempDir = System.getProperty("java.io.tmpdir");
            System.out.println("Using system temp directory: " + tempDir);
        }
        
        // Ensure temp directory ends with separator
        if (!tempDir.endsWith(File.separator)) {
            tempDir = tempDir + File.separator;
        }
        
        String filePath = tempDir + fileName;
        System.out.println("Will create PDF at: " + filePath);
        
        // Ensure directory exists
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            boolean created = tempDirFile.mkdirs();
            System.out.println("Created temp directory: " + created);
        }
        
        // Check directory permissions
        System.out.println("Temp directory can write: " + tempDirFile.canWrite());
        System.out.println("Temp directory can read: " + tempDirFile.canRead());
        
        try {
            File file = new File(filePath);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            
            // Add logo - using multiple fallback approaches
            try {
                // Try these paths to find the logo
                String[] possiblePaths = {
                    "src/main/resources/static/Images/VITALISLINK LOGO LIGHT2.png",
                    "static/Images/VITALISLINK LOGO LIGHT2.png",
                    "Images/VITALISLINK LOGO LIGHT2.png",
                    "VITALISLINK LOGO LIGHT2.png"
                };
                
                Image logo = null;
                
                // Try direct file paths first
                for (String path : possiblePaths) {
                    try {
                        File logoFile = new File(path);
                        if (logoFile.exists() && logoFile.canRead()) {
                            System.out.println("Found logo at: " + path);
                            logo = Image.getInstance(logoFile.getAbsolutePath());
                            break;
                        }
                    } catch (Exception e) {
                        // Continue to next path
                    }
                }
                
                // If still not found, try classpath
                if (logo == null) {
                    try {
                        ClassPathResource resource = new ClassPathResource("static/Images/VITALISLINK LOGO LIGHT2.png");
                        if (resource.exists()) {
                            logo = Image.getInstance(resource.getInputStream().readAllBytes());
                            System.out.println("Found logo in classpath");
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load logo from classpath: " + e.getMessage());
                    }
                }
                
                // If logo was loaded, add it to document
                if (logo != null) {
                    logo.scaleToFit(200, 100);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                } else {
                    // Fallback to text
                    Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
                    Paragraph logoText = new Paragraph("VitalisLink", logoFont);
                    logoText.setAlignment(Element.ALIGN_CENTER);
                    document.add(logoText);
                }
            } catch (Exception e) {
                System.err.println("Logo loading error: " + e.getMessage());
                // Add text header as fallback
                Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
                Paragraph logoText = new Paragraph("VitalisLink", logoFont);
                logoText.setAlignment(Element.ALIGN_CENTER);
                document.add(logoText);
            }
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Blood Donation Booking Confirmation", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);
            
            // Add booking ID with background
            PdfPTable idTable = new PdfPTable(1);
            idTable.setWidthPercentage(100);
            Font idFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            PdfPCell idCell = new PdfPCell(new Phrase("Booking ID: " + booking.getId(), idFont));
            idCell.setBackgroundColor(new BaseColor(255, 87, 87));
            idCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            idCell.setPadding(10);
            idTable.addCell(idCell);
            document.add(idTable);
            document.add(Chunk.NEWLINE);
            
            // Add QR code (generate dynamically if needed)
            if (booking.getQrCodeContent() != null && !booking.getQrCodeContent().isEmpty()) {
                try {
                    // Generate QR Code
                    String base64QR = qrCodeService.generateQRCodeBase64(booking.getQrCodeContent());
                    
                    if (base64QR != null && !base64QR.isEmpty()) {
                        // Convert base64 to image
                        byte[] imgBytes = Base64.getDecoder().decode(base64QR);
                        Image qrCode = Image.getInstance(imgBytes);
                        qrCode.scaleToFit(200, 200);
                        qrCode.setAlignment(Element.ALIGN_CENTER);
                        document.add(qrCode);
                    } else {
                        // Add text notice if QR generation failed
                        Font qrTextFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.ITALIC);
                        Paragraph qrText = new Paragraph("QR Code not available", qrTextFont);
                        qrText.setAlignment(Element.ALIGN_CENTER);
                        document.add(qrText);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to add QR code to PDF: " + e.getMessage());
                    // Add text notice if QR generation failed
                    Font qrTextFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.ITALIC);
                    Paragraph qrText = new Paragraph("QR Code not available", qrTextFont);
                    qrText.setAlignment(Element.ALIGN_CENTER);
                    document.add(qrText);
                }
            }
            document.add(Chunk.NEWLINE);
            
            // Add booking info table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            
            // Add donor information
            addTableRow(table, "Donor Name:", booking.getDonorName(), headerFont, dataFont);
            addTableRow(table, "Email:", booking.getDonorEmail(), headerFont, dataFont);
            addTableRow(table, "Phone:", booking.getDonorPhone(), headerFont, dataFont);
            addTableRow(table, "Blood Group:", booking.getBloodGroup(), headerFont, dataFont);
            
            // Add appointment information
            addTableRow(table, "Date:", booking.getAppointmentDate() != null ? booking.getAppointmentDate().toString() : "", headerFont, dataFont);
            addTableRow(table, "Time:", booking.getAppointmentTime() != null ? booking.getAppointmentTime().toString() : "", headerFont, dataFont);
            addTableRow(table, "Location:", booking.getAppointmentLocation(), headerFont, dataFont);
            
            // Format status with color
            String status = booking.getStatus();
            BaseColor statusColor;
            if ("Confirmed".equals(status)) {
                statusColor = new BaseColor(52, 152, 219); // Blue
            } else if ("Completed".equals(status)) {
                statusColor = new BaseColor(46, 204, 113); // Green
            } else if ("Cancelled".equals(status)) {
                statusColor = new BaseColor(231, 76, 60); // Red
            } else {
                statusColor = new BaseColor(243, 156, 18); // Yellow/Orange for Pending
            }
            
            // Add status with colored cell
            PdfPCell statusLabelCell = new PdfPCell(new Phrase("Status:", headerFont));
            statusLabelCell.setBorderWidth(1);
            statusLabelCell.setPadding(5);
            table.addCell(statusLabelCell);
            
            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            statusFont.setColor(statusColor);
            PdfPCell statusValueCell = new PdfPCell(new Phrase(status, statusFont));
            statusValueCell.setBorderWidth(1);
            statusValueCell.setPadding(5);
            table.addCell(statusValueCell);
            
            if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
                addTableRow(table, "Notes:", booking.getNotes(), headerFont, dataFont);
            }
            
            document.add(table);
            document.add(Chunk.NEWLINE);
            
            // Add instructions
            Font instructionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Important Instructions:", instructionFont));
            
            // Fixed List creation - using proper iText approach
            com.itextpdf.text.List list = new com.itextpdf.text.List(false, 10);
            list.setIndentationLeft(20);
            Font listFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            
            ListItem item1 = new ListItem("Please bring a valid ID", listFont);
            ListItem item2 = new ListItem("Eat a healthy meal before donating", listFont);
            ListItem item3 = new ListItem("Get plenty of rest the night before", listFont);
            ListItem item4 = new ListItem("Drink extra water before donation", listFont);
            
            list.add(item1);
            list.add(item2);
            list.add(item3);
            list.add(item4);
            document.add(list);
            
            document.add(Chunk.NEWLINE);
            
            // Add footer - fixed to use proper font style
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC);
            Paragraph footer = new Paragraph("Thank you for your generosity in donating blood. Your donation can save lives!", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            // Add date information
            Paragraph dateInfo = new Paragraph("Generated on: " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 
                FontFactory.getFont(FontFactory.HELVETICA, 8));
            dateInfo.setAlignment(Element.ALIGN_RIGHT);
            document.add(dateInfo);
            
            document.close();
            
            // Verify file was created
            if (!file.exists() || file.length() == 0) {
                throw new IOException("Failed to create PDF file or file is empty");
            }
            
            return filePath;
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating booking confirmation PDF: " + e.getMessage(), e);
        }
    }
    
    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderWidth(1);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorderWidth(1);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }
}