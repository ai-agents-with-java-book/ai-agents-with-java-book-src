package org.acme.docling;

import ai.docling.core.DoclingDocument;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;
import ai.docling.serve.api.convert.request.options.ImageRefMode;
import ai.docling.serve.api.convert.request.options.OutputFormat;
import ai.docling.serve.api.convert.request.source.FileSource;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;
import io.quarkiverse.docling.runtime.client.DoclingService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BoardingPassScanner {

    @Inject
    DoclingServeApi doclingServeApi;

    static final int bookingIdB = 185;
    static final int bookingIdL = 686;
    static final int bookingIdR = 712;
    static final int bookingIdT = 207;

    static final ConvertDocumentOptions options = ConvertDocumentOptions.builder()
        .imageExportMode(ImageRefMode.REFERENCED)
        .toFormat(OutputFormat.JSON)
        .build();

    public Long scanBookingId(String boardingPassBase64) {

        var fileSource = FileSource.builder()
            .filename("boardingpass.pdf")
            .base64String(boardingPassBase64)
            .build();

        var conversionRequest = ConvertDocumentRequest.builder()
            .source(fileSource)
            .options(options)
            .build();

        InBodyConvertDocumentResponse response = (InBodyConvertDocumentResponse) doclingServeApi.convertSource(conversionRequest);

        List<DoclingDocument.BaseTextItem> texts = response.getDocument().getJsonContent().getTexts();

        Optional<DoclingDocument.BaseTextItem> bookingIdElement = texts.stream()
            .filter(this::isInPosition)
            .findFirst();

        String id = bookingIdElement
            .map(DoclingDocument.BaseTextItem::getText)
            .orElse("-1");

        return Long.parseLong(id);
    }

    private boolean isInPosition(DoclingDocument.BaseTextItem baseTextItem) {
        for(DoclingDocument.ProvenanceItem prov : baseTextItem.getProv()) {
            int b = prov.getBbox().getB().intValue();
            int l = prov.getBbox().getL().intValue();
            int r = prov.getBbox().getR().intValue();
            int t = prov.getBbox().getT().intValue();

            if (b == bookingIdB && l == bookingIdL && r == bookingIdR && t == bookingIdT) {
                return true;
            }
        }

        return false;
    }

}
