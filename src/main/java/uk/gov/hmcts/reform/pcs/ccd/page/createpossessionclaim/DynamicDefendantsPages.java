package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import de.cronn.reflection.util.TypedPropertyGetter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.DefendantConstants;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicDefendantsPages implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        for (int i = 1; i <= DefendantConstants.MAX_NUMBER_OF_DEFENDANTS; i++) {

            // Defendant details page
            var defendantPage = pageBuilder.page("DefendantDetails" + i);
            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"Yes\"");
            }
            defendantPage.pageLabel("Defendant " + i + " details")
                .mandatory(getTempDefField(i));

            // Add Another / Summary page
            var addAnotherPage = pageBuilder.page("DefendantList" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"Yes\"");
            }
                   addAnotherPage.pageLabel("Defendant List")
                           .label("defTable" + i, buildDefendantsSummaryTable(i));
            if (i != DefendantConstants.MAX_NUMBER_OF_DEFENDANTS) {
                addAnotherPage.mandatory(getAddAnotherField(i));
            }

        }
    }

    private TypedPropertyGetter<PCSCase, DefendantDetails> getTempDefField(int i) {
        switch (i) {
            case 1:  return PCSCase::getDefendant1;
            case 2:  return PCSCase::getDefendant2;
            case 3:  return PCSCase::getDefendant3;
            case 4:  return PCSCase::getDefendant4;
            case 5:  return PCSCase::getDefendant5;
            case 6:  return PCSCase::getDefendant6;
            case 7:  return PCSCase::getDefendant7;
            case 8:  return PCSCase::getDefendant8;
            case 9:  return PCSCase::getDefendant9;
            case 10: return PCSCase::getDefendant10;
            case 11: return PCSCase::getDefendant11;
            case 12: return PCSCase::getDefendant12;
            case 13: return PCSCase::getDefendant13;
            case 14: return PCSCase::getDefendant14;
            case 15: return PCSCase::getDefendant15;
            case 16: return PCSCase::getDefendant16;
            case 17: return PCSCase::getDefendant17;
            case 18: return PCSCase::getDefendant18;
            case 19: return PCSCase::getDefendant19;
            case 20: return PCSCase::getDefendant20;
            case 21: return PCSCase::getDefendant21;
            case 22: return PCSCase::getDefendant22;
            case 23: return PCSCase::getDefendant23;
            case 24: return PCSCase::getDefendant24;
            case 25: return PCSCase::getDefendant25;
            default: throw new IllegalArgumentException("Invalid defendant index: " + i);
        }
    }

    private TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        switch (i) {
            case 1:  return PCSCase::getAddAnotherDefendant1;
            case 2:  return PCSCase::getAddAnotherDefendant2;
            case 3:  return PCSCase::getAddAnotherDefendant3;
            case 4:  return PCSCase::getAddAnotherDefendant4;
            case 5:  return PCSCase::getAddAnotherDefendant5;
            case 6:  return PCSCase::getAddAnotherDefendant6;
            case 7:  return PCSCase::getAddAnotherDefendant7;
            case 8:  return PCSCase::getAddAnotherDefendant8;
            case 9:  return PCSCase::getAddAnotherDefendant9;
            case 10: return PCSCase::getAddAnotherDefendant10;
            case 11: return PCSCase::getAddAnotherDefendant11;
            case 12: return PCSCase::getAddAnotherDefendant12;
            case 13: return PCSCase::getAddAnotherDefendant13;
            case 14: return PCSCase::getAddAnotherDefendant14;
            case 15: return PCSCase::getAddAnotherDefendant15;
            case 16: return PCSCase::getAddAnotherDefendant16;
            case 17: return PCSCase::getAddAnotherDefendant17;
            case 18: return PCSCase::getAddAnotherDefendant18;
            case 19: return PCSCase::getAddAnotherDefendant19;
            case 20: return PCSCase::getAddAnotherDefendant20;
            case 21: return PCSCase::getAddAnotherDefendant21;
            case 22: return PCSCase::getAddAnotherDefendant22;
            case 23: return PCSCase::getAddAnotherDefendant23;
            case 24: return PCSCase::getAddAnotherDefendant24;
            case 25: return PCSCase::getAddAnotherDefendant25;
            default: throw new IllegalArgumentException("Invalid add-another index: " + i);
        }
    }

           private String buildDefendantsSummaryTable(int upToDefendant) {
               StringBuilder htmlTable = new StringBuilder();
               htmlTable.append("""
                   <h2>Defendants</h2>
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

               for (int i = 1; i <= upToDefendant; i++) {
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


                   htmlTable.append("<tr class=\"govuk-table__row\">")
                       .append("<td class=\"govuk-table__cell\">Defendant ").append(i).append("</td>")
                       .append("<td class=\"govuk-table__cell\">${defendant").append(i).append(".firstName} ${defendant").append(i).append(".lastName}</td>")
                       .append("<td class=\"govuk-table__cell\">")
                       .append("Address details will be displayed here")
                       .append("</td>")
                       .append("<td class=\"govuk-table__cell\">${defendant").append(i).append(".email}</td>")
                       .append("</tr>");
               }

               htmlTable.append("""
                     </tbody>
                   </table>
                       """);

               return htmlTable.toString();
           }

    /**
     * Builds a defendants summary table with actual data (for runtime use)
     */
    public String buildDefendantsSummaryTableWithData(PCSCase pcsCase, int upToDefendant) {
        StringBuilder htmlTable = new StringBuilder();
        htmlTable.append("""
            <h2>Defendants</h2>
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

        for (int i = 1; i <= upToDefendant; i++) {
            DefendantDetails defendant = getDefendantByIndex(pcsCase, i);
            if (defendant != null) {
                String fullName = buildFullName(defendant);
                String address = formatAddress(defendant.getCorrespondenceAddress());
                String email = defendant.getEmail() != null ? defendant.getEmail() : "Not provided";
                
                htmlTable.append("<tr class=\"govuk-table__row\">")
                    .append("<td class=\"govuk-table__cell\">Defendant ").append(i).append("</td>")
                    .append("<td class=\"govuk-table__cell\">").append(fullName).append("</td>")
                    .append("<td class=\"govuk-table__cell\">").append(address).append("</td>")
                    .append("<td class=\"govuk-table__cell\">").append(email).append("</td>")
                    .append("</tr>");
            }
        }

        htmlTable.append("""
              </tbody>
            </table>
                """);

        return htmlTable.toString();
    }

    private String buildFullName(DefendantDetails defendant) {
        if (defendant.getFirstName() != null && defendant.getLastName() != null) {
            return defendant.getFirstName() + " " + defendant.getLastName();
        } else if (defendant.getFirstName() != null) {
            return defendant.getFirstName();
        } else if (defendant.getLastName() != null) {
            return defendant.getLastName();
        } else {
            return "Name not provided";
        }
    }

    private DefendantDetails getDefendantByIndex(PCSCase pcsCase, int index) {
        return switch (index) {
            case 1 -> pcsCase.getDefendant1();
            case 2 -> pcsCase.getDefendant2();
            case 3 -> pcsCase.getDefendant3();
            case 4 -> pcsCase.getDefendant4();
            case 5 -> pcsCase.getDefendant5();
            case 6 -> pcsCase.getDefendant6();
            case 7 -> pcsCase.getDefendant7();
            case 8 -> pcsCase.getDefendant8();
            case 9 -> pcsCase.getDefendant9();
            case 10 -> pcsCase.getDefendant10();
            case 11 -> pcsCase.getDefendant11();
            case 12 -> pcsCase.getDefendant12();
            case 13 -> pcsCase.getDefendant13();
            case 14 -> pcsCase.getDefendant14();
            case 15 -> pcsCase.getDefendant15();
            case 16 -> pcsCase.getDefendant16();
            case 17 -> pcsCase.getDefendant17();
            case 18 -> pcsCase.getDefendant18();
            case 19 -> pcsCase.getDefendant19();
            case 20 -> pcsCase.getDefendant20();
            case 21 -> pcsCase.getDefendant21();
            case 22 -> pcsCase.getDefendant22();
            case 23 -> pcsCase.getDefendant23();
            case 24 -> pcsCase.getDefendant24();
            case 25 -> pcsCase.getDefendant25();
            default -> null;
        };
    }

    private String formatAddress(AddressUK address) {
        if (address == null) {
            return "No address provided";
        }
        
        StringBuilder addressStr = new StringBuilder();
        
        if (address.getAddressLine1() != null && !address.getAddressLine1().trim().isEmpty()) {
            addressStr.append(address.getAddressLine1().trim());
        }
        
        if (address.getPostTown() != null && !address.getPostTown().trim().isEmpty()) {
            if (addressStr.length() > 0) {
                addressStr.append(", ");
            }
            addressStr.append(address.getPostTown().trim());
        }
        
        if (address.getPostCode() != null && !address.getPostCode().trim().isEmpty()) {
            if (addressStr.length() > 0) {
                addressStr.append(", ");
            }
            addressStr.append(address.getPostCode().trim());
        }
        
           return addressStr.length() > 0 ? addressStr.toString() : "No address provided";
       }

       /**
        * Mid-event callback to populate the defendants table HTML dynamically
        */
       private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                     CaseDetails<PCSCase, State> detailsBefore) {
           PCSCase caseData = details.getData();
           
           // Build the table with current defendant data
           String tableHtml = buildDefendantsSummaryTableWithData(caseData, DefendantConstants.MAX_NUMBER_OF_DEFENDANTS);
           caseData.setDefendantsTableHtml(tableHtml);
           
           return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
               .data(caseData)
               .build();
       }
}

