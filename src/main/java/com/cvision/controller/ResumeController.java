package com.cvision.controller;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/resume")
public class ResumeController {
    private final Tika tika = new Tika();

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadResume (@RequestParam("file")MultipartFile file){
        //- Uploads to S3
        //- Stores metadata (file name, path, userId) in DB
        try {
            if (!file.getContentType().equals("application/pdf") &&
                    !file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF/DOCX allowed! Received: " + file.getContentType());
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }
            Path resumePath =  uploadPath.resolve(file.getOriginalFilename());
            Files.copy(
                    file.getInputStream(),
                    resumePath,
                    StandardCopyOption.REPLACE_EXISTING
            );
            String text = tika.parseToString(file.getInputStream());
            return ResponseEntity.ok("File uploaded: "+text);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }
}
