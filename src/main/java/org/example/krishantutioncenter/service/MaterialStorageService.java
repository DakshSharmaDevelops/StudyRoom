package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.model.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class MaterialStorageService {

    private static final long MAX_NOTE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
    private static final int MAX_EXTRACTED_CHARS = 200_000;

    private final Path storageRoot;

    public MaterialStorageService(@Value("${app.upload-dir:./data/uploads}") String uploadDirectory) {
        this.storageRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public StoredMaterial store(MultipartFile file, MaterialType type) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file to upload.");
        }
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        long size = file.getSize();
        if (type == MaterialType.NOTE) {
            if (size > MAX_NOTE_BYTES) {
                throw new IllegalArgumentException("PDF and text notes must be 20 MB or smaller.");
            }
            if (!extension.equals("pdf") && !extension.equals("txt")) {
                throw new IllegalArgumentException("Notes must be a PDF or plain text file.");
            }
            String extractedText = extractNote(file, extension);
            String key = UUID.randomUUID() + "." + extension;
            save(file, key);
            return new StoredMaterial(originalFilename, contentTypeForNote(extension), key, size, extractedText);
        }
        if (size > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Videos must be 500 MB or smaller.");
        }
        String expectedType = switch (extension) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            default -> throw new IllegalArgumentException("Videos must be MP4 or WebM files.");
        };
        if (!contentType.equals(expectedType) && !contentType.equals("application/octet-stream")) {
            throw new IllegalArgumentException("The uploaded video type does not match its file extension.");
        }
        try (var input = file.getInputStream()) {
            byte[] signature = input.readNBytes(12);
            boolean validSignature = extension.equals("mp4")
                    ? signature.length >= 8 && signature[4] == 'f' && signature[5] == 't'
                    && signature[6] == 'y' && signature[7] == 'p'
                    : signature.length >= 4 && (signature[0] & 0xff) == 0x1a
                    && (signature[1] & 0xff) == 0x45 && (signature[2] & 0xff) == 0xdf
                    && (signature[3] & 0xff) == 0xa3;
            if (!validSignature) {
                throw new IllegalArgumentException("The uploaded video file does not match its MP4 or WebM format.");
            }
        }
        String key = UUID.randomUUID() + "." + extension;
        save(file, key);
        return new StoredMaterial(originalFilename, expectedType, key, size, null);
    }

    public Path resolve(String storageKey) {
        Path resolved = storageRoot.resolve(storageKey).normalize();
        if (!resolved.getParent().equals(storageRoot)) {
            throw new IllegalArgumentException("Invalid stored file key.");
        }
        return resolved;
    }

    private String extractNote(MultipartFile file, String extension) throws IOException {
        if (extension.equals("txt")) {
            try (var input = file.getInputStream()) {
                byte[] bytes = input.readNBytes((int) MAX_NOTE_BYTES + 1);
                if (bytes.length > MAX_NOTE_BYTES) {
                    throw new IllegalArgumentException("Text notes must be 20 MB or smaller.");
                }
                try {
                    String text = StandardCharsets.UTF_8.newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes)).toString();
                    return requireUsableText(text);
                } catch (CharacterCodingException exception) {
                    throw new IllegalArgumentException("Text notes must use UTF-8 encoding.", exception);
                }
            }
        }
        byte[] signature;
        try (var input = file.getInputStream()) {
            signature = input.readNBytes(5);
        }
        if (signature.length != 5 || signature[0] != '%' || signature[1] != 'P'
                || signature[2] != 'D' || signature[3] != 'F' || signature[4] != '-') {
            throw new IllegalArgumentException("The selected file is not a valid PDF document.");
        }
        Files.createDirectories(storageRoot.getParent());
        Path temporary = Files.createTempFile(storageRoot.getParent(), "studyroom-note-", ".pdf");
        try {
            try (var input = file.getInputStream()) {
                Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            try (PDDocument document = Loader.loadPDF(temporary.toFile())) {
                if (document.isEncrypted() || document.getNumberOfPages() > 300) {
                    throw new IllegalArgumentException("PDF notes must be unencrypted and contain no more than 300 pages.");
                }
                return requireUsableText(new PDFTextStripper().getText(document));
            } catch (IOException exception) {
                throw new IllegalArgumentException("The PDF could not be read. Check that it is a valid, unencrypted PDF.", exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String requireUsableText(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("The note does not contain extractable text.");
        }
        if (trimmed.length() > MAX_EXTRACTED_CHARS) {
            throw new IllegalArgumentException("Notes may contain no more than 200,000 extracted characters.");
        }
        return trimmed;
    }

    private void save(MultipartFile file, String storageKey) throws IOException {
        Files.createDirectories(storageRoot);
        Path target = resolve(storageKey);
        try (var input = file.getInputStream()) {
            Files.copy(input, target);
        }
    }

    private String safeOriginalFilename(String suppliedFilename) {
        String clean = StringUtils.cleanPath(suppliedFilename == null ? "" : suppliedFilename)
                .replace('\\', '/');
        String filename = clean.substring(clean.lastIndexOf('/') + 1);
        if (filename.isBlank() || filename.length() > 255 || filename.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Choose a file with a valid filename.");
        }
        return filename;
    }

    private String extensionOf(String filename) {
        int separator = filename.lastIndexOf('.');
        if (separator < 1 || separator == filename.length() - 1) {
            throw new IllegalArgumentException("The uploaded file must have a supported extension.");
        }
        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String contentTypeForNote(String extension) {
        return extension.equals("pdf") ? "application/pdf" : "text/plain; charset=utf-8";
    }

    public record StoredMaterial(String originalFilename, String contentType, String storageKey,
                                 long sizeBytes, String extractedText) {
    }
}
