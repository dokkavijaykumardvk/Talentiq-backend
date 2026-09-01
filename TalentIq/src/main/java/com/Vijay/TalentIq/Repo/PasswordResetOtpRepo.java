package com.Vijay.TalentIq.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.TalentIq.Model.PasswordResetOtp;

public interface PasswordResetOtpRepo extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<PasswordResetOtp> findByEmailAndResetTokenAndUsedFalse(String email, String resetToken);

    List<PasswordResetOtp> findByEmailAndUsedFalse(String email);
}