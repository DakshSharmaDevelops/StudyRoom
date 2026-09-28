package org.example.krishantutioncenter.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiDoubtController {

    private final NotesAiService notesAi;

    public AiDoubtController(NotesAiService notesAi) {
        this.notesAi = notesAi;
    }

    @GetMapping("/student/ask")
    public String ask(@AuthenticationPrincipal AppUser student, Model model) {
        model.addAttribute("student", student);
        model.addAttribute("aiConfigured", notesAi.isConfigured());
        model.addAttribute("aiUsage", notesAi.usageStatus());
        return "student-ask";
    }

    @PostMapping("/student/ask")
    public String answer(@AuthenticationPrincipal AppUser student,
                         @RequestParam String question,
                         RedirectAttributes redirectAttributes) {
        try {
            NotesAiService.DoubtResult result = notesAi.answerDoubt(student.getBatch().getId(), question);
            redirectAttributes.addFlashAttribute("question", question.trim());
            redirectAttributes.addFlashAttribute("answer", result.answer());
            redirectAttributes.addFlashAttribute("sources", result.sources());
            redirectAttributes.addFlashAttribute("needsTutor", result.needsTutor());
        } catch (NotesAiService.AiUnavailableException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/ask";
    }
}
