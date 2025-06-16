package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.Draft;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerCreateApplication;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.citizenCreateApplication;

@Component
@Slf4j
@AllArgsConstructor
public class CaseworkerCreateApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(caseworkerCreateApplication.name(), this::submit)
            .initialState(Draft)
            .name("Create a case")
            .description("Create a draft possession claim")
            .grant(Permission.CRU, UserRole.CIVIL_CASE_WORKER)
            .fields()
            .page("applicant-details-page")
                .mandatory(PCSCase::getApplicantSurname)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Case worker created case {}", caseReference);

        pcsCaseService.createCase(caseReference, pcsCase);
    }

}
