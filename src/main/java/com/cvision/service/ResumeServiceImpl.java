package com.cvision.service;

import jakarta.annotation.PostConstruct;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ResumeServiceImpl implements ResumeService{

    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private DictionaryLemmatizer lemmatizer;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "with", "without",
            "for", "in", "on", "at", "by", "to", "from", "of", "is", "are"
    );

    @PostConstruct
    public void init() throws Exception {
        //TODO: Refine with Open NLP service
        try (
                InputStream tokenModelStream = getClass().getResourceAsStream("/static/opennlp/en-token.bin");
                InputStream posModelStream = getClass().getResourceAsStream("/static/opennlp/en-pos-maxent.bin");
                InputStream lemmaDictStream = getClass().getResourceAsStream("/static/opennlp/en-lemmatizer.dict")
        ) {
            assert tokenModelStream != null;
            TokenizerModel tokenizerModel = new TokenizerModel(tokenModelStream);
            tokenizer = new TokenizerME(tokenizerModel);

            assert posModelStream != null;
            POSModel posModel = new POSModel(posModelStream);
            posTagger = new POSTaggerME(posModel);

            assert lemmaDictStream != null;
            lemmatizer = new DictionaryLemmatizer(lemmaDictStream);
        }
    }

    public List<String> preprocess(String text) {
        // Lowercase & clean
        text = text.toLowerCase().replaceAll("[^a-z\\s]", " ");

        // Tokenize
        String[] tokens = tokenizer.tokenize(text);

        // POS tagging
        String[] posTags = posTagger.tag(tokens);

        // Lemmatization
        String[] lemmas = lemmatizer.lemmatize(tokens, posTags);

        // Filter: valid lemmas or fallback to token
        List<String> finalWords = new ArrayList<>();
        for (int i = 0; i < lemmas.length; i++) {
            String lemma = lemmas[i];
            if (!STOPWORDS.contains(lemma) && lemma.length() > 1 && !lemma.equals("O")) {
                finalWords.add(lemma);
            } else if (!STOPWORDS.contains(tokens[i]) && tokens[i].length() > 1) {
                finalWords.add(tokens[i]); // fallback to token
            }
        }

        return finalWords;
    }
}
