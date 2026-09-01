package com.Vijay.TalentIq.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.Vijay.TalentIq.Model.User;

public interface UserService extends UserDetailsService {
    User registerUser(User user);
    void initiatePasswordReset(String email);
    String verifyOtp(String email, String otp);
    void resetPassword(String email, String resetToken, String newPassword);

    // --- Signup email verification ---
    void verifySignupOtp(String email, String otp);
    void resendSignupOtp(String email);
    
 // --- Used after successful login to fetch display info (firstname/lastname) ---
    User getUserByEmail(String email);
}