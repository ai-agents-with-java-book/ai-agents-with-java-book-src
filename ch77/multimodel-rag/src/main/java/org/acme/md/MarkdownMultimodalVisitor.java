package org.acme.md;

import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.*;

import java.util.*;

public class MarkdownMultimodalVisitor extends AbstractVisitor {

    List<Content> parsedContent = new ArrayList<>();

    public MarkdownMultimodalVisitor(ImageProcessor imageProcessor, TableRowProcessor tableRowProcessor) {
        this.imageProcessor = imageProcessor;
        this.tableRowProcessor = tableRowProcessor;
    }

    ImageProcessor imageProcessor;
    TableRowProcessor tableRowProcessor;

    // A stack to track nested headings
    private final Deque<String> sectionStack = new ArrayDeque<>();

    private String lastText;
    private boolean isHeader;
    private String tableHeaders;

    // -------------------------------------------------------------
    // HANDLE HEADINGS & SECTION CONTEXT
    // -------------------------------------------------------------
    @Override
    public void visit(Heading heading) {

        // Extract heading text
        TextCollector collector = new TextCollector();
        heading.accept(collector);
        String headingText = collector.getText().trim();

        // Adjust stack depth based on heading level
        while (sectionStack.size() >= heading.getLevel()) {
            sectionStack.removeLast();
        }
        sectionStack.addLast(headingText);

        log("Entering heading: " + headingText);

        visitChildren(heading);
    }

    // -------------------------------------------------------------
    // PARAGRAPHS & TEXT EXTRACTION
    // -------------------------------------------------------------
    @Override
    public void visit(Paragraph paragraph) {
        TextCollector collector = new TextCollector();
        paragraph.accept(collector);

        String text = collector.getText().trim();
        if (!text.isEmpty()) {
            log("Text: " + text);
            Content c = new Content();
            c.sectionPath = String.join(" > ", sectionStack);
            c.contentType = ContentType.TEXT;
            c.text = text;
            c.line = paragraph.getSourceSpans().getFirst().getLineIndex();
            this.parsedContent.add(c);

            lastText = text;
        }

        visitChildren(paragraph);
    }

    // -------------------------------------------------------------
    // IMAGE EXTRACTION
    // -------------------------------------------------------------
    @Override
    public void visit(Image image) {
        String description = image.getTitle() == null ? lastText : image.getTitle();
        String describeEmbeddedImage = this.imageProcessor.describeEmbeddedImage(description, image.getDestination());

        Content content = new Content();
        content.contentType = ContentType.IMAGE;
        content.sectionPath = String.join(" > ", sectionStack);
        content.originalContent = image.getDestination();
        content.text = description + " : " + describeEmbeddedImage;
        content.line = image.getSourceSpans().getFirst().getLineIndex();;

        this.parsedContent.add(content);

        log("Description= " + content.text + " Image: url=" + image.getDestination() + ", title=" + image.getTitle());
        visitChildren(image);
        lastText = "";
    }

    @Override
    public void visit(CustomBlock customBlock) {
        switch (customBlock) {
            case TableBlock tb -> visit(tb);
            default -> visitChildren(customBlock);
        }
    }

    @Override
    public void visit(CustomNode customNode) {
        switch (customNode) {
            case TableRow tr -> visit(tr);
            case TableHead th -> visit(th);
            case TableBody tb -> visit(tb);
            case TableCell tc -> visit(tc);
            default -> visitChildren(customNode);
        }
    }

    // -------------------------------------------------------------
    // FULL TABLE EXTRACTION
    // -------------------------------------------------------------

    private void visit(TableBlock tableBlock) {
        log("Table detected:");

        // Each TableBlock contains TableHead + TableBody nodes
        visitChildren(tableBlock);
        tableHeaders = "";
        lastText = "";
    }


    private void visit(TableHead head) {
        log("Table Header:");
        isHeader = true;
        visitChildren(head);
        isHeader = false;
    }


    private void visit(TableRow row) {
        List<String> cells = extractCells(row);
        if (isHeader) {
            tableHeaders = String.join(",", cells);
        } else {

            String summarize = this.tableRowProcessor.summarize(lastText, tableHeaders, String.join(",", cells));

            Content content = new Content();
            content.contentType = ContentType.TABLE;
            content.sectionPath = String.join(" > ", sectionStack);
            content.text = summarize;
            int lineIndex = row.getSourceSpans().getFirst().getLineIndex();
            content.line = lineIndex;
            content.originalContent = "Table Row at line " + lineIndex;

            this.parsedContent.add(content);

            log("Header: " + tableHeaders);
            log("Row: " + cells);
            log("Last: " + lastText);
        }

        visitChildren(row);
    }

    private void visit(TableBody body) {
        log("Table Body:");
        visitChildren(body);
    }

    private void visit(TableCell cell) {
        // We don’t log here because extractCells() handles cell extraction
        visitChildren(cell);
    }

    private List<String> extractCells(TableRow row) {
        List<String> cells = new ArrayList<>();

        Node node = row.getFirstChild();
        while (node != null) {
            if (node instanceof TableCell tableCell) {
                TextCollector col = new TextCollector();
                tableCell.accept(col);
                cells.add(col.getText().trim());
            }
            node = node.getNext();
        }
        return cells;
    }

    private void log(String msg) {
        // System.out.println("[Section: " + String.join(" > ", sectionStack) + "] " + msg);
    }
}

