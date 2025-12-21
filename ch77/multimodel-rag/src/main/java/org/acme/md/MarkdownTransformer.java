package org.acme.md;


import jakarta.inject.Singleton;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

import java.util.List;

@Singleton
public class MarkdownTransformer {

    ImageProcessor imageProcessor;
    TableRowProcessor tableRowProcessor;

    public MarkdownTransformer(ImageProcessor imageProcessor, TableRowProcessor tableRowProcessor) {
        this.imageProcessor = imageProcessor;
        this.tableRowProcessor = tableRowProcessor;
    }

    public List<Content> parse(String markdownContent) {

        MarkdownMultimodalVisitor markdownMultimodalVisitor =
                new MarkdownMultimodalVisitor(imageProcessor, tableRowProcessor);

        var parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build();

        Node node = parser.parse(markdownContent);
        node.accept(markdownMultimodalVisitor);

        return markdownMultimodalVisitor.parsedContent;

    }

}
