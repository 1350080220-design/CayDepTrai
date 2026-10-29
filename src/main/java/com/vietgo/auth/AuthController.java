package com.vietgo.auth;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Dang nhap, dang ky. */
@Controller
public class AuthController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public AuthController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dang-nhap")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String redirect,
                        Model model) {
        if (currentUser.find().isPresent()) return "redirect:/";
        if ("locked".equals(error)) {
            model.addAttribute("loginError", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        } else if (error != null) {
            model.addAttribute("loginError", "Email hoặc mật khẩu không đúng.");
        }
        model.addAttribute("redirect", redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")
                ? redirect : null);
        return "auth/login";
    }

    @GetMapping("/dang-ky")
    public String registerForm(Model model) {
        if (currentUser.find().isPresent()) return "redirect:/";
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/dang-ky")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
                           RedirectAttributes ra) {
        userService.validateRegistration(form, result);
        if (result.hasErrors()) {
            form.setPassword(null);
            form.setConfirmPassword(null);
            return "auth/register";
        }
        userService.register(form);
        ra.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập để bắt đầu đặt vé.");
        ra.addFlashAttribute("email", form.getEmail().trim().toLowerCase());
        return "redirect:/dang-nhap";
    }
}
