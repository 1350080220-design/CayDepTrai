package com.vietgo.web;

import com.vietgo.auth.CurrentUser;
import com.vietgo.common.BusinessException;
import com.vietgo.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/** Du lieu dung chung cho moi trang + xu ly loi nghiep vu. */
@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    private final CurrentUser currentUser;

    public GlobalControllerAdvice(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUser.find().orElse(null);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Loi nghiep vu: quay lai trang truoc va hien thong bao. */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request) {
        return backWithError(request, ex.getMessage());
    }

    /** Vi pham rang buoc UNIQUE (vd: 2 nguoi cung giu 1 ghe trong cung tich tac). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Xung dot du lieu: {}", ex.getMostSpecificCause().getMessage());
        return backWithError(request, "Một hoặc nhiều chỗ bạn chọn vừa được người khác giữ. Vui lòng chọn lại.");
    }

    private String backWithError(HttpServletRequest request, String message) {
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", message);
        return "redirect:" + safeReferer(request);
    }

    /** Chi chap nhan duong dan noi bo cua ung dung. */
    private String safeReferer(HttpServletRequest request) {
        String ref = request.getHeader("Referer");
        if (ref == null) return "/";
        try {
            URI uri = URI.create(ref);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) return "/";
            String path = uri.getRawPath();
            String ctx = request.getContextPath();
            if (path == null || path.isEmpty()) return "/";
            if (!ctx.isEmpty() && path.startsWith(ctx)) path = path.substring(ctx.length());
            if (path.isEmpty()) path = "/";
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
