package com.cvision.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "resumes")
@Getter
@Setter
public class ResumeDocument {
    @Id
    private String id;
    private String originalFileName;
    private String filePath;
    private String contentType;
    private String originalText;
    private String parsedText;
    private Map<String, String> education;
    private Map<String, String> contact;
    private String university;
    private String experienceYears;
    private List<String> skills;
    private List<String> certifications;
    private LocalDateTime uploadedAt;
}
