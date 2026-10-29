package com.vietgo.auth;

import com.vietgo.common.Patterns;
import com.vietgo.domain.Role;
import com.vietgo.domain.User;
import com.vietgo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.util.Optional;

/** Nghiep vu tai khoan: dang ky, cap nhat ho so, doi mat khau. */
@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public Optional<User> findByEmail(String email) {
        return email == null ? Optional.empty() : userRepo.findByEmailIgnoreCase(email.trim());
    }

    /** Kiem tra nghiep vu bo sung cho form dang ky (sau Bean Validation). */
    public void validateRegistration(RegisterForm form, Errors errors) {
        if (form.getEmail() != null && userRepo.existsByEmailIgnoreCase(form.getEmail().trim())) {
            errors.rejectValue("email", "duplicate", "Email này đã được đăng ký");
        }
        String phone = Patterns.normalizePhone(form.getPhone());
        if (phone != null && !phone.isBlank() && userRepo.existsByPhone(phone)) {
            errors.rejectValue("phone", "duplicate", "Số điện thoại này đã được đăng ký");
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
    }

    @Transactional
    public User register(RegisterForm form) {
        User u = new User();
        u.setFullName(Patterns.normalizeName(form.getFullName()));
        u.setEmail(form.getEmail().trim().toLowerCase());
        u.setPhone(Patterns.normalizePhone(form.getPhone()));
        u.setPasswordHash(encoder.encode(form.getPassword()));
        u.setRole(Role.USER);
        return userRepo.save(u);
    }

    @Transactional
    public void updateProfile(User user, ProfileForm form, Errors errors) {
        String phone = Patterns.normalizePhone(form.getPhone());
        if (userRepo.existsByPhoneAndIdNot(phone, user.getId())) {
            errors.rejectValue("phone", "duplicate", "Số điện thoại đã được tài khoản khác sử dụng");
            return;
        }
        User u = userRepo.findById(user.getId()).orElseThrow();
        u.setFullName(Patterns.normalizeName(form.getFullName()));
        u.setPhone(phone);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordForm form, Errors errors) {
        User u = userRepo.findById(user.getId()).orElseThrow();
        if (!encoder.matches(form.getCurrentPassword(), u.getPasswordHash())) {
            errors.rejectValue("currentPassword", "wrong", "Mật khẩu hiện tại không đúng");
            return;
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
            return;
        }
        if (encoder.matches(form.getNewPassword(), u.getPasswordHash())) {
            errors.rejectValue("newPassword", "same", "Mật khẩu mới phải khác mật khẩu hiện tại");
            return;
        }
        u.setPasswordHash(encoder.encode(form.getNewPassword()));
    }

    @Transactional
    public void toggleEnabled(Long userId, User admin) {
        User u = userRepo.findById(userId).orElseThrow();
        if (u.getId().equals(admin.getId())) {
            throw new com.vietgo.common.BusinessException("Không thể tự khóa tài khoản của chính bạn.");
        }
        u.setEnabled(!u.isEnabled());
    }
}
