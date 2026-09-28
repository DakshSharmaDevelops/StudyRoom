package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService passwordReset;

    public PasswordResetController(PasswordResetService passwordReset) {
        this.passwordReset = passwordReset;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestCode(@RequestParam String username, RedirectAttributes attributes) {
        try {
            passwordReset.sendCode(username);
        } catch (PasswordResetDeliveryException exception) {
            logger.warn("Password reset email delivery failed.", exception);
        }
        attributes.addFlashAttribute("notice",
                "If a teacher account has this username and a recovery email, a code will be sent. "
                        + "If it does not arrive, email delivery may need to be configured.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String username,
                                @RequestParam String code,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                RedirectAttributes attributes) {
        if (password.length() < 12) {
            attributes.addFlashAttribute("error", "Choose a password with at least 12 characters.");
            return "redirect:/reset-password";
        }
        if (!password.equals(confirmPassword)) {
            attributes.addFlashAttribute("error", "The passwords do not match.");
            return "redirect:/reset-password";
        }
        if (!passwordReset.resetPassword(username, code.trim(), password)) {
            attributes.addFlashAttribute("error", "The username or code is invalid or expired. Request a new code.");
            return "redirect:/reset-password";
        }
        attributes.addFlashAttribute("success", "Password reset. Sign in with your new password.");
        return "redirect:/login";
    }
}
