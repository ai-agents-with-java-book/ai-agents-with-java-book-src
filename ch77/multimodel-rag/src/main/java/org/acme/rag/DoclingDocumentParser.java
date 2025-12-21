package org.acme.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;

@Singleton
public class DoclingDocumentParser implements DocumentParser {

    @Inject
    DoclingReader doclingReader;

    @Override
    public Document parse(InputStream inputStream) {
        // Only PDFs is supported.
        try {
            String markdown = doclingReader.convertToMarkdown(inputStream.readAllBytes(), "doc.pdf");
            return Document.document(markdown, Metadata.from("render", "docling"));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
