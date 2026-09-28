package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class MaterialController {

    private final LessonMaterialRepository materials;
    private final MaterialStorageService storage;
    private final TeachingBatchRepository batches;
    private final NotesAiService notesAi;

    public MaterialController(LessonMaterialRepository materials, MaterialStorageService storage,
                              TeachingBatchRepository batches, NotesAiService notesAi) {
        this.materials = materials;
        this.storage = storage;
        this.batches = batches;
        this.notesAi = notesAi;
    }

    @GetMapping("/student/materials/{id}")
    public String studentMaterial(@PathVariable Long id, @AuthenticationPrincipal AppUser student, Model model) {
        LessonMaterial material = findForStudent(id, student);
        model.addAttribute("material", material);
        model.addAttribute("aiConfigured", notesAi.isConfigured());
        model.addAttribute("aiUsage", notesAi.usageStatus());
        return "material";
    }

    @GetMapping("/student/materials/{id}/file")
    public ResponseEntity<Resource> studentFile(@PathVariable Long id, @AuthenticationPrincipal AppUser student) {
        return fileResponse(findForStudent(id, student));
    }

    @GetMapping("/admin/library/{id}/file")
    public ResponseEntity<Resource> adminFile(@PathVariable Long id) {
        LessonMaterial material = materials.findWithBatchAndTagsById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        return fileResponse(material);
    }

    @GetMapping("/admin/library")
    public String adminLibrary(Model model) {
        model.addAttribute("materials", materials.findTop20ByOrderByCreatedAtDesc());
        model.addAttribute("batches", batches.findAll());
        return "library";
    }

    @PostMapping("/admin/library")
    public String upload(@RequestParam String title,
                         @RequestParam MaterialType type,
                         @RequestParam Long batchId,
                         @RequestParam MultipartFile file,
                         RedirectAttributes redirectAttributes) throws java.io.IOException {
        String cleanTitle = title.trim();
        if (cleanTitle.isEmpty() || cleanTitle.length() > 160) {
            redirectAttributes.addFlashAttribute("error", "Enter a title up to 160 characters long.");
            return "redirect:/admin/library";
        }

        TeachingBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null) {
            redirectAttributes.addFlashAttribute("error", "Choose an existing batch.");
            return "redirect:/admin/library";
        }
        MaterialStorageService.StoredMaterial stored;
        try {
            stored = storage.store(file, type);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin/library";
        }
        materials.save(new LessonMaterial(cleanTitle, stored.originalFilename(), stored.contentType(),
                stored.storageKey(), stored.sizeBytes(), type, batch, stored.extractedText()));
        redirectAttributes.addFlashAttribute("success", "Material uploaded and shared with " + batch.getName() + ".");
        return "redirect:/admin/library";
    }

    @PostMapping("/admin/library/{id}/ai-summary")
    public String summarize(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            notesAi.summarize(id);
            redirectAttributes.addFlashAttribute("success", "English and Hindi summaries and topic tags generated.");
        } catch (NotesAiService.AiUnavailableException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/admin/library/{id}/ai-summary/clear")
    public String clearSummary(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LessonMaterial material = materials.findWithBatchAndTagsById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (material.getType() == MaterialType.NOTE) {
            material.setAiMetadata(null, null, java.util.List.of());
            materials.save(material);
            redirectAttributes.addFlashAttribute("success", "Generated summaries and tags removed.");
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/admin/library/{id}/ai-summary/review")
    public String reviewSummary(@PathVariable Long id,
                                @RequestParam String englishSummary,
                                @RequestParam String hindiSummary,
                                @RequestParam String tags,
                                @RequestParam(defaultValue = "false") boolean published,
                                RedirectAttributes redirectAttributes) {
        LessonMaterial material = materials.findWithBatchAndTagsById(id)
                .filter(candidate -> candidate.getType() == MaterialType.NOTE)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        String english = englishSummary.trim();
        String hindi = hindiSummary.trim();
        java.util.List<String> cleanTags = java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
        if (english.isEmpty() || english.length() > 4000 || hindi.isEmpty() || hindi.length() > 4000
                || cleanTags.size() < 3 || cleanTags.size() > 8
                || cleanTags.stream().anyMatch(tag -> tag.length() > 50)) {
            redirectAttributes.addFlashAttribute("error",
                    "Keep both summaries under 4,000 characters and enter 3–8 tags of up to 50 characters.");
            return "redirect:/admin/library";
        }
        material.reviewAiMetadata(english, hindi, cleanTags, published);
        materials.save(material);
        redirectAttributes.addFlashAttribute("success", published
                ? "Reviewed summary and tags published to the batch."
                : "AI summary and tags saved as a draft.");
        return "redirect:/admin/library";
    }

    private LessonMaterial findForStudent(Long id, AppUser student) {
        LessonMaterial material = materials.findWithBatchAndTagsById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (student.getBatch() == null || !student.getBatch().getId().equals(material.getBatch().getId())) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return material;
    }

    private ResponseEntity<Resource> fileResponse(LessonMaterial material) {
        var path = storage.resolve(material.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getContentType()))
                .contentLength(material.getSizeBytes())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(material.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(resource);
    }
}
