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
import uk.gov.hmcts.reform.pcs.ccd.common.enforcetheorder.WarrantPagesConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.WarrantDetails;
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
    private final WarrantPagesConfigurer warrantPagesConfigurer;
    private final EnforcementOrderService enforcementOrderService;
    private final AddressFormatter addressFormatter;
    private final DefendantService defendantService;
    private final FeeApplier feeApplier;

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
        warrantPagesConfigurer.configurePages(eventBuilder);
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
