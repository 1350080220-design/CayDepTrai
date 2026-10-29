package com.vietgo.auth;

import com.vietgo.domain.Booking;
import com.vietgo.domain.User;
import com.vietgo.repository.BookingRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Trang tai khoan: thong tin ca nhan, doi mat khau. */
@Controller
@RequestMapping("/tai-khoan")
public class ProfileController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final BookingRepository bookingRepo;

    public ProfileController(CurrentUser currentUser, UserService userService, BookingRepository bookingRepo) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.bookingRepo = bookingRepo;
    }

    @GetMapping
    public String page(Model model) {
        User u = currentUser.require();
        ProfileForm pf = new ProfileForm();
        pf.setFullName(u.getFullName());
        pf.setPhone(u.getPhone());
        model.addAttribute("profileForm", pf);
        model.addAttribute("passwordForm", new ChangePasswordForm());
        fillStats(model, u);
        return "account/index";
    }

    @PostMapping("/thong-tin")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result,
                                Model model, RedirectAttributes ra) {
        User u = currentUser.require();
        if (!result.hasErrors()) {
            userService.updateProfile(u, form, result);
        }
        if (result.hasErrors()) {
            model.addAttribute("passwordForm", new ChangePasswordForm());
            fillStats(model, u);
            return "account/index";
        }
        ra.addFlashAttribute("success", "Đã cập nhật thông tin cá nhân.");
        return "redirect:/tai-khoan";
    }

    @PostMapping("/mat-khau")
    public String changePassword(@Valid @ModelAttribute("passwordForm") ChangePasswordForm form, BindingResult result,
                                 Model model, RedirectAttributes ra) {
        User u = currentUser.require();
        if (!result.hasErrors()) {
            userService.changePassword(u, form, result);
        }
        if (result.hasErrors()) {
            ProfileForm pf = new ProfileForm();
            pf.setFullName(u.getFullName());
            pf.setPhone(u.getPhone());
            model.addAttribute("profileForm", pf);
            model.addAttribute("passwordTab", true);
            fillStats(model, u);
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            form.setConfirmPassword(null);
            return "account/index";
        }
        ra.addFlashAttribute("success", "Đã đổi mật khẩu thành công.");
        return "redirect:/tai-khoan";
    }

    private void fillStats(Model model, User u) {
        List<Booking> list = bookingRepo.findByUserOrderByCreatedAtDesc(u);
        model.addAttribute("bookingTotal", list.size());
        model.addAttribute("trainTotal", list.stream().filter(Booking::isTrain).filter(Booking::isPaid).count());
        model.addAttribute("busTotal", list.stream().filter(Booking::isBus).filter(Booking::isPaid).count());
        model.addAttribute("spent", list.stream().filter(b -> b.getPaidAt() != null)
                .mapToLong(b -> b.getTotalAmount() - b.getRefundAmount()).sum());
        model.addAttribute("recent", list.stream().limit(3).toList());
    }
}
