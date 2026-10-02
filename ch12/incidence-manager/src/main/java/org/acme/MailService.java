package org.acme;


import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.IOException;

@Singleton
public class MailService {

    @Inject
    Mailer mailer;

    @Inject
    Logger logger;

    byte[] logo;

    @PostConstruct
    public void readLogo() throws IOException {
        logo = MailService.class.getResourceAsStream("/logogalaxy.png").readAllBytes();
    }

    @CheckedTemplate
    static class Templates {
        public static native MailTemplate.MailTemplateInstance feedback(CustomerSupportInformation customerSupportInformation);
    }

    public void sendEmail(CustomerSupportInformation customerSupportInformation) {
        logger.info("Support Email Preparation");

        Templates.feedback(customerSupportInformation)
            .from("galaxy@example.com")
            .to(customerSupportInformation.userEmail())
            .subject(customerSupportInformation.subject())
            .addInlineAttachment("logo.png", logo, "image/png", "<logo@quarkus.io>")
            .sendAndAwait();

        logger.info("Support Email sent");
    }

}
