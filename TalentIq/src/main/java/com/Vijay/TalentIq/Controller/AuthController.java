package com.Vijay.TalentIq.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.Vijay.TalentIq.Model.LoginRequest;
import com.Vijay.TalentIq.Model.User;
import com.Vijay.TalentIq.Model.DTO.ForgotPasswordRequest;
import com.Vijay.TalentIq.Model.DTO.LoginResponse;
import com.Vijay.TalentIq.Model.DTO.MessageResponse;
import com.Vijay.TalentIq.Model.DTO.ResetPasswordRequest;
import com.Vijay.TalentIq.Model.DTO.SignupRequest;
import com.Vijay.TalentIq.Model.DTO.VerifyOtpRequest;
import com.Vijay.TalentIq.Model.DTO.VerifyOtpResponse;
import com.Vijay.TalentIq.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            User user = userService.getUserByEmail(request.email());

            return ResponseEntity.ok(new LoginResponse(
                    "Login successful",
                    user.getFirstname(),
                    user.getLastname(),
                    user.getEmail()
            ));
        } catch (org.springframework.security.authentication.DisabledException e) {
            return ResponseEntity.status(403).body(
                new MessageResponse("Please verify your email before logging in.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new MessageResponse("Invalid email or password"));
        }
    }

    // SIGNUP
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = new User();
            user.setFirstname(request.firstname());
            user.setLastname(request.lastname());
            user.setEmail(request.email());
            user.setPassword(request.password()); // plaintext here; UserServiceImpl hashes it

            User registeredUser = userService.registerUser(user);
            registeredUser.setPassword(null);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // VERIFY SIGNUP OTP
    @PostMapping("/api/auth/verify-signup-otp")
    public ResponseEntity<?> verifySignupOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            userService.verifySignupOtp(request.email(), request.otp());
            return ResponseEntity.ok(new MessageResponse("Email verified successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // RESEND SIGNUP OTP
    @PostMapping("/api/auth/resend-signup-otp")
    public ResponseEntity<?> resendSignupOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.resendSignupOtp(request.email());
        } catch (Exception e) {
            // Never reveal whether the account exists
        }
        return ResponseEntity.ok(
            new MessageResponse("If an account exists for this email, a new OTP has been sent.")
        );
    }

    // FORGOT PASSWORD (send OTP)
    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            userService.initiatePasswordReset(request.email());
        } catch (Exception e) {
            // Never reveal whether the email exists or leak internal errors
        }
        return ResponseEntity.ok(
            new MessageResponse("If an account exists for this email, an OTP has been sent.")
        );
    }

    // VERIFY OTP (password reset)
    @PostMapping("/api/auth/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            String resetToken = userService.verifyOtp(request.email(), request.otp());
            return ResponseEntity.ok(new VerifyOtpResponse("OTP verified successfully.", resetToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    // RESET PASSWORD
    @PostMapping("/api/auth/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Passwords do not match"));
        }

        try {
            userService.resetPassword(request.email(), request.resetToken(), request.newPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}