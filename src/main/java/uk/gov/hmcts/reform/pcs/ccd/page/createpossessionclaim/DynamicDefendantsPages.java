package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import de.cronn.reflection.util.TypedPropertyGetter;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.AddressValidator;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicDefendantsPages implements CcdPageConfiguration {

    /**
     * Maximum number of defendants that can be added to a case.
     */
    private static final int MAX_NUMBER_OF_DEFENDANTS = 25;

    private final AddressValidator addressValidator;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        // Generate 25 defendant detail pages and 25 defendant list pages
        for (int i = 1; i <= MAX_NUMBER_OF_DEFENDANTS; i++) {
            final int defendantIndex = i; // Make effectively final for lambda

            // Create defendant details page (e.g., "Defendant 1 Details", "Defendant 2 Details")
            createDefendantDetailsPage(pageBuilder, i, defendantIndex);

            // Create defendant list page (e.g., "Defendant List 1", "Defendant List 2")
            createDefendantListPage(pageBuilder, i);
        }
    }

    /**
     * Creates a defendant details page for entering defendant information.
     */
    private void createDefendantDetailsPage(PageBuilder pageBuilder, int i, int defendantIndex) {
        var defendantPage = pageBuilder.page("DefendantDetails" + i, 
            (details, detailsBefore) -> midEventForDefendant(details, detailsBefore, defendantIndex));
        
        // Only show this page if previous "Add Another Defendant" was YES (except for first defendant)
        if (i > 1) {
            defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
        }
        
        defendantPage.pageLabel("Defendant " + i + " Details")
            .complex(getTempDefField(i))
                .readonly(DefendantDetails::getNameSectionLabel)
                .mandatory(DefendantDetails::getNameKnown)
                .mandatory(DefendantDetails::getFirstName)
                .mandatory(DefendantDetails::getLastName)

                .readonly(DefendantDetails::getAddressSectionLabel)
                .mandatory(DefendantDetails::getAddressKnown)
                .mandatory(DefendantDetails::getAddressSameAsPossession)
                .complex(DefendantDetails::getCorrespondenceAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .mandatory(DefendantDetails::getCorrespondenceAddress)

                .readonly(DefendantDetails::getEmailSectionLabel)
                .mandatory(DefendantDetails::getEmailKnown)
                .mandatory(DefendantDetails::getEmail)
            .done();
    }

    /**
     * Creates a defendant list page showing summary and "Add Another Defendant?" option.
     */
    private void createDefendantListPage(PageBuilder pageBuilder, int i) {
        var addAnotherPage = pageBuilder.page("DefendantList" + i);
        
        // Only show this page if previous "Add Another Defendant" was YES (except for first defendant)
        if (i > 1) {
            addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
        }
        
        addAnotherPage.pageLabel("Defendant List")
            .label("defTable" + i, buildDefendantsSummaryTable(i));
        
        // Only add "Add Another Defendant?" field if not the last defendant
        if (i != MAX_NUMBER_OF_DEFENDANTS) {
            addAnotherPage.mandatory(getAddAnotherField(i));
        }
    }

    /**
     * Builds an HTML table showing defendants 1 through upToDefendant.
     * 
     * @param upToDefendant the highest defendant number to include in the table
     * @return HTML table string for display in CCD
     */
    public String buildDefendantsSummaryTable(int upToDefendant) {
        if (upToDefendant < 1 || upToDefendant > MAX_NUMBER_OF_DEFENDANTS) {
            throw new IllegalArgumentException("Invalid defendant count: " + upToDefendant);
        }

        StringBuilder htmlTable = new StringBuilder();
        htmlTable.append("""
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

        // Generate table rows for each defendant
        for (int i = 1; i <= upToDefendant; i++) {
            htmlTable.append(buildDefendantTableRow(i));
        }

        htmlTable.append("""
              </tbody>
            </table>
                """);

        return htmlTable.toString();
    }

    /**
     * Builds a single table row for a defendant.
     * 
     * @param defendantNumber the defendant number (1-based)
     * @return HTML table row string
     */
    private String buildDefendantTableRow(int defendantNumber) {
        // Build the HTML row with defendant-specific field references
        return new StringBuilder()
                .append("<tr class=\"govuk-table__row\">")
                .append("<td class=\"govuk-table__cell\">Defendant ").append(defendantNumber).append("</td>")
                .append("<td class=\"govuk-table__cell\">${defendant").append(defendantNumber)
                    .append(".firstName} ${defendant").append(defendantNumber).append(".lastName}</td>")
                .append("<td class=\"govuk-table__cell\">")
                .append("${defendant").append(defendantNumber).append(".correspondenceAddress.AddressLine1}<br>")
                .append("${defendant").append(defendantNumber).append(".correspondenceAddress.PostTown}<br>")
                .append("${defendant").append(defendantNumber).append(".correspondenceAddress.PostCode}")
                .append("</td>")
                .append("<td class=\"govuk-table__cell\">${defendant").append(defendantNumber).append(".email}</td>")
                .append("</tr>")
                .toString();
    }

    /**
     * Gets the TypedPropertyGetter for the specified defendant field.
     * 
     * @param i the defendant index (1-25)
     * @return TypedPropertyGetter for the defendant field
     * @throws IllegalArgumentException if index is out of range
     */
    private static TypedPropertyGetter<PCSCase, DefendantDetails> getTempDefField(int i) {
        return switch (i) {
            case 1 -> PCSCase::getDefendant1;
            case 2 -> PCSCase::getDefendant2;
            case 3 -> PCSCase::getDefendant3;
            case 4 -> PCSCase::getDefendant4;
            case 5 -> PCSCase::getDefendant5;
            case 6 -> PCSCase::getDefendant6;
            case 7 -> PCSCase::getDefendant7;
            case 8 -> PCSCase::getDefendant8;
            case 9 -> PCSCase::getDefendant9;
            case 10 -> PCSCase::getDefendant10;
            case 11 -> PCSCase::getDefendant11;
            case 12 -> PCSCase::getDefendant12;
            case 13 -> PCSCase::getDefendant13;
            case 14 -> PCSCase::getDefendant14;
            case 15 -> PCSCase::getDefendant15;
            case 16 -> PCSCase::getDefendant16;
            case 17 -> PCSCase::getDefendant17;
            case 18 -> PCSCase::getDefendant18;
            case 19 -> PCSCase::getDefendant19;
            case 20 -> PCSCase::getDefendant20;
            case 21 -> PCSCase::getDefendant21;
            case 22 -> PCSCase::getDefendant22;
            case 23 -> PCSCase::getDefendant23;
            case 24 -> PCSCase::getDefendant24;
            case 25 -> PCSCase::getDefendant25;
            default -> throw new IllegalArgumentException("Invalid defendant index: " + i);
        };
    }

    /**
     * Gets the TypedPropertyGetter for the specified "Add Another Defendant" field.
     * 
     * @param i the defendant index (1-25)
     * @return TypedPropertyGetter for the addAnotherDefendant field
     * @throws IllegalArgumentException if index is out of range
     */
    private static TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        return switch (i) {
            case 1 -> PCSCase::getAddAnotherDefendant1;
            case 2 -> PCSCase::getAddAnotherDefendant2;
            case 3 -> PCSCase::getAddAnotherDefendant3;
            case 4 -> PCSCase::getAddAnotherDefendant4;
            case 5 -> PCSCase::getAddAnotherDefendant5;
            case 6 -> PCSCase::getAddAnotherDefendant6;
            case 7 -> PCSCase::getAddAnotherDefendant7;
            case 8 -> PCSCase::getAddAnotherDefendant8;
            case 9 -> PCSCase::getAddAnotherDefendant9;
            case 10 -> PCSCase::getAddAnotherDefendant10;
            case 11 -> PCSCase::getAddAnotherDefendant11;
            case 12 -> PCSCase::getAddAnotherDefendant12;
            case 13 -> PCSCase::getAddAnotherDefendant13;
            case 14 -> PCSCase::getAddAnotherDefendant14;
            case 15 -> PCSCase::getAddAnotherDefendant15;
            case 16 -> PCSCase::getAddAnotherDefendant16;
            case 17 -> PCSCase::getAddAnotherDefendant17;
            case 18 -> PCSCase::getAddAnotherDefendant18;
            case 19 -> PCSCase::getAddAnotherDefendant19;
            case 20 -> PCSCase::getAddAnotherDefendant20;
            case 21 -> PCSCase::getAddAnotherDefendant21;
            case 22 -> PCSCase::getAddAnotherDefendant22;
            case 23 -> PCSCase::getAddAnotherDefendant23;
            case 24 -> PCSCase::getAddAnotherDefendant24;
            case 25 -> PCSCase::getAddAnotherDefendant25;
            default -> throw new IllegalArgumentException("Invalid add-another index: " + i);
        };
    }

    /**
     * Mid-event callback for defendant details pages.
     * Handles setting correspondence address if it's the same as property address and validates address fields.
     * 
     * @param details current case details
     * @param detailsBefore previous case details
     * @param defendantIndex the defendant index being processed
     * @return response with updated case data
     */
    public AboutToStartOrSubmitResponse<PCSCase, State> midEventForDefendant(
            CaseDetails<PCSCase, State> details,
            CaseDetails<PCSCase, State> detailsBefore,
            int defendantIndex) {
        PCSCase caseData = details.getData();

        DefendantDetails defendant = getDefendantByIndex(caseData, defendantIndex);
        if (defendant != null) {
            // Set correspondence address if it's the same as property address
            if (defendant.getAddressSameAsPossession() == VerticalYesNo.YES) {
                defendant.setCorrespondenceAddress(caseData.getPropertyAddress());
            }
            
            // Validate address fields if address is known and not same as possession
            if (defendant.getAddressSameAsPossession() == VerticalYesNo.NO
                && defendant.getAddressKnown() == VerticalYesNo.YES) {

                AddressUK correspondenceAddress = defendant.getCorrespondenceAddress();
                List<String> validationErrors = addressValidator.validateAddressFields(correspondenceAddress);
                if (!validationErrors.isEmpty()) {
                    return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                        .errors(validationErrors)
                        .build();
                }
            }
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

    /**
     * Gets the defendant details for the specified index.
     * 
     * @param caseData the case data
     * @param index the defendant index (1-25)
     * @return DefendantDetails or null if index is out of range
     */
    private DefendantDetails getDefendantByIndex(PCSCase caseData, int index) {
        if (index < 1 || index > MAX_NUMBER_OF_DEFENDANTS) {
            return null;
        }
        return switch (index) {
            case 1 -> caseData.getDefendant1();
            case 2 -> caseData.getDefendant2();
            case 3 -> caseData.getDefendant3();
            case 4 -> caseData.getDefendant4();
            case 5 -> caseData.getDefendant5();
            case 6 -> caseData.getDefendant6();
            case 7 -> caseData.getDefendant7();
            case 8 -> caseData.getDefendant8();
            case 9 -> caseData.getDefendant9();
            case 10 -> caseData.getDefendant10();
            case 11 -> caseData.getDefendant11();
            case 12 -> caseData.getDefendant12();
            case 13 -> caseData.getDefendant13();
            case 14 -> caseData.getDefendant14();
            case 15 -> caseData.getDefendant15();
            case 16 -> caseData.getDefendant16();
            case 17 -> caseData.getDefendant17();
            case 18 -> caseData.getDefendant18();
            case 19 -> caseData.getDefendant19();
            case 20 -> caseData.getDefendant20();
            case 21 -> caseData.getDefendant21();
            case 22 -> caseData.getDefendant22();
            case 23 -> caseData.getDefendant23();
            case 24 -> caseData.getDefendant24();
            case 25 -> caseData.getDefendant25();
            default -> null;
        };
    }
}

