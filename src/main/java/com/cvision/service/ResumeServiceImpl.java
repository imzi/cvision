package com.cvision.service;

import jakarta.annotation.PostConstruct;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeServiceImpl implements ResumeService{

    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    private DictionaryLemmatizer lemmatizer;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "with", "without",
            "for", "in", "on", "at", "by", "to", "from", "of", "is", "are"
    );

    private static final Set<String> SKILL_SET = Set.of("java", "spring", "python", "aws", "docker", "kubernetes", "react", "node", "sql", "git");
    private static final Set<String> DEGREE_KEYWORDS = Set.of("bachelor", "master", "phd", "bsc", "msc", "mba", "mca");
    private static final Set<String> CERTIFICATIONS = Set.of("aws certified", "ocjp", "azure certified", "google cloud certified", "pmp", "scrum master");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+)\\s*(\\+)?\\s*(year|yr|years|yrs)");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_/]+");
    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://(www\\.)?github\\.com/[a-zA-Z0-9\\-_/]+");
    private static final Pattern UNIVERSITY_PATTERN = Pattern.compile(
            "(?i)([A-Z][a-z]+(?:\\s+of)?\\s+(university|institute|college)[\\w\\s]*)"
    );
    private static final Pattern PROJECT_TITLE_PATTERN = Pattern.compile("(?m)^[-•\\*]\\s*([A-Z][\\w\\s]{3,50})");



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

    public Map<String, Object> extractEntities(String originalText, List<String> lemmatizedWords) {
        Map<String, Object> result = new HashMap<>();

        // Skills
        List<String> skills = lemmatizedWords.stream()
                .filter(SKILL_SET::contains)
                .distinct()
                .collect(Collectors.toList());

        // Education
        Optional<String> education = lemmatizedWords.stream()
                .filter(DEGREE_KEYWORDS::contains)
                .findFirst();

        // Certifications
        List<String> certs = CERTIFICATIONS.stream()
                .filter(cert -> originalText.toLowerCase().contains(cert))
                .toList();

        // Experience (years)
        Matcher matcher = EXPERIENCE_PATTERN.matcher(originalText.toLowerCase());
        Integer years = null;
        if (matcher.find()) {
            years = Integer.parseInt(matcher.group(1));
        }

        // Links (LinkedIn, GitHub, University)
        Map<String, String> linksAndOrgs = new HashMap<>();

        Matcher linkedin = LINKEDIN_PATTERN.matcher(originalText);
        if (linkedin.find()) linksAndOrgs.put("linkedin", linkedin.group()); // TODO: need to work on these

        Matcher github = GITHUB_PATTERN.matcher(originalText);
        if (github.find()) linksAndOrgs.put("github", github.group());

        Matcher university = UNIVERSITY_PATTERN.matcher(originalText);
        if (university.find()) linksAndOrgs.put("university", university.group().trim());

        // Project Names
        List<String> projectNames = new ArrayList<>();
        Matcher projectMatcher = PROJECT_TITLE_PATTERN.matcher(originalText);
        while (projectMatcher.find()) {
            projectNames.add(projectMatcher.group(1).trim());
        }

        // Add the results to the map
        result.put("skills", skills);
        result.put("education", education.orElse(null));
        result.put("certifications", certs);
        result.put("experienceYears", years);
        result.put("linksAndOrgs", linksAndOrgs);
        result.put("projectNames", projectNames);

        return result;
    }

}
