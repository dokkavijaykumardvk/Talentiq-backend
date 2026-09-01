package com.Vijay.TalentIq.Repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Vijay.TalentIq.Model.User;

public interface UserRepo extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}