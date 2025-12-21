package org.acme;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.rag.DoclingReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@QuarkusTest
public class DoclingReaderTest {

    @Inject
    DoclingReader doclingReader;

    @Test
    public void shouldConvertPDF() throws IOException {
        Path path = Paths.get("src/test/resources/borns.pdf");
        String converted = doclingReader.convertToMarkdown(path);
        Assertions.assertThat(converted).isNotEmpty();
    }

}
