package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.AdditionalInformationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.AggressiveAnimalsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ChangeNameAddressPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.CriminalAntisocialRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.EvictionDelayWarningPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.EvictionRisksPosedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.FirearmsPossessionRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LegalCostsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LandRegistryFeesPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LivingInThePropertyPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.MoneyOwedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.StatementOfTruthPlaceHolder;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PeopleWhoWillBeEvictedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PeopleYouWantToEvictPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PoliceOrSocialServicesRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.PropertyAccessDetailsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ProtestorGroupRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.LanguageUsedPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.RepaymentsPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VerbalOrWrittenThreatsRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.ViolentAggressiveRiskPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.VulnerableAdultsChildrenPage;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.warrant.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import java.util.ArrayList;
import java.util.List;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class EnforceTheOrder implements CCDConfig<PCSCase, State, UserRole> {
    // Business requirements to be agreed on for the conditions when this event can be triggered
    private final EnforcementOrderService enforcementOrderService;
    private final AddressFormatter addressFormatter;
    private final DefendantService defendantService;
    private final FeeApplier feeApplier;
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

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enforceTheOrder.name(), this::submit, this::start)
                .forAllStates()
                .name("Enforce the order")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();
        configurePages(eventBuilder);
    }

    private void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        PageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, enforceTheOrder);
        pageBuilder
                .add(new EnforcementApplicationPage())
                .add(new NameAndAddressForEvictionPage())
                .add(new ChangeNameAddressPage())
                .add(new PeopleWhoWillBeEvictedPage())
                .add(new PeopleYouWantToEvictPage())
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
                .add(new LegalCostsPage())
                .add(new LandRegistryFeesPage())
                .add(new RepaymentsPage())
                .add(new LanguageUsedPage())
                .add(new StatementOfTruthPlaceHolder());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setFormattedPropertyAddress(addressFormatter
            .formatAddressWithHtmlLineBreaks(pcsCase.getPropertyAddress()));

        initializeDefendantData(pcsCase);
        populateDefendantSelectionList(pcsCase);
        applyWarrantFeeAmount(pcsCase);
        applyWritFeeAmount(pcsCase);

        return pcsCase;
    }

    private void initializeDefendantData(PCSCase caseData) {
        var allDefendants = caseData.getAllDefendants();
        if (!CollectionUtils.isEmpty(allDefendants)) {
            caseData.setDefendant1(allDefendants.getFirst().getValue());
        }
    }

    private void populateDefendantSelectionList(PCSCase caseData) {
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        WarrantDetails warrantDetails = WarrantDetails.builder().build();
        enforcementOrder.setWarrantDetails(warrantDetails);
        var allDefendants = caseData.getAllDefendants();
        List<DynamicStringListElement> listItems = defendantService.buildDefendantListItems(allDefendants);

        enforcementOrder.getWarrantDetails().setSelectedDefendants(
            DynamicMultiSelectStringList.builder()
                .value(new ArrayList<>())
                .listItems(listItems)
                .build()
        );
    }

    private void applyWarrantFeeAmount(PCSCase pcsCase) {
        feeApplier.applyFeeAmount(
            pcsCase,
            FeeTypes.ENFORCEMENT_WARRANT_FEE,
            (caseData, fee) -> caseData.getEnforcementOrder().setWarrantFeeAmount(fee)
        );
    }

    private void applyWritFeeAmount(PCSCase pcsCase) {
        feeApplier.applyFeeAmount(
            pcsCase,
            FeeTypes.ENFORCEMENT_WRIT_FEE,
            (caseData, fee) -> caseData.getEnforcementOrder().setWritFeeAmount(fee)
        );
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        enforcementOrderService.saveAndClearDraftData(caseReference,
                eventPayload.caseData().getEnforcementOrder());

        log.debug("Saved submitted enforcement order data and deleted draft data for case reference {}",
                caseReference);

        return SubmitResponse.defaultResponse();
    }
}
