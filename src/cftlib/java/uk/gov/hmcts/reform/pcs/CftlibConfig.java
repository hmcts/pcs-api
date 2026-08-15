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
import java.util.HashMap;
import java.util.List;

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

        var users = new HashMap<String, List<String>>();
        users.put("caseworker@pcs.com", List.of("caseworker", "caseworker-pcs"));
        users.put("pcs-hearing-centre-team-leader-01@localhost", List.of("caseworker", "caseworker-pcs"));
        users.put("pcs.solicitor.org1@hmcts.net", List.of("caseworker", "caseworker-pcs-solicitor"));
        users.put("pcs-defendant-solicitor@test.com", List.of("pui-case-manager", "caseworker",
                                                              "caseworker-pcs-solicitor",  "caseworker-pcs"));
        users.put("citizen@pcs.com", List.of("citizen"));
        users.put("data.store.idam.system.user@gmail.com", List.of());
        users.put("ccd.import@pcs.com", List.of("ccd-import"));
        users.put("pcs-system-user@localhost",
                  List.of("caseworker", "caseworker-pcs", "ccd-import", "pcs-system-update"));
        users.put("wa-system-user@localhost",
                  List.of("caseworker", "caseworker-civil", "caseworker-wa-task-configuration"));
        users.put("exui-system-user@localhost", List.of("caseworker", "caseworker-pcs", "ccd-import"));
        users.put("prd-admin-user@localhost", List.of());
        users.put("master.caa@gmail.com", List.of("caseworker-caa"));

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "CIVIL", "PCS", State.CASE_ISSUED.name());
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
        roleNames.add("caseworker-ras-validation");
        roleNames.add("GS_profile");
        roleNames.add("caseworker-wa-task-configuration");
        roleNames.add("pui-case-manager");
        roleNames.add("caseworker-caa");

        lib.createRoles(roleNames.toArray(new String[0]));
    }

    private void createRoleAssignments(CFTLib lib) throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                        .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);
    }

}
