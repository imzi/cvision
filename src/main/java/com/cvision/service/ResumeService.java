package com.cvision.service;

import java.util.List;
import java.util.Map;

public interface ResumeService {
    List<String> preprocess(String text);
    Map<String, Object> extractEntities(String originalText, List<String> lemmatizedWords);
}
