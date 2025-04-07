package uk.gov.hmcts.reform.pcs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Configures the CFTLib with the required users, roles and CCD definitions.
 * The Cftlib will find and execute this configuration class once all services are ready.
 */
@Component
public class CftlibConfig implements CFTLibConfigurer {
    @Autowired
    @Lazy
    CCDDefinitionGenerator configWriter;

    @Override
    public void configure(CFTLib lib) {
        var users = Map.of(
            "caseworker@pcs.com", List.of("caseworker", "caseworker-civil", "creator"),
            "caseworker2@pcs.com", List.of("caseworker", "caseworker-civil", "creator-no-read"),
            "caseworker3@pcs.com", List.of("caseworker", "caseworker-civil", "updater"),
            "readonly@pcs.com", List.of("caseworker", "caseworker-civil", "reader"),
            "solicitor@interestedparty.com", List.of("caseworker", "caseworker-civil", "interested-party")
        );

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "CIVIL", "PCS", State.Open.name());
        }

        lib.createRoles(
            "caseworker",
            "caseworker-civil",
            "caseworker-other",
            "judiciary",
            "interested-party",
            "creator",
            "creator-no-read",
            "updater",
            "reader"
        );

        // Generate CCD definitions
        configWriter.generateAllCaseTypesToJSON(new File("build/definitions"));

        // Import CCD definitions
        lib.importJsonDefinition(new File("build/definitions/PCS"));
    }
}
