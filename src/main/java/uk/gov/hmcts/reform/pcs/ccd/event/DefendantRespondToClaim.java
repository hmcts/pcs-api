package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.defendantRespondToClaim;

@Component
@Slf4j
@AllArgsConstructor
public class DefendantRespondToClaim implements CCDConfig<PCSCase, State, UserRole> {

    private PcsCaseRepository pcsCaseRepository;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(defendantRespondToClaim.name(), this::submit)
            .forAllStates()
//            .showCondition(ShowConditions.NEVER_SHOW)
            .name("Respond to claim")
            .description("Respond to claim")
            .grant(Permission.CRU, DEFENDANT)
            .fields()
            .page("respondToClaim-code")
            .label("defendantRespondToClaim-info", """
                        ---
                        You can provide a response to the claim here
                        """)
            .optionalWithLabel(PCSCase::getDefendantResponse, "Provide your response to the claim");
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        PcsCaseEntity pcsCaseEntity = getPcsCaseEntity(caseReference);
        pcsCaseEntity.setDefendantResponse(pcsCase.getDefendantResponse());
    }

    private PcsCaseEntity getPcsCaseEntity(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

}
