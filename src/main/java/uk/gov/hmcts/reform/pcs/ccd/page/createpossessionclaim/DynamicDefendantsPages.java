package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.DefendantConstants;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.util.DefendantUtils;

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
                .mandatory(DefendantUtils.getTempDefField(i));

            // Add Another / Summary page
            var addAnotherPage = pageBuilder.page("DefendantList" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"Yes\"");
            }
                   addAnotherPage.pageLabel("Defendant List")
                           .label("defTable" + i, buildDefendantsSummaryTable(i));
            if (i != DefendantConstants.MAX_NUMBER_OF_DEFENDANTS) {
                addAnotherPage.mandatory(DefendantUtils.getAddAnotherField(i));
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



}

