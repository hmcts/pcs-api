package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.ResolvedCCDConfig;
import uk.gov.hmcts.ccd.sdk.generator.ConfigGenerator;
import uk.gov.hmcts.ccd.sdk.generator.JsonUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.NoticeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.RequiredDocumentsTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.TenancyLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.OccupationContractOrLicenceTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.RentArrearsTabDetails;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.ccd.sdk.FieldUtils.getCaseFields;
import static uk.gov.hmcts.ccd.sdk.FieldUtils.getFieldId;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DocumentMoveRoles.DOCUMENT_MOVE_ROLES;

@Component
public class DocumentMoveComplexAuthorisation implements ConfigGenerator<PCSCase, State, UserRole> {

    @Override
    public void write(File outputFolder, ResolvedCCDConfig<PCSCase, State, UserRole> config) {
        JsonUtils.mergeInto(outputFolder.toPath().resolve("AuthorisationComplexType.json"),
                            buildRows(config.getCaseType()), new JsonUtils.AddMissing(),
                            "CaseTypeID", "CaseFieldID", "ListElementCode", "UserRole");
    }

    List<Map<String,Object>> buildRows(String caseType) {
        List<Map<String,Object>> rows = new ArrayList<>();
        addFields(rows, caseType, "detailsTab_TenancyLicenceDetails",
                  TenancyLicenceTabDetails.class,
                  Set.of("tenancyLicenceDocuments"));
        addFields(rows, caseType, "detailsTab_NoticeDetails",
                  NoticeTabDetails.class,
                  Set.of("noticeDocuments"));
        addFields(rows, caseType, "detailsTab_RentArrearsDetails",
                  RentArrearsTabDetails.class,
                  Set.of("rentStatement"));
        addFields(rows, caseType, "detailsTab_OccupationContractLicenceDetails",
                  OccupationContractOrLicenceTabDetails.class,
                  Set.of("documents"));
        addFields(rows, caseType, "detailsTab_RequiredDocumentsDetails",
                  RequiredDocumentsTabDetails.class,
                  Set.of("energyPerformanceCertificates", "gasSafetyReports", "electricalInstallationReports"));
        return rows;
    }

    private void addFields(List<Map<String, Object>> rows, String caseType, String root,
                           Class<?> type, Set<String> documentFields) {
        for (var field : getCaseFields(type)) {
            String fieldId = getFieldId(field, null);
            for (UserRole role : DOCUMENT_MOVE_ROLES) {
                Map<String, Object> row = JsonUtils.caseRow(caseType);
                row.put("CaseFieldID", root);
                row.put("ListElementCode", fieldId);
                row.put("UserRole", role.getRole());
                row.put("CRUD", documentFields.contains(fieldId) ? "RU" : "R");
                rows.add(row);
            }
        }
    }
}
