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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.PostcodeValidator;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.caseworkerUpdateApplication;

@Component
@Slf4j
@AllArgsConstructor
public class CaseworkerUpdateApplication implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final PostcodeValidator postcodeValidator;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(caseworkerUpdateApplication.name(), this::submit)
            .forStates(CASE_ISSUED)
            .name("Update case")
            .description("Update a possession case")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .fields()
            .page("page-1", this::validatePostcode)
                .mandatory(PCSCase::getPropertyAddress)
            .done();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> validatePostcode(CaseDetails<PCSCase, State> details,
                                                                         CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();

        // Validate postcode format
        if (caseData.getPropertyAddress() != null && caseData.getPropertyAddress().getPostCode() != null
            && !postcodeValidator.isValidPostcode(caseData.getPropertyAddress().getPostCode())) {
            return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
                .data(caseData)
                .errors(List.of("Enter a valid postcode"))
                .build();
        }

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(List.of())
            .build();
    }

    public void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.info("Caseworker updated case {}", caseReference);

        pcsCaseService.patchCase(caseReference, pcsCase);
    }
}
