package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentController {

    private final LessonMaterialRepository materials;
    private final NotesAiService notesAi;

    public StudentController(LessonMaterialRepository materials, NotesAiService notesAi) {
        this.materials = materials;
        this.notesAi = notesAi;
    }

    @GetMapping("/student")
    public String studentHome(@AuthenticationPrincipal AppUser student, Model model) {
        model.addAttribute("student", student);
        model.addAttribute("materials", materials.findAllByBatchIdOrderByCreatedAtDesc(student.getBatch().getId()));
        model.addAttribute("aiConfigured", notesAi.isConfigured());
        model.addAttribute("aiUsage", notesAi.usageStatus());
        return "student-home";
    }
}
