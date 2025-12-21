package org.acme.md;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.ai.TableDescriptor;

@ApplicationScoped
public class TableRowProcessor {

    @Inject
    TableDescriptor tableDescriptor;

    public String summarize(String description, String headers, String row) {
        return tableDescriptor.summarizeTable(description, headers, row);
    }

}
