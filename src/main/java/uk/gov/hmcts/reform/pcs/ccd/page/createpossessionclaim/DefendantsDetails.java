package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefendantsDetails implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("defendantsDetails", this::midEvent)
            .showCondition("addAnotherDefendant=\"Yes\" OR addAnotherDefendant=\"\"")
            .readonly(PCSCase::getCurrentDefendantNumber, NEVER_SHOW)
            .pageLabel("Defendant ${currentDefendantNumber} details")
            .mandatory(PCSCase::getDefendant1);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Variables are initialized by the previous page (ResumeClaim)

        // If current defendant has data, add it to the defendants list
        if (hasMeaningfulDefendantDetails(caseData.getDefendant1())) {
            DefendantDetails current = caseData.getDefendant1();
            
            // Set correspondence address if it's the same as possession address
            if (VerticalYesNo.YES == current.getAddressSameAsPossession()) {
                current.setCorrespondenceAddress(caseData.getPropertyAddress());
            }

            // Initialize defendants list if needed
            if (caseData.getDefendants() == null) {
                caseData.setDefendants(new ArrayList<>());
            }

            // Add to list if not already there
            if (!caseData.getDefendants().stream()
                .anyMatch(listValue -> listValue.getValue().equals(current))) {
                caseData.getDefendants().add(new ListValue<>(UUID.randomUUID().toString(), current));
            }
        }

        // Generate the HTML table from the defendants list
        String tableHtml = buildDefendantsTableHtml(caseData.getDefendants());
        caseData.setDefendantsTableHtml(tableHtml);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    private boolean hasMeaningfulDefendantDetails(DefendantDetails d) {
        if (d == null) {
            return false;
        }
        
        // Check if defendant has meaningful data
        return (d.getNameKnown() != null || d.getAddressKnown() != null || d.getEmailKnown() != null)
            || (d.getFirstName() != null && !d.getFirstName().isBlank())
            || (d.getLastName() != null && !d.getLastName().isBlank())
            || (d.getEmail() != null && !d.getEmail().isBlank())
            || d.getCorrespondenceAddress() != null;
    }

    private String buildDefendantsTableHtml(List<ListValue<DefendantDetails>> defendants) {
        if (defendants == null || defendants.isEmpty()) {
            return """
                <table class="govuk-table">
                <caption class="govuk-table__caption govuk-table__caption--m">Defendants</caption>
                <thead class="govuk-table__head">
                <tr class="govuk-table__row">
                <th scope="col" class="govuk-table__header">Defendant</th>
                <th scope="col" class="govuk-table__header">Defendant name</th>
                <th scope="col" class="govuk-table__header">Defendant correspondence address</th>
                <th scope="col" class="govuk-table__header">Defendant email address</th>
                </tr>
                </thead>
                <tbody class="govuk-table__body">
                <tr class="govuk-table__row">
                <td class="govuk-table__cell" colspan="4">No defendants added yet</td>
                </tr>
                </tbody>
                </table>
                """;
        }

        StringBuilder html = new StringBuilder();
        html.append("""
            <table class="govuk-table">
            <caption class="govuk-table__caption govuk-table__caption--m">Defendants</caption>
            <thead class="govuk-table__head">
            <tr class="govuk-table__row">
            <th scope="col" class="govuk-table__header">Defendant</th>
            <th scope="col" class="govuk-table__header">Defendant name</th>
            <th scope="col" class="govuk-table__header">Defendant correspondence address</th>
            <th scope="col" class="govuk-table__header">Defendant email address</th>
            </tr>
            </thead>
            <tbody class="govuk-table__body">
            """);

        for (int i = 0; i < defendants.size(); i++) {
            ListValue<DefendantDetails> listValue = defendants.get(i);
            DefendantDetails defendant = listValue.getValue();
            
            html.append("<tr class=\"govuk-table__row\">");
            html.append("<td class=\"govuk-table__cell\">Defendant ").append(i + 1).append("</td>");
            
            // Name
            String name = "";
            if (defendant.getFirstName() != null && !defendant.getFirstName().isBlank()) {
                name += defendant.getFirstName();
            }
            if (defendant.getLastName() != null && !defendant.getLastName().isBlank()) {
                if (!name.isEmpty()) name += " ";
                name += defendant.getLastName();
            }
            html.append("<td class=\"govuk-table__cell\">").append(escapeHtml(name)).append("</td>");
            
            // Address
            String address = "";
            if (defendant.getCorrespondenceAddress() != null) {
                if (defendant.getCorrespondenceAddress().getAddressLine1() != null) {
                    address += defendant.getCorrespondenceAddress().getAddressLine1();
                }
                if (defendant.getCorrespondenceAddress().getPostTown() != null) {
                    if (!address.isEmpty()) address += ", ";
                    address += defendant.getCorrespondenceAddress().getPostTown();
                }
                if (defendant.getCorrespondenceAddress().getPostCode() != null) {
                    if (!address.isEmpty()) address += ", ";
                    address += defendant.getCorrespondenceAddress().getPostCode();
                }
            }
            html.append("<td class=\"govuk-table__cell\">").append(escapeHtml(address)).append("</td>");
            
            // Email
            String email = defendant.getEmail() != null ? defendant.getEmail() : "";
            html.append("<td class=\"govuk-table__cell\">").append(escapeHtml(email)).append("</td>");
            
            html.append("</tr>");
        }

        html.append("""
            </tbody>
            </table>
            """);
        
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
