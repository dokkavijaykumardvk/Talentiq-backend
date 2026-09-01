package com.Vijay.TalentIq.ServiceImpl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.Vijay.TalentIq.Model.PasswordResetOtp;
import com.Vijay.TalentIq.Model.User;
import com.Vijay.TalentIq.Repo.PasswordResetOtpRepo;
import com.Vijay.TalentIq.Repo.UserRepo;
import com.Vijay.TalentIq.service.EmailService;
import com.Vijay.TalentIq.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;

    private final UserRepo userRepo;
    private final PasswordResetOtpRepo passwordResetOtpRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserServiceImpl(
            UserRepo userRepo,
            PasswordResetOtpRepo passwordResetOtpRepo,
            BCryptPasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.userRepo = userRepo;
        this.passwordResetOtpRepo = passwordResetOtpRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // REGISTER USER — account created unverified, OTP sent to email
    @Override
    public User registerUser(User user) {
        if (userRepo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);

        String otp = generateOtp();
        user.setSignupOtp(passwordEncoder.encode(otp));
        user.setSignupOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));

        User savedUser = userRepo.save(user);

        emailService.sendSignupOtpEmail(savedUser.getEmail(), otp, OTP_EXPIRY_MINUTES);

        return savedUser;
    }

    // VERIFY SIGNUP OTP
    @Override
    public void verifySignupOtp(String email, String otp) {
        email = email.trim().toLowerCase();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        if (user.isEmailVerified()) {
            return; // already verified, nothing to do
        }

        if (user.getSignupOtp() == null || user.getSignupOtpExpiry() == null) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        if (user.getSignupOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        if (!passwordEncoder.matches(otp, user.getSignupOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setEmailVerified(true);
        user.setSignupOtp(null);
        user.setSignupOtpExpiry(null);
        userRepo.save(user);
    }

    // RESEND SIGNUP OTP
    @Override
    public void resendSignupOtp(String email) {
        email = email.trim().toLowerCase();

        Optional<User> userOpt = userRepo.findByEmail(email);
        if (userOpt.isEmpty()) {
            return; // don't leak whether the account exists
        }

        User user = userOpt.get();
        if (user.isEmailVerified()) {
            return;
        }

        String otp = generateOtp();
        user.setSignupOtp(passwordEncoder.encode(otp));
        user.setSignupOtpExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        userRepo.save(user);

        emailService.sendSignupOtpEmail(user.getEmail(), otp, OTP_EXPIRY_MINUTES);
    }

    // LOAD USER FOR LOGIN
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEmailVerified(),
                true,
                true,
                true,
                Collections.emptyList());
    }

    // FORGOT PASSWORD (send OTP)
    @Override
    public void initiatePasswordReset(String email) {
        email = email.trim().toLowerCase();

        Optional<User> userOpt = userRepo.findByEmail(email);

        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();

        Optional<PasswordResetOtp> lastOtp =
                passwordResetOtpRepo.findTopByEmailOrderByCreatedAtDesc(email);

        if (lastOtp.isPresent()) {
            LocalDateTime cooldownExpiry =
                    lastOtp.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (cooldownExpiry.isAfter(LocalDateTime.now())) {
                return;
            }
        }

        List<PasswordResetOtp> existing = passwordResetOtpRepo.findByEmailAndUsedFalse(email);
        existing.forEach(o -> o.setUsed(true));
        passwordResetOtpRepo.saveAll(existing);

        String otp = generateOtp();

        PasswordResetOtp record = new PasswordResetOtp();
        record.setUser(user);
        record.setEmail(email);
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        record.setAttempts(0);
        record.setVerified(false);
        record.setUsed(false);
        record.setCreatedAt(LocalDateTime.now());

        passwordResetOtpRepo.save(record);

        emailService.sendOtpEmail(user.getEmail(), otp, OTP_EXPIRY_MINUTES);
    }

    private String generateOtp() {
        int number = secureRandom.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    // VERIFY OTP (password reset)
    @Override
    public String verifyOtp(String email, String otp) {
        email = email.trim().toLowerCase();

        PasswordResetOtp record = passwordResetOtpRepo
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OTP"));

        if (record.isUsed()) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }

        if (record.getAttempts() >= MAX_ATTEMPTS) {
            throw new RuntimeException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            record.setAttempts(record.getAttempts() + 1);
            passwordResetOtpRepo.save(record);
            throw new RuntimeException("Invalid or expired OTP");
        }

        String resetToken = UUID.randomUUID().toString();
        record.setVerified(true);
        record.setResetToken(resetToken);
        record.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        passwordResetOtpRepo.save(record);

        return resetToken;
    }

    // RESET PASSWORD
    @Override
    public void resetPassword(String email, String resetToken, String newPassword) {
        email = email.trim().toLowerCase();

        PasswordResetOtp record = passwordResetOtpRepo
                .findByEmailAndResetTokenAndUsedFalse(email, resetToken)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset request"));

        if (!record.isVerified()) {
            throw new RuntimeException("OTP not verified");
        }

        if (record.getResetTokenExpiry() == null
                || record.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset session has expired. Please start again.");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset request"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        record.setUsed(true);
        passwordResetOtpRepo.save(record);
    }

	@Override
	public User getUserByEmail(String email) {
		return userRepo.findByEmail(email.trim().toLowerCase())
	            .orElseThrow(() -> new RuntimeException("User not found"));
	}
}