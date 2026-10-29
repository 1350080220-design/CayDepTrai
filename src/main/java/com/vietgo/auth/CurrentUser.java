package com.vietgo.auth;

import com.vietgo.domain.User;
import com.vietgo.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Lay thuc the User cua nguoi dang dang nhap. */
@Component
public class CurrentUser {

    private final UserRepository userRepo;

    public CurrentUser(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public Optional<User> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userRepo.findByEmailIgnoreCase(auth.getName());
    }

    /** Nguoi dung bat buoc da dang nhap (cac URL da duoc Spring Security bao ve). */
    public User require() {
        return find().orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                "Vui lòng đăng nhập"));
    }
}
