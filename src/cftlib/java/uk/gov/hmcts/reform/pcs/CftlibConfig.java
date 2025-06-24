package uk.gov.hmcts.reform.pcs;

import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.reform.pcs.ccd.PCSCaseType;
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
@Import(PCSCaseType.class)
@Component
public class CftlibConfig implements CFTLibConfigurer {

    private final CCDDefinitionGenerator configWriter;

    public CftlibConfig(@Lazy CCDDefinitionGenerator configWriter) {
        this.configWriter = configWriter;
    }

    @Override
    public void configure(CFTLib lib) throws Exception {
        var users = Map.of(
            "caseworker@pcs.com", List.of("caseworker", "caseworker-pcs"));

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "PCS", "PCS", State.Open.name());
        }

        // Create local system user
        lib.createIdamUser("pcs-system-user@localhost");

        lib.createRoles(
            "caseworker",
            "caseworker-pcs"
        );

        // Generate CCD definitions
        configWriter.generateAllCaseTypesToJSON(new File("build/definitions"));

        // Import CCD definitions
        lib.importJsonDefinition(new File("build/definitions/" + PCSCaseType.getCaseType()));
        //lib.importJsonDefinition(new File("build/definitions/GENERALAPPLICATION"));
    }
}
