package com.cvision.service;

import jakarta.annotation.PostConstruct;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.stereotype.Service;


import java.io.InputStream;

@Service
public class OpenNLPService {

    private TokenizerME tokenizer;
    private DictionaryLemmatizer lemmatizer;

    @PostConstruct
    public void init() throws Exception {
        // Load tokenizer model
        try (InputStream tokenModelStream = getClass().getResourceAsStream("/static/opennlp/en-token.bin")) {
            TokenizerModel tokenizerModel = new TokenizerModel(tokenModelStream);
            tokenizer = new TokenizerME(tokenizerModel);
        }

        // Load lemmatizer dictionary (not .bin!)
        try (InputStream lemmatizerDictStream = getClass().getResourceAsStream("/static/opennlp/en-lemmatizer.dict")) {
            lemmatizer = new DictionaryLemmatizer(lemmatizerDictStream);
        }

        try (InputStream lemmatizerDictStream = getClass().getResourceAsStream("/static/opennlp/en-lemmatizer.dict")) {
            lemmatizer = new DictionaryLemmatizer(lemmatizerDictStream);
        }
    }

    public String[] tokenize(String sentence) {
        return tokenizer.tokenize(sentence);
    }

    public String[] lemmatize(String[] tokens, String[] posTags) {
        return lemmatizer.lemmatize(tokens, posTags);
    }
}