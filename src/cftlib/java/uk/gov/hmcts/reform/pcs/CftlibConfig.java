package uk.gov.hmcts.reform.pcs;

import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toCollection;

/**
 * Configures the CFTLib with the required users, roles and CCD definitions.
 * The Cftlib will find and execute this configuration class once all services are ready.
 */
@Component
public class CftlibConfig implements CFTLibConfigurer {

    private final CCDDefinitionGenerator configWriter;

    public CftlibConfig(@Lazy CCDDefinitionGenerator configWriter) {
        this.configWriter = configWriter;
    }

    @Override
    public void configure(CFTLib lib) throws Exception {

        var users = Map.of(
            "caseworker@pcs.com", List.of("caseworker", "caseworker-pcs"),
            "housing-provider@pcs.com", List.of("caseworker", "caseworker-pcs", "housing-provider"),
            "citizen@pcs.com", List.of("citizen"),
            "data.store.idam.system.user@gmail.com", List.of()
        );

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "PCS", "PCS", State.CASE_ISSUED.name());
        }

        createAccessProfiles(lib);
        createRoleAssignments(lib);

        // Generate CCD definitions
        configWriter.generateAllCaseTypesToJSON(new File("build/definitions"));

        // Import CCD definitions
        lib.importJsonDefinition(new File("build/definitions/" + CaseType.getCaseType()));
    }

    private void createAccessProfiles(CFTLib lib) {
        List<String> roleNames = Arrays.stream(UserRole.values())
            .map(UserRole::getRole)
            .collect(toCollection(ArrayList::new));

        roleNames.add("caseworker");

        lib.createRoles(roleNames.toArray(new String[0]));
    }

    private void createRoleAssignments(CFTLib lib) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                        .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);
    }

}
