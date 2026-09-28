package org.example.krishantutioncenter.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
public class AdminController {

    private final AppUserRepository users;
    private final TeachingBatchRepository batches;
    private final PasswordEncoder passwordEncoder;
    private final LessonMaterialRepository materials;
    private final QuizRepository quizzes;
    private final NotesAiService notesAi;

    public AdminController(AppUserRepository users, TeachingBatchRepository batches,
                           PasswordEncoder passwordEncoder, LessonMaterialRepository materials, QuizRepository quizzes,
                           NotesAiService notesAi) {
        this.users = users;
        this.batches = batches;
        this.passwordEncoder = passwordEncoder;
        this.materials = materials;
        this.quizzes = quizzes;
        this.notesAi = notesAi;
    }

    @GetMapping("/admin")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("studentCount", users.countByRole(AccountRole.STUDENT));
        var allBatches = batches.findAll();
        model.addAttribute("batches", allBatches);
        model.addAttribute("batchStudentCounts", allBatches.stream().collect(java.util.stream.Collectors.toMap(
                TeachingBatch::getId,
                batch -> users.countByRoleAndBatchId(AccountRole.STUDENT, batch.getId()))));
        model.addAttribute("materialCount", materials.count());
        model.addAttribute("noteCount", materials.countByType(MaterialType.NOTE));
        model.addAttribute("videoCount", materials.countByType(MaterialType.VIDEO));
        model.addAttribute("recentMaterials", materials.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("quizCount", quizzes.count());
        model.addAttribute("teacherName", users.findByUsername(authentication.getName())
                .map(AppUser::getDisplayName).orElse(authentication.getName()));
        model.addAttribute("aiConfigured", notesAi.isConfigured());
        model.addAttribute("aiUsage", notesAi.usageStatus());
        return "dashboard";
    }

    @GetMapping("/admin/students")
    public String students(Model model) {
        model.addAttribute("students", users.findAllByRoleOrderByDisplayName(AccountRole.STUDENT));
        model.addAttribute("batches", batches.findAll());
        return "students";
    }

    @PostMapping("/admin/students")
    public String createStudent(@RequestParam String displayName,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam Long batchId,
                                RedirectAttributes redirectAttributes) {
        String cleanName = displayName.trim();
        String cleanUsername = username.trim().toLowerCase(Locale.ROOT);
        if (cleanName.isEmpty() || cleanName.length() > 100 || !cleanUsername.matches("[a-z0-9._-]{3,80}")) {
            redirectAttributes.addFlashAttribute("error", "Enter a name and a username (3–80 letters, numbers, dots, dashes or underscores).");
            return "redirect:/admin/students";
        }
        if (password.length() < 12) {
            redirectAttributes.addFlashAttribute("error", "Student passwords must be at least 12 characters.");
            return "redirect:/admin/students";
        }
        if (users.existsByUsername(cleanUsername)) {
            redirectAttributes.addFlashAttribute("error", "That username is already in use.");
            return "redirect:/admin/students";
        }
        TeachingBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null) {
            redirectAttributes.addFlashAttribute("error", "Choose an existing batch.");
            return "redirect:/admin/students";
        }
        users.save(new AppUser(cleanUsername, cleanName, passwordEncoder.encode(password), AccountRole.STUDENT, batch));
        redirectAttributes.addFlashAttribute("success", "Student account created. Share the username and password with the student privately.");
        return "redirect:/admin/students";
    }

    @GetMapping("/admin/batches")
    public String batches(Model model) {
        var allBatches = batches.findAll();
        var studentCounts = allBatches.stream().collect(java.util.stream.Collectors.toMap(
                TeachingBatch::getId,
                batch -> users.countByRoleAndBatchId(AccountRole.STUDENT, batch.getId())));
        model.addAttribute("batches", allBatches);
        model.addAttribute("studentCounts", studentCounts);
        return "batches";
    }

    @PostMapping("/admin/batches")
    public String createBatch(@RequestParam String name,
                              @RequestParam String subject,
                              RedirectAttributes redirectAttributes) {
        String cleanName = name.trim();
        String cleanSubject = subject.trim();
        if (cleanName.isEmpty() || cleanName.length() > 80 || cleanSubject.isEmpty() || cleanSubject.length() > 80) {
            redirectAttributes.addFlashAttribute("error", "Enter a batch name and subject (up to 80 characters each).");
            return "redirect:/admin/batches";
        }
        if (batches.existsByNameIgnoreCase(cleanName)) {
            redirectAttributes.addFlashAttribute("error", "A batch with that name already exists.");
            return "redirect:/admin/batches";
        }
        batches.save(new TeachingBatch(cleanName, cleanSubject));
        redirectAttributes.addFlashAttribute("success", "Batch created.");
        return "redirect:/admin/batches";
    }
}
