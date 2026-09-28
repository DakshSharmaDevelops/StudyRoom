package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
public class SetupController {

    private final AppUserRepository users;
    private final AdminAccountSetupService setup;

    public SetupController(AppUserRepository users, AdminAccountSetupService setup) {
        this.users = users;
        this.setup = setup;
    }

    @GetMapping("/setup")
    public String setupPage(HttpServletRequest request, Model model) {
        requireLocalRequest(request);
        long adminCount = users.countByRole(AccountRole.ADMIN);
        if (adminCount > 1) {
            return "redirect:/login";
        }
        model.addAttribute("replacingTeacher", adminCount == 1);
        return "setup";
    }

    @PostMapping("/setup")
    public String createAdmin(HttpServletRequest request,
                              @RequestParam String displayName,
                              @RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              @RequestParam(defaultValue = "false") boolean confirmReplacement,
                              RedirectAttributes redirectAttributes) {
        requireLocalRequest(request);
        String cleanName = displayName.trim();
        String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        if (cleanName.isEmpty() || cleanName.length() > 100
                || !cleanUsername.matches("[a-z0-9._-]{3,80}")
                || cleanEmail.length() > 254
                || !cleanEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            redirectAttributes.addFlashAttribute("error",
                    "Enter your name, a valid email address, and a username with 3–80 letters, numbers, dots, dashes or underscores.");
            return "redirect:/setup";
        }
        if (password.length() < 12) {
            redirectAttributes.addFlashAttribute("error", "Choose a password with at least 12 characters.");
            return "redirect:/setup";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "The passwords do not match.");
            return "redirect:/setup";
        }
        boolean replacingTeacher = users.countByRole(AccountRole.ADMIN) == 1;
        if (replacingTeacher && !confirmReplacement) {
            redirectAttributes.addFlashAttribute("error",
                    "Confirm that you want to replace the teacher login before continuing.");
            return "redirect:/setup";
        }
        try {
            if (replacingTeacher) {
                setup.replaceTeacherLogin(cleanName, cleanUsername, password, cleanEmail);
            } else {
                setup.createInitialAdmin(cleanName, cleanUsername, password, cleanEmail);
            }
        } catch (IllegalStateException exception) {
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/setup";
        }
        redirectAttributes.addFlashAttribute("success", replacingTeacher
                ? "Teacher login updated. Your classroom data has been kept."
                : "Teacher account created. Sign in with your new credentials.");
        return "redirect:/login";
    }

    private void requireLocalRequest(HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                throw new ResponseStatusException(FORBIDDEN, "Initial account setup is only available on this computer.");
            }
        } catch (UnknownHostException exception) {
            throw new ResponseStatusException(FORBIDDEN, "Initial account setup is only available on this computer.");
        }
    }
}
