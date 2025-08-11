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
            .fields() ;
        addDefendantPages(eventBuilder);
        eventBuilder.done();
    }

    private void addDefendantPages(FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder<PCSCase, UserRole, State>> event) {
        //TODO: optimize this
        for (int i = 1; i <= 3; i++) {

            var defendantPage = event.page("AddDefendant" + i);
            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            defendantPage.pageLabel("Defendant" + i)
                .mandatory(getTempDefField(i));

            var addAnotherPage = event.page("AddAnotherDefendant" + i);
            if (i > 1) {
                addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            addAnotherPage.pageLabel("Defendant Summary")
                .label("defTable" + i, """
                 <table>
                  <thead>
                    <tr>
                      <th>No.</th>
                      <th>Name</th>
                      <th>Email Address</th>
                    </tr>
                  </thead>
                  <tbody>
                  <tr><td>Defendant 1</td><td>${defendant1.firstName} ${defendant1.lastName}</td>
                    <td>${defendant1.email}</td></tr>
                  <tr><td>Defendant 2</td><td>${defendant2.firstName} ${defendant2.lastName}</td>
                    <td>${defendant2.email}</td></tr>
                  <tr><td>Defendant 3</td><td>${defendant3.firstName} ${defendant3.lastName}</td>
                    <td>${defendant3.email}</td></tr>
                  </tbody>
                </table>
               """)
	        .mandatory(getAddAnotherField(i));
        }
    }

    private TypedPropertyGetter<PCSCase, Defendant> getTempDefField(int i) {
        switch (i) {
            case 1: return PCSCase::getDefendant1;
            case 2: return PCSCase::getDefendant2;
            case 3: return PCSCase::getDefendant3;

            default: throw new IllegalArgumentException("Invalid defendant index: " + i);
        }
    }

    private TypedPropertyGetter<PCSCase, ?> getAddAnotherField(int i) {
        switch (i) {
            case 1: return PCSCase::getAddAnotherDefendant1;
            case 2: return PCSCase::getAddAnotherDefendant2;
            case 3: return PCSCase::getAddAnotherDefendant3;

            default: throw new IllegalArgumentException("Invalid add-another index: " + i);
        }
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
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

        log.info("Caseworker updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}

