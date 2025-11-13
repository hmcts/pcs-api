package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import java.math.BigDecimal;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final FeesAndPayService feesAndPayService;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;

    private static final String CASE_ISSUED_FEE_TYPE = "caseIssueFee";
    private static final String FEE = "Unable to retrieve";

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.AWAITING_FURTHER_CLAIM_DETAILS)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new PostcodeNotAssignedToCourt());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        try {
            caseData.setFeeAmount(formatAsCurrency(
                feesAndPayService.getFee(CASE_ISSUED_FEE_TYPE).getFeeAmount()
            ));
        } catch (Exception e) {
            // Fallback to default fee if API is unavailable (during config generation)
            log.error("Error while getting fee", e);
            caseData.setFeeAmount(FEE);
        }

        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, caseData.getPropertyAddress(), caseData.getLegislativeCountry());

        return SubmitResponse.defaultResponse();
    }

    private String formatAsCurrency(BigDecimal amount) {
        if (amount == null) {
            return FEE;
        }
        return "£" + amount.stripTrailingZeros().toPlainString();
    }
}
