package uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim;

import de.cronn.reflection.util.TypedPropertyGetter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
@Slf4j
@AllArgsConstructor
public class DynamicDefendantsPages implements CcdPageConfiguration {

    @Override
    public void addTo(PageBuilder pageBuilder) {
        int maxNum = 25;
        for (int i = 1; i <= maxNum; i++) {

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
            if (i != maxNum) {
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
}

