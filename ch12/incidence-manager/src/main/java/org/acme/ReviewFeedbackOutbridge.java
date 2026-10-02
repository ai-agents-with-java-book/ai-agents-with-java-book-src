package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class ReviewFeedbackOutbridge {

    private static final JsonFormat CE_JSON = (JsonFormat) EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE);

    @Inject
    Logger logger;

    // match the type emitted by our workflow: "org.acme.email.review.required"
    private static final String REVIEW_REQUIRED_TYPE = "org.acme.email.review.required";

    public String onFlowOut(byte[] record) {
        try {
            CloudEvent ce = CE_JSON.deserialize(record);
            if (ce == null || ce.getType() == null) {
                logger.errorf("Not a cloud event %s", ce);
                return "";
            }

            if (REVIEW_REQUIRED_TYPE.equals(ce.getType())) {
                byte[] data = ce.getData() != null ? ce.getData().toBytes() : null;
                // If there's no data, send a minimal envelope so the UI can handle it.
                String json = (data == null || data.length == 0)
                    ? "{\"type\":\"" + REVIEW_REQUIRED_TYPE + "\",\"payload\":null}"
                    : new String(data, StandardCharsets.UTF_8);

                logger.infof("OUTBRIDGE>>> Received review (workflow instance: %s) required event: %s",
                    ce.getExtension("flowinstanceid"), json);

                return json;
            }
            logger.infof("Cloud Event Type %s is not %s", ce.getType(), REVIEW_REQUIRED_TYPE);
            return "";
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.errorf("Failed to consume event %s", new String(record), ex);
            throw new IllegalStateException(ex);
        }
    }

}
