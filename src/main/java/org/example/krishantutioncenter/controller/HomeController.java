package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AppUserRepository users;

    public HomeController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return users.countByRole(AccountRole.ADMIN) == 0 ? "redirect:/setup" : "redirect:/login";
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin" : "redirect:/student";
    }

    @GetMapping("/login")
    public String login() {
        return users.countByRole(AccountRole.ADMIN) == 0 ? "redirect:/setup" : "login";
    }
}
