package org.acme.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {
    private final SupportDrafts drafts;

    public SummaryController(SupportSummary summary) {
        drafts = new SupportDrafts(summary);
    }

    @PostMapping(value = "/summaries", consumes = "text/plain", produces = "text/plain")
    public ResponseEntity<String> summarize(@RequestBody String request) {
        try {
            return ResponseEntity.ok(drafts.prepareSummary(request));
        } catch (IllegalArgumentException invalid) {
            return ResponseEntity.badRequest().body(invalid.getMessage());
        } catch (SummaryUnavailableException unavailable) {
            return ResponseEntity.status(502).body(unavailable.getMessage());
        }
    }
}
