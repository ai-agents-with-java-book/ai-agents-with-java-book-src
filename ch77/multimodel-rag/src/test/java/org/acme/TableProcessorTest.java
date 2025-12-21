package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.md.TableRowProcessor;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TableProcessorTest {

    @Inject
    TableRowProcessor tableRowProcessor;

    @Test
    void shouldProcessARow() {
        String desc = "The following table shows the top names given to newborns in 2025";
        String headers = "Name,Number,Rate per 1000";
        String row = "Marc,344,12.46";

        System.out.println(tableRowProcessor.summarize(desc, headers, row));
    }
}
