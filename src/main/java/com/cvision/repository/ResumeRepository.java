package com.cvision.repository;

import com.cvision.model.ResumeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeRepository extends MongoRepository<ResumeDocument, String> {
}
