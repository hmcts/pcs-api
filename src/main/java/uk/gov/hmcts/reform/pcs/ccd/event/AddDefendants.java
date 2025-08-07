package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
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

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;

@Component
@Slf4j
@AllArgsConstructor
public class AddDefendants implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent("addDefendants", this::submit)
            .forStates(CASE_ISSUED)
            .name("Add defendants")
            .description("Add one or more defendants")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .showSummary()

            .fields()

            // PAGE 1 - Add first defendant
            .page("add-defendant-1", this::midEvent1)
            .pageLabel("Add Defendant 1")
               // .field("addEditDefendants", DisplayContext.Optional)
            .mandatory(PCSCase::getAddEditDefendant1)


            // PAGE 2 - Summary + ask to continue
            .page("summary-1")
            .mandatory(PCSCase::getDefendantsSummary,NEVER_SHOW)
                //.field("addEditDefendants", DisplayContext.Optional)
            .label("defendantsSummary1",
                   """
            <table>
              <thead>
                <tr>
                  <th>No.</th>
                  <th>Name</th>
                </tr>
              </thead>
              <tbody>
                <tr><td>1</td><td>${addEditDefendant1.firstName} ${addEditDefendant1.lastName}</td></tr>
                <tr><td>2</td><td>${addEditDefendant2.firstName} ${addEditDefendant2.lastName}</td></tr>
                <tr><td>3</td><td>${addEditDefendant3.firstName} ${addEditDefendant3.lastName}</td></tr>
              </tbody>
            </table>
            """)
            .mandatory(PCSCase::getAddAnotherDefendant1)


            // PAGE 3 - Add second
            .page("add-defendant-2",this::midEvent2)
            .showCondition("addAnotherDefendant1=\"YES\"")
            .pageLabel("Add Defendant 2")
            .mandatory(PCSCase::getAddEditDefendant2)
                // .field("addEditDefendants", DisplayContext.Optional)


            // PAGE 4 - Summary + ask to continue
            .page("summary-3")
            .mandatory(PCSCase::getDefendantsSummary,NEVER_SHOW)
              //  .field("addEditDefendants", DisplayContext.Optional)
           .showCondition("addAnotherDefendant1=\"YES\"")
            .label("defendantsSummary3",
                   """
                <table>
                  <thead>
                    <tr>
                      <th>No.</th>
                      <th>Name</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr><td>1</td><td>${addEditDefendant1.firstName} ${addEditDefendant1.lastName}</td></tr>
                    <tr><td>2</td><td>${addEditDefendant2.firstName} ${addEditDefendant2.lastName}</td></tr>
                    <tr><td>3</td><td>${addEditDefendant3.firstName} ${addEditDefendant3.lastName}</td></tr>
                  </tbody>
                </table>
                """)
            .mandatory(PCSCase::getAddAnotherDefendant2)

            // PAGE 5 - Add third
            .page("add-defendant-3",this::midEvent3)
            .showCondition("addAnotherDefendant2=\"YES\"")
            .pageLabel("Add Defendant 3")
            .mandatory(PCSCase::getAddEditDefendant3)
               // .field("addEditDefendants", DisplayContext.Optional)

            // PAGE 6 - Final Summary
            .page("summary-4")
            .showCondition("addAnotherDefendant2=\"YES\"")
            .mandatory(PCSCase::getDefendantsSummary,NEVER_SHOW)
              //  .field("addEditDefendants", DisplayContext.Optional)
            .label("defendantsSummary4",
                   """
               <table>
                 <thead>
                   <tr>
                     <th>No.</th>
                     <th>Name</th>
                   </tr>
                 </thead>
                 <tbody>
                   <tr><td>1</td><td>${addEditDefendant1.firstName} ${addEditDefendant1.lastName}</td></tr>
                   <tr><td>2</td><td>${addEditDefendant2.firstName} ${addEditDefendant2.lastName}</td></tr>
                   <tr><td>3</td><td>${addEditDefendant3.firstName} ${addEditDefendant3.lastName}</td></tr>
                 </tbody>
               </table>
               """)

            .done();
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

