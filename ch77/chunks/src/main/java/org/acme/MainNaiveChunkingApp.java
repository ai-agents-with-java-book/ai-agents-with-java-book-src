package org.acme;

import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

public class MainNaiveChunkingApp {


    static String TEXT = """
            Freddie Mercury was a British singer and songwriter who achieved global fame as the lead vocalist and pianist of the rock band Queen. 
            Regarded as one of the greatest singers in the history of rock music, he was known for his flamboyant stage persona and four-octave vocal range defying the conventions of a rock frontman with his theatrical style, influencing the artistic direction of Queen.
            """;
    public static void main(String[] args) {
        DocumentSplitter splitter = DocumentSplitters.recursive(134, 20);
        
        Document doc = Document.from(TEXT);
        List<TextSegment> chunks = splitter.split(doc);
        chunks.stream().map(t -> t.text()).forEach(t -> System.out.println("* " + t));
    }
}
