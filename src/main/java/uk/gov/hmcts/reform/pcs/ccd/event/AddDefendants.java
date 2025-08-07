package uk.gov.hmcts.reform.pcs.ccd.event;

import de.cronn.reflection.util.TypedPropertyGetter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.AddEditDefendant;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            .showEventNotes()
            .fields(); // gives access to.page

        //loops pages
        addDefendantPages(eventBuilder);

        // finish
        eventBuilder.done();
    }
    private void addDefendantPages(FieldCollection.FieldCollectionBuilder<PCSCase, State, Event.EventBuilder<PCSCase, UserRole, State>> event) {
        //TODO: optimize this
        for (int i = 1; i <= 3; i++) {
            var defendantPage = event.page("AddDefendant" + i);

            if (i > 1) {
                defendantPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
            }
            // the first defendant page shows without a condition
            defendantPage.mandatory(getTempDefField(i));

            if (i < 3) {
                var addAnotherPage = event.page("AddAnotherDefendant" + i);
                if (i > 1) {
                    addAnotherPage.showCondition("addAnotherDefendant" + (i - 1) + "=\"YES\"");
                }
                // the first add page shows without a condition
                addAnotherPage.mandatory(getAddAnotherField(i));
            }
        }
    }


    private TypedPropertyGetter<PCSCase, ?> getTempDefField(int i) {
        switch (i) {
            case 1: return PCSCase::getAddEditDefendant1;
            case 2: return PCSCase::getAddEditDefendant2;
            case 3: return PCSCase::getAddEditDefendant3;

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

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent1(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        return updateDefendants(details, detailsBefore, details.getData().getAddEditDefendant1(), "1");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent2(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        return updateDefendants(details, detailsBefore, details.getData().getAddEditDefendant2(), "2");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent3(CaseDetails<PCSCase, State> details,
                                                                   CaseDetails<PCSCase, State> detailsBefore) {
        return updateDefendants(details, detailsBefore, details.getData().getAddEditDefendant3(), "3");
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> updateDefendants(CaseDetails<PCSCase, State> details,
                                                                          CaseDetails<PCSCase, State> detailsBefore,
                                                                          AddEditDefendant newDefendant,
                                                                          String id) {
        PCSCase caseData = details.getData();
        PCSCase caseDataBefore = detailsBefore.getData();

        List<ListValue<AddEditDefendant>> currentDefendants = Optional.ofNullable(caseDataBefore.getAddEditDefendants())
                .map(ArrayList::new)
                .orElse(new ArrayList<>());

        if (newDefendant != null) {
            currentDefendants.add(new ListValue<>(id, newDefendant));
        }

        caseData.setAddEditDefendants(currentDefendants);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .build();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworker updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}

