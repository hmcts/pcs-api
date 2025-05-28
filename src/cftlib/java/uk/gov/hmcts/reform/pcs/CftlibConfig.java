package uk.gov.hmcts.reform.pcs;

import io.micrometer.core.instrument.util.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
            "caseworker@pcs.com", List.of("caseworker", "caseworker-civil"),
            "applicant-solicitor@pcs.com", List.of("caseworker"),
            "respondent-solicitor@pcs.com", List.of("caseworker"),
            "judge@pcs.com", List.of("caseworker")
        );

        /*
            #### User ID for caseworker@pcs.com will be 74e702fa-e20f-3a40-bc1d-d915f0874d00
            #### User ID for applicant-solicitor@pcs.com will be 4751c408-6d6c-334c-acb7-cba80f76374c
            #### User ID for respondent-solicitor@pcs.com will be 0a7b40c5-c450-3f1e-84e5-2f99cef415ce
            #### User ID for judge@pcs.com will be 749ce9f7-535a-3cf5-ba07-f66e6d55c5fa
         */

        dumpUserIds(users.keySet());

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "CIVIL", "PCS", State.Open.name());
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

    private void dumpUserIds(Set<String> userEmails) {
        userEmails
            .forEach(
                userEmail -> {
                    String idamSimulatorUserId = UUID.nameUUIDFromBytes(userEmail.getBytes()).toString();
                    System.out.printf("#### User ID for %s will be %s%n", userEmail, idamSimulatorUserId);
                }
        );
    }

}
