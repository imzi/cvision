package com.cvision.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    private LocalDateTime uploadedAt;
}
