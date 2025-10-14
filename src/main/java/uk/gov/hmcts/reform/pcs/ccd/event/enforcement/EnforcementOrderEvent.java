package uk.gov.hmcts.reform.pcs.ccd.event.enforcement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.builder.SavingPageBuilderFactory;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.EnforcementApplicationPage;
import uk.gov.hmcts.reform.pcs.ccd.page.enforcement.NameAndAddressForEvictionPage;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.exception.UnsubmittedDataException;

import static uk.gov.hmcts.reform.pcs.ccd.domain.State.AWAITING_SUBMISSION_TO_HMCTS;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.enforceTheOrder;

@Slf4j
@Component
@AllArgsConstructor
public class EnforcementOrderEvent implements CCDConfig<PCSCase, State, UserRole> {

    private final SavingPageBuilderFactory savingPageBuilderFactory;
    private final DraftCaseDataService draftCaseDataService;
    private final ModelMapper modelMapper;

    @Override
    public void configureDecentralised(
            DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
                configBuilder
                        .decentralisedEvent(enforceTheOrder.name(), this::submit, this::start)
                        .forStateTransition(AWAITING_SUBMISSION_TO_HMCTS, AWAITING_SUBMISSION_TO_HMCTS)
                        .name("Enforce the order")
                        .grant(Permission.CRUD, UserRole.PCS_SOLICITOR);

        configurePages(eventBuilder);
    }

    void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        savingPageBuilderFactory.create(eventBuilder)
                .add(new EnforcementApplicationPage())
                .add(new NameAndAddressForEvictionPage());
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        long caseReference = eventPayload.caseReference();
        draftCaseDataService.getUnsubmittedCaseData(caseReference)
            .ifPresentOrElse(
                unsubmittedCaseData -> modelMapper.map(unsubmittedCaseData, caseData),
                () -> {
                    throw new UnsubmittedDataException("No unsubmitted case data found for case " + caseReference);
                }
            );
        AddressUK propertyAddress = caseData.getPropertyAddress();
        String formattedAddress = String.format(
            "%s<br />%s<br />%s",
            propertyAddress.getAddressLine1(),
            propertyAddress.getPostTown(),
            propertyAddress.getPostCode()
        );

        caseData.setFormattedClaimantContactAddress(formattedAddress);
        return caseData;
    }

    private SubmitResponse submit(EventPayload<PCSCase, State> eventPayload) {
        return SubmitResponse.builder().build();
    }
}
