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

import static org.apache.commons.lang3.StringUtils.capitalize;

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
    private static final List<String> DEGREE_KEYWORDS = List.of(
            "bachelor", "b.sc", "bsc", "BSc", "Bsc",   "btech", "b.e", "b.eng",
            "master", "m.sc", "msc", "mtech", "m.e", "m.eng",
            "phd", "doctorate", "mba", "mca", "ba", "ma"
    );

    private static final List<String> FIELDS_OF_STUDY = List.of(
            "computer science", "information technology", "software engineering",
            "data science", "artificial intelligence", "machine learning",
            "electrical engineering", "mechanical engineering", "business administration",
            "finance", "marketing", "human resources"
    );
    private static final Set<String> CERTIFICATIONS = Set.of("aws certified", "ocjp", "azure certified", "google cloud certified", "pmp", "scrum master");
    // Regex: digit-based
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "(\\d{1,2})\\s*(\\+)?\\s*(years|yrs)?\\s*(of)?\\s*experience",
            Pattern.CASE_INSENSITIVE
    );

    // Regex: word-based (optional, for extra enhancement)
    private static final Map<String, Integer> WORD_NUMBER_MAP = Map.ofEntries(
            Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3),
            Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6),
            Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9),
            Map.entry("ten", 10)
    );

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9\\-_/]+");
    private static final Pattern GITHUB_PATTERN = Pattern.compile("https?://(www\\.)?github\\.com/[a-zA-Z0-9\\-_/]+");
    private static final Pattern UNIVERSITY_PATTERN = Pattern.compile(
            "\\b(?:[A-Z][a-z]+\\s)*University(?:\\s(?:of\\s(?:[A-Z][a-z]+\\s?)+))?\\b"
    );
    private static final Pattern PROJECT_TITLE_PATTERN = Pattern.compile("(?m)^[-•\\*]\\s*([A-Z][\\w\\s]{3,50})");

    // Email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Phone number (Sri Lanka example and general)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+94|0)?[\\s-]?(7[01245678])[\\s-]?[0-9]{3}[\\s-]?[0-9]{4}"
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

    public Map<String, Object> extractEntities(String originalText, List<String> lemmatizedWords) {
        Map<String, Object> result = new HashMap<>();

        // Skills
        List<String> skills = lemmatizedWords.stream()
                .filter(SKILL_SET::contains)
                .distinct()
                .collect(Collectors.toList());

        // Education
        Pattern DEGREE_PATTERN = Pattern.compile(
                "(bachelor|master|msc|bsc|mba|phd|diploma|associate)[\\w\\s,-]*?(computer science|information technology|software engineering|engineering|ict|ai|artificial intelligence|data science)?",
                Pattern.CASE_INSENSITIVE
        );

        // Certifications
        List<String> certs = CERTIFICATIONS.stream()
                .filter(cert -> originalText.toLowerCase().contains(cert))
                .toList();

        // Experience (years)
        String years = extractExperienceYears(originalText);

        // Links (LinkedIn, GitHub, University)
        Map<String, String> linksAndOrgs = new HashMap<>();

        Matcher linkedin = LINKEDIN_PATTERN.matcher(originalText);
        if (linkedin.find()) linksAndOrgs.put("linkedin", linkedin.group()); // TODO: need to work on these

        Matcher github = GITHUB_PATTERN.matcher(originalText);
        if (github.find()) linksAndOrgs.put("github", github.group());

        String universityName = null;
        Matcher university = UNIVERSITY_PATTERN.matcher(originalText);
        if (university.find()) {
            universityName =  university.group().trim();
        }
        // Project Names
        List<String> projectNames = new ArrayList<>();
        Matcher projectMatcher = PROJECT_TITLE_PATTERN.matcher(originalText);
        while (projectMatcher.find()) {
            projectNames.add(projectMatcher.group(1).trim());
        }


        Map<String, String> contact = new HashMap<>();
        contact.put("email", extractFirstMatch(EMAIL_PATTERN, originalText));
        contact.put("phone", extractFirstMatch(PHONE_PATTERN, originalText));



        // Add the results to the map
        result.put("skills", skills);
        result.put("education", extractEducation(originalText));
        result.put("certifications", certs);
        result.put("experienceYears", years);
        result.put("linksAndOrgs", linksAndOrgs);
        result.put("projectNames", projectNames);
        result.put("contact", contact);

        return result;
    }

    private static String extractFirstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public String extractExperienceYears(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1); // returns the digit part
        }

        // Optional: Check for word-based years
        for (Map.Entry<String, Integer> entry : WORD_NUMBER_MAP.entrySet()) {
            if (text.toLowerCase().contains(entry.getKey() + " years")) {
                return String.valueOf(entry.getValue());
            }
        }

        return "null"; // default if nothing found
    }
    public Map<String, String> extractEducation(String text) {
        String lower = text.toLowerCase();

        String degree = null;
        for (String keyword : DEGREE_KEYWORDS) {
            if (lower.contains(keyword)) {
                degree = keyword;
                break;
            }
        }

        String field = null;
        for (String study : FIELDS_OF_STUDY) {
            if (lower.contains(study)) {
                field = study;
                break;
            }
        }

        String universityName = null;
        Matcher university = UNIVERSITY_PATTERN.matcher(text);
        if (university.find()) {
            universityName =  university.group().trim();
        }


        Map<String, String> result = new HashMap<>();
        result.put("degree", degree != null ? degree : "unknown");
        result.put("field", field != null ? field : "unknown");
        result.put("university", universityName != null ? universityName : "unknown");
        return result;
    }






}
