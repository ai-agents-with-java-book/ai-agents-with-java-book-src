package org.acme.rag;


import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import io.quarkiverse.docling.runtime.client.DoclingService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Path;

@Singleton
public class DoclingReader {

    @Inject
    DoclingService doclingService;

    public String convertToMarkdown(byte[] content, String filename) {
        ConvertDocumentResponse convertDocumentResponse = doclingService.convertFromBytes(content, filename, OutputFormat.MARKDOWN);
        return convertDocumentResponse.getDocument().getMarkdownContent();
    }

    public String convertToMarkdown(Path file) throws IOException {
        ConvertDocumentResponse convertDocumentResponse = doclingService.convertFile(file, OutputFormat.MARKDOWN);
        return convertDocumentResponse.getDocument().getMarkdownContent();
    }

}
