package com.vietgo.repository;

import com.vietgo.domain.Role;
import com.vietgo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByRole(Role role);

    List<User> findAllByOrderByCreatedAtDesc();
}
