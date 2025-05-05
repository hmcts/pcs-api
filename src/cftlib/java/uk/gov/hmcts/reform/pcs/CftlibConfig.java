package uk.gov.hmcts.reform.pcs;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CCDDefinitionGenerator;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLib;
import uk.gov.hmcts.rse.ccd.lib.api.CFTLibConfigurer;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

/**
 * Configures the CFTLib with the required users, roles and CCD definitions.
 * The Cftlib will find and execute this configuration class once all services are ready.
 */
@Component
public class CftlibConfig implements CFTLibConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CftlibConfig.class);

    @Autowired
    @Lazy
    CCDDefinitionGenerator configWriter;

    @Override
    public void configure(CFTLib lib) throws Exception {
        var users = Map.of(
            "case-allocator@pcs.com", List.of(),
            "caseworker@pcs.com", List.of("caseworker", "caseworker-civil"),
            "housing-association@pcs.com", List.of("caseworker", "caseworker-civil"),
            "applicant1@pcs.com", List.of("caseworker", "caseworker-civil"),
            "respondent1@pcs.com", List.of("caseworker", "caseworker-civil"),
            "judge@pcs.com", List.of("caseworker", "caseworker-civil"),
            "not-in-ras@pcs.com", List.of("caseworker", "caseworker-civil")
        );

        /*
            #### User ID for applicant1@pcs.com will be 68489173-2079-3f0a-9d7d-89bf7f29e52b
            #### User ID for case-allocator@pcs.com will be a6a64c98-bd48-329b-858c-e9c41949f937
            #### User ID for housing-association@pcs.com will be 8ebad581-46bf-3c78-96da-77e0445a5695
            #### User ID for caseworker@pcs.com will be 74e702fa-e20f-3a40-bc1d-d915f0874d00
            #### User ID for respondent1@pcs.com will be 9d47b501-d853-3662-9233-9ec9fac76622
            #### User ID for judge@pcs.com will be 749ce9f7-535a-3cf5-ba07-f66e6d55c5fa
            #### User ID for not-in-ras@pcs.com will be 568d1afb-f6fa-3da2-be42-51b2487b26a9
         */

        // Create users and roles including in idam simulator
        for (var entry : users.entrySet()) {
            lib.createIdamUser(entry.getKey(), entry.getValue().toArray(new String[0]));
            lib.createProfile(entry.getKey(), "CIVIL", "PCS", State.Open.name());
        }

        List<String> roleNames = Arrays.stream(UserRole.values())
            .map(UserRole::getRole)
            .collect(toCollection(ArrayList::new));

        roleNames.add("caseworker");


        lib.createRoles(roleNames.toArray(new String[0]));

        users.keySet()
            .forEach(
                userEmail -> {
                    String idamSimulatorUserId = UUID.nameUUIDFromBytes(userEmail.getBytes()).toString();
                    logger.info("User ID for {} will be {}", userEmail, idamSimulatorUserId);
                    System.out.printf("#### User ID for %s will be %s%n", userEmail, idamSimulatorUserId);
                }
            );

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        var json = IOUtils.toString(resourceLoader.getResource("classpath:cftlib-am-role-assignments.json")
                                        .getInputStream(), Charset.defaultCharset());
        lib.configureRoleAssignments(json);

        // Generate CCD definitions
        configWriter.generateAllCaseTypesToJSON(new File("build/definitions"));

        // Import CCD definitions
        lib.importJsonDefinition(new File("build/definitions/PCS"));
    }
}
