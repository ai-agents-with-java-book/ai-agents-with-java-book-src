package org.acme;


import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.acme.docling.BoardingPassScanner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class BoardingPassScannerTest {

    @Inject
    BoardingPassScanner boardingPassScanner;

    @Test
    public void shouldReadBookingId() throws IOException {

        try(InputStream boardingPass = BoardingPassScannerTest.class.getResourceAsStream("/boarding-pass-11.pdf")) {
            String s = Base64.getEncoder().encodeToString(boardingPass.readAllBytes());
            Long id = boardingPassScanner.scanBookingId(s);

           assertThat(id).isEqualTo(11);
        }

    }

}
