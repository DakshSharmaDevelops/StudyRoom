package org.example.krishantutioncenter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialStorageServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesUtf8TextAndReplacesUntrustedFilenameWithStorageKey() throws Exception {
        MaterialStorageService storage = new MaterialStorageService(temporaryDirectory.resolve("uploads").toString());
        String text = "Area = length times width";
        MockMultipartFile file = new MockMultipartFile("file", "../notes.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));

        MaterialStorageService.StoredMaterial stored = storage.store(file, MaterialType.NOTE);

        assertEquals("notes.txt", stored.originalFilename());
        assertEquals(text, stored.extractedText());
        assertTrue(Files.isRegularFile(storage.resolve(stored.storageKey())));
        assertTrue(stored.storageKey().matches("[0-9a-f-]{36}\\.txt"));
    }

    @Test
    void extractsSelectableTextFromPdf() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            try (PDPageContentStream stream = new PDPageContentStream(document, document.getPage(0))) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Plants make food through photosynthesis.");
                stream.endText();
            }
            document.save(bytes);
        }
        MaterialStorageService storage = new MaterialStorageService(temporaryDirectory.resolve("pdf-uploads").toString());

        MaterialStorageService.StoredMaterial stored = storage.store(
                new MockMultipartFile("file", "science.pdf", "application/pdf", bytes.toByteArray()),
                MaterialType.NOTE);

        assertTrue(stored.extractedText().contains("photosynthesis"));
        assertTrue(Files.isRegularFile(storage.resolve(stored.storageKey())));
    }

    @Test
    void rejectsSpoofedPdfAndImageFileTypes() {
        MaterialStorageService storage = new MaterialStorageService(temporaryDirectory.resolve("invalid-uploads").toString());

        assertThrows(IllegalArgumentException.class, () -> storage.store(
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)),
                MaterialType.NOTE));
        assertThrows(IllegalArgumentException.class, () -> storage.store(
                new MockMultipartFile("file", "notes.png", "image/png", new byte[]{1, 2, 3}),
                MaterialType.NOTE));
    }
}
