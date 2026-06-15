package uk.gov.hmcts.reform.pcs.ccd.generator;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.generator.ConfigGenerator;
import uk.gov.hmcts.ccd.sdk.generator.JsonUtils;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

/**
 * The CCD SDK emits History tab rows for read-only judicial roles, but does not
 * emit the matching caseHistory field authorisation needed by EXUI.
 */
@Order
@Component
public class JudicialHistoryAuthorisationGenerator implements ConfigGenerator<PCSCase, State, AccessProfile> {

    private static final String CASE_HISTORY_FIELD = "caseHistory";
    private static final List<AccessProfile> JUDICIAL_HISTORY_ROLES = List.of(
        AccessProfile.CIRCUIT_JUDGE,
        AccessProfile.FEE_PAID_JUDGE,
        AccessProfile.JUDGE,
        AccessProfile.LEADERSHIP_JUDGE
    );

    @Override
    public void write(File outputFolder, ResolvedCCDConfig<PCSCase, State, AccessProfile> config) {
        File authorisationCaseFieldDirectory = new File(outputFolder, "AuthorisationCaseField");
        authorisationCaseFieldDirectory.mkdirs();

        for (AccessProfile role : JUDICIAL_HISTORY_ROLES) {
            Path output = Path.of(authorisationCaseFieldDirectory.getPath(), role.getRole() + ".json");

            JsonUtils.mergeInto(
                output,
                List.of(caseHistoryAuthorisation(config.getCaseType(), role)),
                new JsonUtils.AddMissing(),
                "CaseFieldID",
                "UserRole"
            );
        }
    }

    private Map<String, Object> caseHistoryAuthorisation(String caseTypeId, AccessProfile role) {
        Map<String, Object> authorisation = JsonUtils.caseRow(caseTypeId);
        authorisation.put("CaseFieldID", CASE_HISTORY_FIELD);
        authorisation.put("CRUD", Permission.toString(Permission.CRU));
        authorisation.put("UserRole", role.getRole());
        return authorisation;
    }
}
