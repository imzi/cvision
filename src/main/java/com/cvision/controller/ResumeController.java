package com.cvision.controller;

import com.cvision.model.ResumeDocument;
import com.cvision.repository.ResumeRepository;
import com.cvision.service.ResumeService;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resume")
public class ResumeController {
    private final Tika tika = new Tika();

    private static final String UPLOAD_DIR = "uploads/";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private final ResumeRepository resumeRepository;
    private final ResumeService resumeService;

    public ResumeController(ResumeRepository resumeRepository, ResumeService resumeService) {
        this.resumeRepository = resumeRepository;
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume (@RequestParam("file")MultipartFile file){
        //- Uploads to S3
        //- Stores metadata (file name, path, userId) in DB
        try {
            if (!file.getContentType().equals(APPLICATION_PDF) && !file.getContentType().equals(DOCX)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF or DOCX allowed! Received: " + file.getContentType());
            }

            // Create upload directory if missing
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file locally
            Path resumePath = uploadPath.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), resumePath, StandardCopyOption.REPLACE_EXISTING);

            // Parse resume
            String parsedText = tika.parseToString(file.getInputStream());
            List<String> cleanedTokens = resumeService.preprocess(parsedText);
            String cleanedText = String.join(" ", cleanedTokens);
            Map<String, Object> extracted = resumeService.extractEntities(parsedText, cleanedTokens);
            // Save metadata + text into MongoDB
            ResumeDocument doc = new ResumeDocument();
            doc.setOriginalFileName(file.getOriginalFilename());
            doc.setFilePath(resumePath.toAbsolutePath().toString());
            doc.setContentType(file.getContentType());
            doc.setParsedText(cleanedText);
            doc.setSkills((List<String>) extracted.get("skills"));
            doc.setEducation((String) extracted.get("education"));
            doc.setExperienceYears(String.valueOf((Integer) extracted.get("experienceYears")));
            doc.setCertifications((List<String>) extracted.get("certifications"));
            doc.setUploadedAt(LocalDateTime.now());

            ResumeDocument savedDoc = resumeRepository.save(doc);

            // Return basic info
            Map<String, Object> response = new HashMap<>();
            response.put("resumeId", savedDoc.getId());
            response.put("message", "Resume uploaded and parsed successfully.");
            response.put("uploadedAt", savedDoc.getUploadedAt());

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload and parse resume: " + e.getMessage());
        }
    }
}
