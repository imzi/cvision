package com.cvision.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resumes")
@Getter
@Setter
public class ResumeDocument {
    @Id
    private String id;
    private String originalFileName;
    private String filePath;
    private String contentType;
    private String parsedText;
    private String education;
    private String experienceYears;
    private List<String> skills;
    private List<String> certifications;
    private LocalDateTime uploadedAt;
}
