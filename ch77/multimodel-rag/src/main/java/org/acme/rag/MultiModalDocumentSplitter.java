package org.acme.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.acme.md.Content;
import org.acme.md.MarkdownTransformer;
import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class MultiModalDocumentSplitter implements DocumentSplitter  {

    private static DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(100, 0);

    @Inject
    Logger logger;

    @Inject
    MarkdownTransformer markdownParser;

    @Override
    public List<TextSegment> split(Document document) {
        String content = document.text();
        List<Content> parsedContent = this.markdownParser.parse(content);

        return parsedContent.stream()
                .flatMap(c -> this.process(c, document.metadata()).stream())
                .toList();
    }

    private List<TextSegment> process(Content content, Metadata metadata) {
        return switch (content.contentType) {
            case TEXT -> this.processText(content, metadata);
            case TABLE -> this.processTable(content, metadata);
            case IMAGE -> this.processImage(content, metadata);
        };
    }

    private List<TextSegment> processImage(Content content, Metadata metadata) {
        String[] splits = splitter.split(content.text);
        List<TextSegment> imageDescriptionSegments = new ArrayList<>();
        for (String split : splits) {
            Map<String, ? extends Serializable> meta = Map.of(
                    "kind", "image",
                    "section", content.sectionPath,
                    "line", content.line,
                    "original", content.originalContent
            );
            Metadata imageMetadata = Metadata
                    .from(meta).merge(metadata);

            logger.infof(">> Processing Image with %s", split);

            imageDescriptionSegments.add(TextSegment.textSegment(split, imageMetadata));
        }

        return imageDescriptionSegments;
    }

    private List<TextSegment> processTable(Content content, Metadata metadata) {
        Map<String, ? extends Serializable> meta = Map.of(
                "kind", "table",
                "section", content.sectionPath,
                "line", content.line,
                "original", content.originalContent
        );
        Metadata tableMetadata = Metadata
                .from(meta).merge(metadata);

        logger.infof(">> Processing Table with %s", content.text);

        return List.of(TextSegment.textSegment(content.text, tableMetadata));
    }

    private List<TextSegment> processText(Content content, Metadata metadata) {
        String[] splits = splitter.split(content.text);
        List<TextSegment> textSegments = new ArrayList<>();
        for (String split : splits) {

            Map<String, ? extends Serializable> meta = Map.of(
                    "kind", "text",
                    "section", content.sectionPath,
                    "line", content.line
            );
            Metadata textMetadata = Metadata
                                        .from(meta).merge(metadata);
            logger.infof(">> Processign Text with %s", split);
            textSegments.add(TextSegment.textSegment(split, textMetadata));
        }
        return textSegments;
    }
}
