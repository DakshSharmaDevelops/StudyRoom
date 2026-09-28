package org.example.krishantutioncenter.controller;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
public class QuizController {

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final TeachingBatchRepository batches;

    public QuizController(QuizRepository quizzes, QuizAttemptRepository attempts, TeachingBatchRepository batches) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.batches = batches;
    }

    @GetMapping("/admin/quizzes")
    public String teacherQuizzes(Model model) {
        model.addAttribute("batches", batches.findAll());
        model.addAttribute("quizzes", quizzes.findAllByOrderByCreatedAtDesc());
        return "admin-quizzes";
    }

    @PostMapping("/admin/quizzes")
    public String createQuiz(@RequestParam String title,
                             @RequestParam String topic,
                             @RequestParam Long batchId,
                             @RequestParam(required = false) List<String> prompts,
                             @RequestParam(required = false) List<String> optionsA,
                             @RequestParam(required = false) List<String> optionsB,
                             @RequestParam(required = false) List<String> optionsC,
                             @RequestParam(required = false) List<String> optionsD,
                             @RequestParam(required = false) List<Integer> correctOptions,
                             RedirectAttributes redirectAttributes) {
        String cleanTitle = title.trim();
        String cleanTopic = topic.trim();
        if (cleanTitle.isEmpty() || cleanTitle.length() > 120 || cleanTopic.isEmpty() || cleanTopic.length() > 100) {
            redirectAttributes.addFlashAttribute("error", "Enter a quiz title and topic within the character limits.");
            return "redirect:/admin/quizzes";
        }
        TeachingBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null) {
            redirectAttributes.addFlashAttribute("error", "Choose an existing batch.");
            return "redirect:/admin/quizzes";
        }
        if (prompts == null || optionsA == null || optionsB == null || optionsC == null || optionsD == null
                || correctOptions == null || prompts.size() != optionsA.size() || prompts.size() != optionsB.size()
                || prompts.size() != optionsC.size() || prompts.size() != optionsD.size()
                || prompts.size() != correctOptions.size() || prompts.size() > 20) {
            redirectAttributes.addFlashAttribute("error", "Add up to 20 complete multiple-choice questions.");
            return "redirect:/admin/quizzes";
        }
        Quiz quiz = new Quiz(cleanTitle, cleanTopic, batch);
        for (int index = 0; index < prompts.size(); index++) {
            String prompt = prompts.get(index).trim();
            String a = optionsA.get(index).trim();
            String b = optionsB.get(index).trim();
            String c = optionsC.get(index).trim();
            String d = optionsD.get(index).trim();
            Integer correctOption = correctOptions.get(index);
            if (prompt.isEmpty() && a.isEmpty() && b.isEmpty() && c.isEmpty() && d.isEmpty()) {
                continue;
            }
            if (prompt.isEmpty() || prompt.length() > 500 || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()
                    || a.length() > 200 || b.length() > 200 || c.length() > 200 || d.length() > 200
                    || correctOption == null || correctOption < 0 || correctOption > 3) {
                redirectAttributes.addFlashAttribute("error", "Each question needs a prompt, four options, and one correct answer.");
                return "redirect:/admin/quizzes";
            }
            quiz.addQuestion(new QuizQuestion(prompt, a, b, c, d, correctOption));
        }
        if (quiz.getQuestions().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Add at least one complete question.");
            return "redirect:/admin/quizzes";
        }
        quizzes.save(quiz);
        redirectAttributes.addFlashAttribute("success", "Quiz published to " + batch.getName() + ".");
        return "redirect:/admin/quizzes";
    }

    @GetMapping("/student/quizzes")
    public String studentQuizzes(@AuthenticationPrincipal AppUser student, Model model) {
        model.addAttribute("quizzes", quizzes.findAllByBatchIdOrderByCreatedAtDesc(student.getBatch().getId()));
        return "student-quizzes";
    }

    @GetMapping("/student/quizzes/{id}")
    public String studentQuiz(@PathVariable Long id, @AuthenticationPrincipal AppUser student, Model model) {
        Quiz quiz = findForStudent(id, student);
        model.addAttribute("quiz", quiz);
        return "quiz";
    }

    @PostMapping("/student/quizzes/{id}")
    public String submitQuiz(@PathVariable Long id, @AuthenticationPrincipal AppUser student,
                             @RequestParam(required = false) List<Integer> answers,
                             RedirectAttributes redirectAttributes) {
        Quiz quiz = findForStudent(id, student);
        if (answers == null || answers.size() != quiz.getQuestions().size()
                || answers.stream().anyMatch(answer -> answer == null || answer < 0 || answer > 3)) {
            redirectAttributes.addFlashAttribute("error", "Answer each question before submitting.");
            return "redirect:/student/quizzes/" + id;
        }
        int score = 0;
        for (int index = 0; index < answers.size(); index++) {
            if (answers.get(index) == quiz.getQuestions().get(index).getCorrectOption()) {
                score++;
            }
        }
        QuizAttempt attempt = attempts.save(new QuizAttempt(quiz, student, score, answers.size()));
        return "redirect:/student/quizzes/result?id=" + attempt.getId();
    }

    @GetMapping("/student/quizzes/result")
    public String quizResult(@RequestParam Long id, @AuthenticationPrincipal AppUser student, Model model) {
        QuizAttempt attempt = attempts.findByIdAndStudentId(id, student.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        model.addAttribute("attempt", attempt);
        return "quiz-result";
    }

    private Quiz findForStudent(Long id, AppUser student) {
        Quiz quiz = quizzes.findWithBatchAndQuestionsById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (student.getBatch() == null || !student.getBatch().getId().equals(quiz.getBatch().getId())) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return quiz;
    }
}
