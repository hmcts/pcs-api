package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CheckYourAnswersPlaceHolder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionDelayWarningPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EvictionRisksPosedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.LivingInThePropertyPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.MoneyOwedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.VulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcement.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.CurrencyFormatter;
import uk.gov.hmcts.reform.pcs.feesandpay.service.FeesAndPayService;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class EnforcementOrderEvent implements CCDConfig<PCSCase, State, UserRole> {
    // Business requirements to be agreed on for the conditions when this event can be triggered
    private static final String ENFORCEMENT_WARRANT_FEE = "FEE0380";
    private static final String ENFORCEMENT_WRIT_FEE = "FEE0397";

    private final EnforcementOrderService enforcementOrderService;
    private final AddressFormatter addressFormatter;
    private final FeesAndPayService feesAndPayService;
    private final ViolentAggressiveRiskPage violentAggressiveRiskPage;
    private final VerbalOrWrittenThreatsRiskPage verbalOrWrittenThreatsRiskPage;
    private final ProtestorGroupRiskPage protestorGroupRiskPage;
    private final PoliceOrSocialServicesRiskPage policeOrSocialServicesRiskPage;
    private final FirearmsPossessionRiskPage firearmsPossessionRiskPage;
    private final CriminalAntisocialRiskPage criminalAntisocialRiskPage;
    private final AggressiveAnimalsRiskPage aggressiveAnimalsRiskPage;
    private final PropertyAccessDetailsPage propertyAccessDetailsPage;
    private final VulnerableAdultsChildrenPage vulnerableAdultsChildrenPage;
    private final AdditionalInformationPage additionalInformationPage;
    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final CurrencyFormatter currencyFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enforceTheOrder.name(), this::submit, this::start)
                .forState(AWAITING_SUBMISSION_TO_HMCTS)
                .name("Enforce the order")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);
        configurePages(eventBuilder);
    }

    private void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        PageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, enforceTheOrder);
        pageBuilder
                .add(new EnforcementApplicationPage())
                .add(new NameAndAddressForEvictionPage())
                .add(new LivingInThePropertyPage())
                .add(new EvictionDelayWarningPage())
                .add(new EvictionRisksPosedPage())
                .add(violentAggressiveRiskPage)
                .add(firearmsPossessionRiskPage)
                .add(criminalAntisocialRiskPage)
                .add(verbalOrWrittenThreatsRiskPage)
                .add(protestorGroupRiskPage)
                .add(policeOrSocialServicesRiskPage)
                .add(aggressiveAnimalsRiskPage)
                .add(vulnerableAdultsChildrenPage)
                .add(propertyAccessDetailsPage)
                .add(additionalInformationPage)
                .add(new MoneyOwedPage())
                .add(new CheckYourAnswersPlaceHolder());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setFormattedPropertyAddress(addressFormatter
            .formatAddressWithHtmlLineBreaks(pcsCase.getPropertyAddress()));

        if (pcsCase.getAllDefendants() != null && !pcsCase.getAllDefendants().isEmpty()) {
            pcsCase.setDefendant1(pcsCase.getAllDefendants().getFirst().getValue());
        }

        applyWarrantFeeAmount(pcsCase);
        applyWritFeeAmount(pcsCase);

        return pcsCase;
    }

    private void applyWarrantFeeAmount(PCSCase pcsCase) {
        try {
            pcsCase.getEnforcementOrder().setWarrantFeeAmount(currencyFormatter.formatAsCurrency(
                feesAndPayService.getFee(ENFORCEMENT_WARRANT_FEE).getFeeAmount()
            ));
        } catch (Exception e) {
            // Fallback to default fee if API is unavailable (during config generation)
            log.error("Error while getting warrant fee", e);
            pcsCase.getEnforcementOrder().setWarrantFeeAmount(currencyFormatter.formatAsCurrency(null));
        }
    }

    private void applyWritFeeAmount(PCSCase pcsCase) {
        try {
            pcsCase.getEnforcementOrder().setWritFeeAmount(currencyFormatter.formatAsCurrency(
                feesAndPayService.getFee(ENFORCEMENT_WRIT_FEE).getFeeAmount()
            ));
        } catch (Exception e) {
            // Fallback to default fee if API is unavailable (during config generation)
            log.error("Error while getting writ fee", e);
            pcsCase.getEnforcementOrder().setWritFeeAmount(currencyFormatter.formatAsCurrency(null));
        }
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        // Delete unsubmitted data once HDPI-2637 implemented after enforcement order is created in same transaction
        enforcementOrderService.createEnforcementOrder(caseReference, eventPayload.caseData().getEnforcementOrder());

        return SubmitResponse.defaultResponse();
    }
}
