package uk.gov.hmcts.reform.pcs;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toCollection;

/**
 * Configures the CFTLib with the required users, roles and CCD definitions.
 * The Cftlib will find and execute this configuration class once all services are ready.
 */
@Component
public class CftlibConfig implements CFTLibConfigurer {

    private static final String BAILIFF = "sam.jones@justice.gov.uk";
    private static final String BAILIFF_ADMIN = "pcs-bailiff-admin@localhost";
    private static final String BAILIFF_MANAGER = "pcs-bailiff-manager@localhost";

    /**
     * Forename and surname of the staff users that staff reference data lists, keyed by email.
     */
    private static final Map<String, String[]> STAFF_NAMES = Map.of(
        BAILIFF, new String[] {"Sam", "Jones"},
        BAILIFF_ADMIN, new String[] {"Ada", "BailiffAdmin"},
        BAILIFF_MANAGER, new String[] {"Mark", "BailiffManager"}
    );

    private final CCDDefinitionGenerator configWriter;

    public CftlibConfig(@Lazy CCDDefinitionGenerator configWriter) {
        this.configWriter = configWriter;
    }

    @Override
    public void configure(CFTLib lib) throws Exception {

        var users = new HashMap<String, List<String>>();
        users.put("caseworker@pcs.com", List.of("caseworker", "caseworker-pcs"));
        users.put("judge@pcs.com", List.of("caseworker", "caseworker-pcs"));
        users.put("pcs-hearing-centre-team-leader-01@localhost", List.of("caseworker", "caseworker-pcs"));
        users.put("pcs-solicitor1@test.com", List.of("caseworker", "caseworker-pcs-solicitor"));
        users.put("pcs-defendant-solicitor@test.com", List.of("pui-case-manager", "caseworker",
                                                              "caseworker-pcs-solicitor",  "caseworker-pcs"));
        // The e2e suite's defendant solicitor. createCaseAPI.action.ts hardcodes this address when it links
        // a defendant solicitor to a party, so the local stack has to know it by the same name as AAT.
        users.put("pcs-org1-solicitor2@test.com", List.of("pui-case-manager", "caseworker",
                                                          "caseworker-pcs-solicitor", "caseworker-pcs"));
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
        // Bailiff staff for the enforcement journeys. The bailiff roles are RAS organisation roles
        // (UserRole.BAILIFF, BAILIFF_ADMIN, BAILIFF_MANAGER), granted in cftlib-am-role-assignments.json;
        // the IDAM roles here only get them into XUI. The bailiff's hmcts-admin assignment there is scoped to
        // PCS so that XUI's StaffUser type-ahead can list them: XUI reads staff role categories only from a
        // fixed list of role names (which has hmcts-admin but not bailiff) in the staff-supported jurisdictions.
        users.put(BAILIFF, List.of("caseworker", "caseworker-pcs"));
        users.put(BAILIFF_ADMIN, List.of("caseworker", "caseworker-pcs"));
        users.put(BAILIFF_MANAGER, List.of("caseworker", "caseworker-pcs"));

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "CIVIL", "PCS", State.CASE_ISSUED.name());
        }
        nameStaffUsers(users);

        createAccessProfiles(lib);
        createRoleAssignments(lib);

        // Generate CCD definitions
        configWriter.generateAllCaseTypesToJSON(new File("build/definitions"));

        // Import CCD definitions
        lib.importJsonDefinition(new File("build/definitions/" + CaseType.getCaseType()));
    }

    /**
     * Gives the bailiff staff the names wiremock's staff reference data stub
     * (wiremock/mappings/rd-professional/get-users-by-service-name.json) lists them under. XUI's StaffUser
     * type-ahead shows the staff reference data name, while "Allocate case to bailiff" reads the name from
     * IDAM, so the two have to agree. CFTLib's createIdamUser names everyone "A User"; the simulator
     * replaces an account that is added again.
     */
    private void nameStaffUsers(Map<String, List<String>> users) throws IOException, InterruptedException {
        if (!"localAuth".equals(System.getenv("RSE_LIB_AUTH-MODE"))) {
            return;
        }
        var client = HttpClient.newHttpClient();
        var mapper = new ObjectMapper();
        for (var staff : STAFF_NAMES.entrySet()) {
            var account = Map.of(
                "email", staff.getKey(),
                "forename", staff.getValue()[0],
                "surname", staff.getValue()[1],
                "password", "password",
                "roles", users.get(staff.getKey()).stream().map(role -> Map.of("code", role)).toList()
            );
            var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:5062/testing-support/accounts"))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(account)))
                .build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("Naming IDAM user " + staff.getKey() + " failed: HTTP "
                                                    + response.statusCode());
            }
        }
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
