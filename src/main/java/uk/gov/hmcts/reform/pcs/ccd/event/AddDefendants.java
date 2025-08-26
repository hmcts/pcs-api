package uk.gov.hmcts.reform.pcs.ccd.event;

import de.cronn.reflection.util.TypedPropertyGetter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.Defendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;

@Component
@Slf4j
@AllArgsConstructor
public class AddDefendants implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        var eventBuilder = configBuilder
            .decentralisedEvent("addDefendants", this::submit)
            .forStates(CASE_ISSUED)
            .name("Add defendants")
            .description("Add up to 25 defendants")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .showSummary()
            .fields();
        addDefendantPages(eventBuilder);
        eventBuilder.done();
    }

    private void addDefendantPages(FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder
            <PCSCase, UserRole, State>> event) {
        int maxNum = 3;
        for (int i = 1; i <= maxNum; i++) {

            //Defendant details page
            var defendantPage = event.page("AddDefendant" + i);
            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            defendantPage.pageLabel("Defendant ")
                .mandatory(getTempDefField(i));

            //Add Another / Summary page
            var addAnotherPage = event.page("AddAnotherDefendant" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            addAnotherPage.pageLabel("Defendant List")
                    .label("defTable" + i, buildDefendantsSummaryTable(i));
            if (i != maxNum) {
                addAnotherPage.mandatory(getAddAnotherField(i));
            }

        }
    }

    private TypedPropertyGetter<PCSCase, Defendant> getTempDefField(int i) {
        switch (i) {
            case 1:  return PCSCase::getDefendant1;
            case 2:  return PCSCase::getDefendant2;
            case 3:  return PCSCase::getDefendant3;
            default: throw new IllegalArgumentException("Invalid defendant index: " + i);
        }
    }

    private TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        switch (i) {
            case 1:  return PCSCase::getAddAnotherDefendant1;
            case 2:  return PCSCase::getAddAnotherDefendant2;
            case 3:  return PCSCase::getAddAnotherDefendant3;
            default: throw new IllegalArgumentException("Invalid add-another index: " + i);
        }
    }

    private String buildDefendantsSummaryTable(int upToDefendant) {
        StringBuilder htmlTable = new StringBuilder();
        htmlTable.append("""
        <h2> Defendants </h2><br>
        <table>
          <thead>
            <tr>
              <th>Defendant</th>
              <th>Defendant <br> name</th>
              <th>Defendant <br> correspondence address</th>
              <th>Defendant <br> email address</th>
            </tr>
          </thead>
          <tbody>
            """);

        for (int i = 1; i <= upToDefendant; i++) {
            htmlTable.append("<tr><td>Defendant ")
                .append(i)
                .append("</td><td>${defendant")
                .append(i)
                .append(".firstName} ${defendant")
                .append(i)
                .append(".lastName}</td><td>")
                .append("${defendant").append(i).append(".correspondenceAddress.AddressLine1}<br>")
                .append("${defendant").append(i).append(".correspondenceAddress.PostTown}<br>")
                .append("${defendant").append(i).append(".correspondenceAddress.PostCode}")
                .append("</td><td>${defendant")
                .append(i)
                .append(".email}</td></tr>");
        }

        htmlTable.append("""
          </tbody>
        </table>
            """);

        return htmlTable.toString();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        List<ListValue<Defendant>> finalDefendants = new ArrayList<>();
        if (pcsCase.getDefendant1() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant1()));
        }
        if (pcsCase.getDefendant2() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant2()));
        }
        if (pcsCase.getDefendant3() != null) {
            finalDefendants.add(new ListValue<>(UUID.randomUUID().toString(), pcsCase.getDefendant3()));
        }
        pcsCase.setDefendants(finalDefendants);
        long caseReference = eventPayload.caseReference();

        log.info("Caseworker updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}

