package org.example.krishantutioncenter;

import org.junit.jupiter.api.BeforeEach;
import org.example.krishantutioncenter.config.*;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotesAiServiceTests {

    private LessonMaterialRepository materials;
    private AiUsageRepository usage;
    private AiQuotaService quota;
    private AiFeatureProperties properties;
    private AiTextGenerator generator;
    private NotesAiService notesAi;

    @BeforeEach
    void setUp() {
        materials = mock(LessonMaterialRepository.class);
        usage = mock(AiUsageRepository.class);
        quota = mock(AiQuotaService.class);
        properties = new AiFeatureProperties();
        properties.setApiKey("test-api-key");
        generator = mock(AiTextGenerator.class);
        notesAi = new NotesAiService(materials, usage, quota, properties, generator);
    }

    @Test
    void generatesBilingualSummaryAndPersistsSuggestedTags() {
        LessonMaterial note = note(51L, "Plant biology", "Green plants make food through photosynthesis using sunlight and chlorophyll.");
        when(materials.findWithBatchAndTagsById(51L)).thenReturn(Optional.of(note));
        when(generator.generateJson(anyString())).thenReturn(Map.of(
                "englishSummary", "Plants use sunlight and chlorophyll to make food.",
                "hindiSummary", "पौधे प्रकाश और क्लोरोफिल से भोजन बनाते हैं।",
                "tags", List.of("Photosynthesis", "Plants", "Chlorophyll")
        ));

        NotesAiService.SummaryResult result = notesAi.summarize(51L);

        assertEquals("Plants use sunlight and chlorophyll to make food.", result.englishSummary());
        assertEquals("पौधे प्रकाश और क्लोरोफिल से भोजन बनाते हैं।", result.hindiSummary());
        assertEquals(List.of("Photosynthesis", "Plants", "Chlorophyll"), result.tags());
        assertEquals(result.tags(), note.getTags());
        verify(quota).reserve();
        verify(materials).save(note);
    }

    @Test
    void answersOnlyWithBatchNotesAndReturnsCitedSources() {
        LessonMaterial matchingNote = note(61L, "Plant biology", "Green plants make food through photosynthesis using sunlight and chlorophyll.");
        when(materials.findNotesByBatchIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(matchingNote));
        when(generator.generateJson(anyString())).thenReturn(Map.of(
                "answer", "Plants use sunlight and chlorophyll during photosynthesis.",
                "sources", List.of(Map.of("id", 61, "quote",
                        "Green plants make food through photosynthesis using sunlight"))
        ));

        NotesAiService.DoubtResult result = notesAi.answerDoubt(7L, "How do plants use photosynthesis?");

        assertEquals("Plants use sunlight and chlorophyll during photosynthesis.", result.answer());
        assertEquals(List.of("Plant biology"), result.sources());
        assertFalse(result.needsTutor());
        verify(generator).generateJson(org.mockito.ArgumentMatchers.argThat(prompt ->
                prompt.contains("NOTE ID 61") && prompt.contains("photosynthesis")
                        && prompt.contains("How do plants use photosynthesis?")));
        verify(quota).reserve();
    }

    @Test
    void unsupportedQuestionDoesNotSendAnyNotesToProvider() {
        when(materials.findNotesByBatchIdOrderByCreatedAtDesc(8L))
                .thenReturn(List.of(note(70L, "Plant biology", "Plants make food using sunlight.")));

        NotesAiService.DoubtResult result = notesAi.answerDoubt(8L, "How do black holes evaporate?");

        assertTrue(result.needsTutor());
        assertTrue(result.sources().isEmpty());
        assertTrue(result.answer().contains("message your tutor"));
        verify(generator, never()).generateJson(anyString());
        verify(quota, never()).reserve();
    }

    @Test
    void unsupportedRomanizedHindiQuestionGetsHindiTutorFallback() {
        when(materials.findNotesByBatchIdOrderByCreatedAtDesc(8L)).thenReturn(List.of());

        NotesAiService.DoubtResult result = notesAi.answerDoubt(8L, "Kya black holes evaporate?");

        assertTrue(result.needsTutor());
        assertTrue(result.answer().contains("कृपया"));
        verify(generator, never()).generateJson(anyString());
        verify(quota, never()).reserve();
    }

    @Test
    void rejectsUncitedModelAnswerAndPromptsTutorInstead() {
        when(materials.findNotesByBatchIdOrderByCreatedAtDesc(9L))
                .thenReturn(List.of(note(81L, "Plant biology", "Green plants use chlorophyll in photosynthesis.")));
        when(generator.generateJson(anyString())).thenReturn(Map.of(
                "answer", "Plants use chlorophyll to make food.",
                "sources", List.of(Map.of("id", 999, "quote", "Plants use chlorophyll in photosynthesis."))
        ));

        NotesAiService.DoubtResult result = notesAi.answerDoubt(9L, "How do plants use chlorophyll?");

        assertTrue(result.needsTutor());
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void rejectsEvidenceQuotesThatDoNotAppearInTheRetrievedNotes() {
        when(materials.findNotesByBatchIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(note(91L, "Plant biology", "Green plants use chlorophyll in photosynthesis.")));
        when(generator.generateJson(anyString())).thenReturn(Map.of(
                "answer", "Chlorophyll reflects green light.",
                "sources", List.of(Map.of("id", 91, "quote", "Chlorophyll reflects green light from leaves."))
        ));

        NotesAiService.DoubtResult result = notesAi.answerDoubt(10L, "How does chlorophyll affect photosynthesis?");

        assertTrue(result.needsTutor());
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void rejectsProviderCallsWhenNoApiKeyIsConfigured() {
        properties.setApiKey("");

        org.junit.jupiter.api.Assertions.assertThrows(NotesAiService.AiUnavailableException.class,
                () -> notesAi.answerDoubt(9L, "How do plants use chlorophyll?"));

        verify(generator, never()).generateJson(anyString());
        verify(quota, never()).reserve();
    }

    @Test
    void rejectsOversizedSummaryWithoutSendingOrConsumingQuota() {
        when(materials.findWithBatchAndTagsById(101L))
                .thenReturn(Optional.of(note(101L, "Long note", "x".repeat(24_001))));

        org.junit.jupiter.api.Assertions.assertThrows(NotesAiService.AiUnavailableException.class,
                () -> notesAi.summarize(101L));

        verify(generator, never()).generateJson(anyString());
        verify(quota, never()).reserve();
    }

    private LessonMaterial note(Long id, String title, String text) {
        TeachingBatch batch = new TeachingBatch("Batch " + id, "Biology");
        LessonMaterial note = new LessonMaterial(title, "notes.txt", "text/plain", id + ".txt",
                text.length(), MaterialType.NOTE, batch, text);
        ReflectionTestUtils.setField(note, "id", id);
        return note;
    }
}
