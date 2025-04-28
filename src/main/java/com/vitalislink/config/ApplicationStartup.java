package com.vitalislink.config;

import com.vitalislink.model.User;
import com.vitalislink.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartup {

    @Autowired
    private UserService userService;
    
    @Value("${app.admin.email:admin@vitalislink.com}")
    private String adminEmail;
    
    @Value("${app.admin.password:admin123}")
    private String adminPassword;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Create admin user if it doesn't exist
        if (userService.getUserByEmail(adminEmail).isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(adminPassword); // Will be encoded by service
            adminUser.setFullName("System Administrator");
            adminUser.setAdmin(true);
            
            userService.registerUser(adminUser);
            System.out.println("Created default admin user: " + adminEmail);
        }
    }
}