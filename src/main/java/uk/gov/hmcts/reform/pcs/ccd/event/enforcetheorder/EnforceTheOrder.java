package uk.gov.hmcts.reform.pcs.ccd.event.enforcetheorder;

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
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.RawWarrantDetails;
import uk.gov.hmcts.reform.pcs.ccd.page.CommonPageContent;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.EnforcementPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrant.WarrantPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.warrantofrestitution.WarrantOfRestitutionPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ.WritPageConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.DefendantService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicMultiSelectStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;
import static uk.gov.hmcts.reform.pcs.ccd.testcasesupport.TestSupportEnvironment.isNonProdTestSupportEnabled;
import static uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter.BR_DELIMITER;
import static uk.gov.hmcts.reform.pcs.ccd.util.EnforcementTypeUtil.createDynamicStringList;

@Slf4j
@Component
@AllArgsConstructor
public class EnforceTheOrder implements CCDConfig<PCSCase, State, UserRole> {

    // Business requirements to be agreed on for the conditions when this event can be triggered
    private final EnforcementPageConfigurer enforcementPageConfigurer;
    private final WarrantPageConfigurer warrantPagesConfigurer;
    private final WritPageConfigurer writPageConfigurer;
    private final WarrantOfRestitutionPageConfigurer warrantOfRestitutionPageConfigurer;
    private final EnforcementOrderService enforcementOrderService;
    private final AddressFormatter addressFormatter;
    private final DefendantService defendantService;
    private final FeeApplier feeApplier;
    private final SavingPageBuilderFactory savingPageBuilderFactory;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        if (isNonProdTestSupportEnabled()) {
            log.info("Configuring non-production Enforcement event: {}", enforceTheOrder.name());
            configure(configBuilder);
        }
    }

    void configure(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(enforceTheOrder.name(), this::submit, this::start)
                .forAllStates()
                .name("Enforce the order")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .showSummary();
        SavingPageBuilder pageBuilder = savingPageBuilderFactory.create(eventBuilder, enforceTheOrder);
        enforcementPageConfigurer.configurePages(pageBuilder);
        warrantPagesConfigurer.configurePages(pageBuilder);
        writPageConfigurer.configurePages(pageBuilder);
        warrantOfRestitutionPageConfigurer.configurePages(pageBuilder);
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase pcsCase = eventPayload.caseData();
        pcsCase.setFormattedPropertyAddress(addressFormatter
                .formatMediumAddress(pcsCase.getPropertyAddress(), BR_DELIMITER));

        populateDefendantSelectionList(pcsCase);
        applyWarrantFeeAmount(pcsCase);
        applyWritFeeAmount(pcsCase);
        setEnforcementTypes(eventPayload.caseReference(), pcsCase.getEnforcementOrder());

        return pcsCase;
    }

    private void populateDefendantSelectionList(PCSCase caseData) {
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        RawWarrantDetails warrantDetails = RawWarrantDetails.builder().build();
        enforcementOrder.setRawWarrantDetails(warrantDetails);
        var allDefendants = caseData.getAllDefendants();
        List<DynamicStringListElement> listItems = defendantService.buildDefendantListItems(allDefendants);

        enforcementOrder.getRawWarrantDetails().setSelectedDefendants(
            DynamicMultiSelectStringList.builder()
                .value(new ArrayList<>())
                .listItems(listItems)
                .build()
        );
    }

    private void applyWarrantFeeAmount(PCSCase pcsCase) {
        feeApplier.applyFeeAmount(pcsCase,
            FeeType.ENFORCEMENT_WARRANT_FEE,
            (caseData, fee) -> caseData.getEnforcementOrder().setWarrantFeeAmount(fee));
    }

    private void applyWritFeeAmount(PCSCase pcsCase) {
        feeApplier.applyFeeAmount(pcsCase,
            FeeType.ENFORCEMENT_WRIT_FEE,
            (caseData, fee) -> caseData.getEnforcementOrder().setWritFeeAmount(fee)
        );
    }

    private void setEnforcementTypes(long caseReference, EnforcementOrder enforcementOrder) {
        List<SelectEnforcementType> enforcementTypes = new ArrayList<>(List.of(WARRANT, WRIT));

        EnforcementOrder retrievedWarrantOrder =
                enforcementOrderService.retrieveEnforcementOrder(caseReference, WARRANT);
        if (retrievedWarrantOrder != null) {
            enforcementTypes.add(WARRANT_OF_RESTITUTION);
            setWarrantRestInfoText(enforcementOrder, CommonPageContent.WARRANT_OF_RESTITUTION_INFO_TEXT);
        } else {
            setWarrantRestInfoText(enforcementOrder, "");
        }
        enforcementOrder.setChooseEnforcementType(createDynamicStringList(enforcementTypes));
    }

    private void setWarrantRestInfoText(EnforcementOrder enforcementOrder, String text) {
        enforcementOrder.setWarrantOfRestitutionInfoText(text);
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        return submitOrder(caseReference, eventPayload.caseData());
    }

    public SubmitResponse<State> submitOrder(long caseReference, PCSCase pcsCase) {
        enforcementOrderService.saveAndClearDraftData(caseReference, pcsCase.getEnforcementOrder());
        log.debug("Saved submitted enforcement order data and deleted draft data for case reference {}",
                  caseReference);
        return SubmitResponse.defaultResponse();
    }

}
