package com.Vijay.TalentIq.Model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "First name is required")
    private String firstname;

    @NotBlank(message = "Last name is required")
    private String lastname;

    // Strength is enforced by SignupRequest (plaintext, pre-hash).
    // This field only ever stores the bcrypt hash — a @Pattern here
    // would fail validation on every save() since a hash never matches
    // a plaintext-strength regex.
    @NotBlank(message = "Password is required")
    private String password;

    private boolean emailVerified;
    private String signupOtp;
    private LocalDateTime signupOtpExpiry;

    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getSignupOtp() { return signupOtp; }
    public void setSignupOtp(String signupOtp) { this.signupOtp = signupOtp; }
    public LocalDateTime getSignupOtpExpiry() { return signupOtpExpiry; }
    public void setSignupOtpExpiry(LocalDateTime signupOtpExpiry) { this.signupOtpExpiry = signupOtpExpiry; }
}