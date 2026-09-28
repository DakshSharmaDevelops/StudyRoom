package org.example.krishantutioncenter;

import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@AutoConfigureMockMvc
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:studyroom-test;DB_CLOSE_DELAY=-1")
class KrishanTutionCenterApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeachingBatchRepository batches;

    @Autowired
    private AppUserRepository users;

    @Test
    void contextLoads() {
    }

    @Test
    void signInPageIsPublicAndDashboardIsProtected() throws Exception {
        if (users.countByRole(AccountRole.ADMIN) == 0) {
            mockMvc.perform(get("/setup"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("setup"));
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/setup"));
            mockMvc.perform(get("/login"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/setup"));
        } else {
            mockMvc.perform(get("/setup"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("setup"));
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin").with(user("teacher").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
        mockMvc.perform(get("/admin/students").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardAssetsAreServed() throws Exception {
        mockMvc.perform(get("/css/dashboard.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/js/dashboard.js"))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCanCreateBatchAndStudentAccountButStudentCannotManageAccounts() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/admin/batches")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Class " + suffix)
                        .param("subject", "Mathematics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/batches"));

        TeachingBatch batch = batches.findAll().stream()
                .filter(candidate -> candidate.getName().equals("Class " + suffix))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/admin/batches").with(user("teacher").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("batches"));
        mockMvc.perform(get("/admin/library").with(user("teacher").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("library"));

        mockMvc.perform(post("/admin/students")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("displayName", "Test Student")
                        .param("username", "student-" + suffix)
                        .param("password", "a-long-test-password")
                        .param("batchId", batch.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students"));

        AppUser student = users.findByUsername("student-" + suffix).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(AccountRole.STUDENT, student.getRole());
        org.junit.jupiter.api.Assertions.assertNotEquals("a-long-test-password", student.getPassword());

        String noteText = "Fractions are equal when their numerator and denominator have the same ratio.";
        mockMvc.perform(multipart("/admin/library")
                        .file(new MockMultipartFile("file", "fractions.txt", "text/plain",
                                noteText.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Fractions")
                        .param("type", "NOTE")
                        .param("batchId", batch.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/library"));

        LessonMaterial material = org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> materialRepository.findAll().stream()
                        .filter(candidate -> candidate.getTitle().equals("Fractions"))
                        .findFirst().orElseThrow());
        material = materialRepository.findWithBatchAndTagsById(material.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(noteText, material.getExtractedText());
        mockMvc.perform(post("/admin/library/" + material.getId() + "/ai-summary")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/library"));
        material.reviewAiMetadata("Equivalent fractions represent the same value.",
                "समतुल्य भिन्न एक ही मान दर्शाते हैं।",
                java.util.List.of("Fractions", "Equivalent fractions", "Ratios"), false);
        materialRepository.save(material);
        mockMvc.perform(get("/admin/library").with(user("teacher").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Review AI draft before sharing")));

        mockMvc.perform(post("/admin/quizzes")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Fractions check")
                        .param("topic", "Fractions")
                        .param("batchId", batch.getId().toString())
                        .param("prompts", "Which fraction equals one half?")
                        .param("optionsA", "1/3")
                        .param("optionsB", "2/4")
                        .param("optionsC", "3/4")
                        .param("optionsD", "4/5")
                        .param("correctOptions", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/quizzes"));
        Quiz quiz = quizzes.findAll().stream()
                .filter(candidate -> candidate.getTitle().equals("Fractions check"))
                .findFirst().orElseThrow();
        mockMvc.perform(get("/admin/quizzes").with(user("teacher").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-quizzes"));

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "student-" + suffix)
                        .param("password", "a-long-test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"))
                .andReturn();
        MockHttpSession studentSession = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(get("/student").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(view().name("student-home"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Student")));
        mockMvc.perform(get("/student/ask").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(view().name("student-ask"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AI setup needed")));
        mockMvc.perform(get("/student/quizzes").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(view().name("student-quizzes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fractions check")));
        mockMvc.perform(get("/student/quizzes/" + quiz.getId()).session(studentSession))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Which fraction equals one half?")));
        mockMvc.perform(post("/student/quizzes/" + quiz.getId())
                        .session(studentSession)
                        .with(csrf())
                        .param("answers", "1"))
                .andExpect(status().is3xxRedirection());
        QuizAttempt quizAttempt = quizAttempts.findAll().stream()
                .filter(attempt -> attempt.getStudent().getId().equals(student.getId()))
                .findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1, quizAttempt.getScore());
        mockMvc.perform(get("/student/quizzes/result")
                        .session(studentSession)
                        .param("id", quizAttempt.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("quiz-result"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1 / 1")));
        mockMvc.perform(get("/student/materials/" + material.getId())
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(noteText)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Equivalent fractions represent"))));
        mockMvc.perform(post("/admin/library/" + material.getId() + "/ai-summary/review")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .param("englishSummary", "Equivalent fractions represent the same value.")
                        .param("hindiSummary", "समतुल्य भिन्न एक ही मान दर्शाते हैं।")
                        .param("tags", "Fractions, Equivalent fractions, Ratios")
                        .param("published", "true"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/materials/" + material.getId()).session(studentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Equivalent fractions represent the same value.")));
        mockMvc.perform(get("/student/materials/" + material.getId() + "/file")
                        .session(studentSession))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().string(noteText));

        mockMvc.perform(post("/admin/batches")
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Other class " + suffix)
                        .param("subject", "Science"))
                .andExpect(status().is3xxRedirection());
        TeachingBatch otherBatch = batches.findAll().stream()
                .filter(candidate -> candidate.getName().equals("Other class " + suffix))
                .findFirst().orElseThrow();
        Quiz otherQuiz = new Quiz("Other class quiz", "Science", otherBatch);
        otherQuiz.addQuestion(new QuizQuestion("Which subject?", "Math", "Science", "Art", "Music", 1));
        quizzes.save(otherQuiz);
        mockMvc.perform(get("/student/quizzes/" + otherQuiz.getId()).session(studentSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(multipart("/admin/library")
                        .file(new MockMultipartFile("file", "private.txt", "text/plain",
                                "Only for another class".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .with(user("teacher").roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Private note")
                        .param("type", "NOTE")
                        .param("batchId", otherBatch.getId().toString()))
                .andExpect(status().is3xxRedirection());
        LessonMaterial privateMaterial = materialRepository.findAll().stream()
                .filter(candidate -> candidate.getTitle().equals("Private note"))
                .findFirst().orElseThrow();
        mockMvc.perform(get("/student/materials/" + privateMaterial.getId())
                        .session(studentSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/student/materials/" + privateMaterial.getId() + "/file")
                        .session(studentSession))
                .andExpect(status().isNotFound());

        String studentQuestion = "Can you explain equivalent fractions?";
        mockMvc.perform(post("/student/messages")
                        .session(studentSession)
                        .with(csrf())
                        .param("body", studentQuestion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/messages"));
        mockMvc.perform(get("/student/messages").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(view().name("student-messages"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(studentQuestion)));

        String teacherUsername = "teacher-" + suffix;
        users.save(new AppUser(teacherUsername, "Test Teacher", passwordEncoder.encode("another-long-password"),
                AccountRole.ADMIN, null));
        mockMvc.perform(get("/admin/messages")
                        .with(user(teacherUsername).roles("ADMIN"))
                        .param("studentId", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-messages"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(studentQuestion)));
        mockMvc.perform(post("/admin/messages")
                        .with(user(teacherUsername).roles("ADMIN"))
                        .with(csrf())
                        .param("studentId", student.getId().toString())
                        .param("body", "Let's review this in our next class."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/messages?studentId=" + student.getId()));

        mockMvc.perform(post("/admin/students")
                        .with(user("student").roles("STUDENT"))
                        .with(csrf())
                        .param("displayName", "Should Not Work")
                        .param("username", "not-allowed-" + suffix)
                        .param("password", "another-long-password")
                        .param("batchId", batch.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Autowired
    private LessonMaterialRepository materialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QuizRepository quizzes;

    @Autowired
    private QuizAttemptRepository quizAttempts;
}
