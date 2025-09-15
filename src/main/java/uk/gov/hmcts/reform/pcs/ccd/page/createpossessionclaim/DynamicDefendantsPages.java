package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import de.cronn.reflection.util.TypedPropertyGetter;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicDefendantsPages implements CcdPageConfiguration {

    /**
     * Maximum number of defendants that can be added to a case.
     */
    private static final int MAX_NUMBER_OF_DEFENDANTS = 25;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        for (int i = 1; i <= MAX_NUMBER_OF_DEFENDANTS; i++) {

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
            if (i != MAX_NUMBER_OF_DEFENDANTS) {
                addAnotherPage.mandatory(getAddAnotherField(i));
            }

        }
    }


           public String buildDefendantsSummaryTable(int upToDefendant) {
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
                   htmlTable.append("<tr class=\"govuk-table__row\">")
                       .append("<td class=\"govuk-table__cell\">Defendant ").append(i).append("</td>")
                       .append("<td class=\"govuk-table__cell\">${defendant").append(i).append(".firstName} ${defendant").append(i).append(".lastName}</td>")
                       .append("<td class=\"govuk-table__cell\">")
                       .append("${defendant").append(i).append(".correspondenceAddress.AddressLine1}<br>")
                       .append("${defendant").append(i).append(".correspondenceAddress.PostTown}<br>")
                       .append("${defendant").append(i).append(".correspondenceAddress.PostCode}")
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

}

