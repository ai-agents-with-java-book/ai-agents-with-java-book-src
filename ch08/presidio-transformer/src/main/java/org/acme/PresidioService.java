package org.acme;

import io.quarkiverse.presidio.runtime.Analyzer;
import io.quarkiverse.presidio.runtime.Anonymizer;
import io.quarkiverse.presidio.runtime.model.AnalyzeRequest;
import io.quarkiverse.presidio.runtime.model.AnonymizeRequest;
import io.quarkiverse.presidio.runtime.model.AnonymizeResponse;
import io.quarkiverse.presidio.runtime.model.Mask;
import io.quarkiverse.presidio.runtime.model.RecognizerResultWithAnaysisExplanation;
import io.quarkiverse.presidio.runtime.model.Redact;
import io.quarkiverse.presidio.runtime.model.Replace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PresidioService {

    @RestClient
    Analyzer analyzer;

    @RestClient
    Anonymizer anonymizer;

    @Inject
    Logger logger;

    public String process(String text) {
        final List<RecognizerResultWithAnaysisExplanation> analyzed = analyze(text, "en");
        System.out.println(analyzed);
        logger.info("**************** ANALYZE ****************************");
        logger.info(enrichForLogging(text, analyzed));
        logger.info("*****************************************************");
        String anonymized= anonymize(text, analyzed).getText();
        logger.info("**************** ANONYMIZED ****************************");
        logger.info(anonymized);
        logger.info("********************************************************");
        return anonymized;
    }

    private List<RecognizerResultWithAnaysisExplanation> analyze(String text, String language) {
        AnalyzeRequest analyzeRequest = new AnalyzeRequest();
        analyzeRequest.text(text);
        analyzeRequest.language(language);
        analyzeRequest.setEntities(Arrays.asList("PHONE_NUMBER", "EMAIL_ADDRESS", "PERSON"));
        return analyzer
            .analyzePost(analyzeRequest);
    }

    static Replace REPLACE = new Replace("ANONYMIZED");
    static Mask MASK = new Mask("*", 4, true);
    static Redact REDACT = new Redact();

    private AnonymizeResponse anonymize(String text, List<RecognizerResultWithAnaysisExplanation> recognizerResults) {

        AnonymizeRequest anonymizeRequest = new AnonymizeRequest();

        anonymizeRequest.setText(text);

        anonymizeRequest.putAnonymizersItem("DEFAULT", REPLACE);
        anonymizeRequest.putAnonymizersItem("PHONE_NUMBER", MASK);
        anonymizeRequest.putAnonymizersItem("EMAIL_ADDRESS", REDACT);
        anonymizeRequest.analyzerResults(
            Collections.unmodifiableList(recognizerResults));

        return this.anonymizer.anonymizePost(anonymizeRequest);
    }

    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    private static String enrichForLogging(String text, List<RecognizerResultWithAnaysisExplanation> recognizerResultWithAnaysisExplanations) {
        List<int[]> indexes = recognizerResultWithAnaysisExplanations
                                            .stream()
                                            .map(r -> new int[] {
                                                r.getStart(), r.getEnd()
                                            })
                                            .collect(Collectors.toCollection(ArrayList::new));
        return formatHighlighted(text, indexes);
    }

    private static String formatHighlighted(String text, List<int[]> ranges) {

        StringBuilder output = new StringBuilder();
        output.append(RESET);
        
        int currentIndex = 0;

        ranges.sort((a, b) -> Integer.compare(a[0], b[0]));

        for (int[] range : ranges) {
            int start = range[0];
            int end = range[1];

            // Append text before the highlight
            if (start > currentIndex) {
                output.append(text, currentIndex, start);
            }

            // Append highlighted text
            output.append(RED).append(text, start, end).append(RESET);

            // Move current index forward
            currentIndex = end;
        }

        // Append any remaining text after the last highlight
        if (currentIndex < text.length()) {
            output.append(text.substring(currentIndex));
        }

        // Print the final output with color highlights
        return output.toString();
    }
}