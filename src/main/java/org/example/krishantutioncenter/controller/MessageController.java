package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

@Controller
public class MessageController {

    private final AppUserRepository users;
    private final PrivateMessageRepository messages;

    public MessageController(AppUserRepository users, PrivateMessageRepository messages) {
        this.users = users;
        this.messages = messages;
    }

    @GetMapping("/student/messages")
    public String studentInbox(@AuthenticationPrincipal AppUser student, Model model) {
        model.addAttribute("student", student);
        model.addAttribute("messages", messages.findAllByStudentIdOrderByCreatedAtAsc(student.getId()));
        return "student-messages";
    }

    @PostMapping("/student/messages")
    public String studentSend(@AuthenticationPrincipal AppUser student, @RequestParam String body,
                              RedirectAttributes redirectAttributes) {
        String cleanBody = body.trim();
        if (cleanBody.isEmpty() || cleanBody.length() > 2000) {
            redirectAttributes.addFlashAttribute("error", "Messages must be between 1 and 2,000 characters.");
            return "redirect:/student/messages";
        }
        messages.save(new PrivateMessage(student, student, cleanBody));
        return "redirect:/student/messages";
    }

    @GetMapping("/admin/messages")
    public String teacherInbox(@RequestParam(required = false) Long studentId, Model model) {
        List<AppUser> students = users.findAllByRoleOrderByDisplayName(AccountRole.STUDENT);
        Long selectedId = studentId;
        if (selectedId == null && !students.isEmpty()) {
            selectedId = messages.findTop100ByOrderByCreatedAtDesc().stream()
                    .filter(message -> message.getSender().getRole() == AccountRole.STUDENT)
                    .map(message -> message.getStudent().getId())
                    .findFirst().orElse(students.get(0).getId());
        }
        Long selectedStudentId = selectedId;
        AppUser selected = selectedStudentId == null ? null : students.stream()
                .filter(student -> Objects.equals(student.getId(), selectedStudentId))
                .findFirst().orElse(null);
        List<PrivateMessage> conversation = selected == null ? List.of()
                : messages.findAllByStudentIdOrderByCreatedAtAsc(selected.getId());
        model.addAttribute("students", students);
        model.addAttribute("selectedStudent", selected);
        model.addAttribute("messages", conversation);
        return "admin-messages";
    }

    @PostMapping("/admin/messages")
    public String teacherSend(Authentication authentication, @RequestParam Long studentId,
                              @RequestParam String body, RedirectAttributes redirectAttributes) {
        AppUser student = users.findById(studentId)
                .filter(user -> user.getRole() == AccountRole.STUDENT)
                .orElse(null);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Choose an existing student conversation.");
            return "redirect:/admin/messages";
        }
        String cleanBody = body.trim();
        if (cleanBody.isEmpty() || cleanBody.length() > 2000) {
            redirectAttributes.addFlashAttribute("error", "Messages must be between 1 and 2,000 characters.");
            return "redirect:/admin/messages?studentId=" + studentId;
        }
        AppUser teacher = users.findByUsername(authentication.getName())
                .filter(user -> user.getRole() == AccountRole.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Signed-in administrator account was not found."));
        messages.save(new PrivateMessage(student, teacher, cleanBody));
        return "redirect:/admin/messages?studentId=" + studentId;
    }
}
