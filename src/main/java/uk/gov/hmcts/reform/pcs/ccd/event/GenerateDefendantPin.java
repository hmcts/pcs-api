package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.generatedefendantpin.ShowDefendantLetter;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.generateDefendantPin;

@Component
@Slf4j
@AllArgsConstructor
public class GenerateDefendantPin implements CCDConfig<PCSCase, State, UserRole> {

    private final AccessCodeService accessCodeService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder = configBuilder
            .decentralisedEvent(generateDefendantPin.name(), this::submit, this::start)
            .forStates(State.AWAITING_SUBMISSION_TO_HMCTS, State.CASE_ISSUED)
            .name("Send PIN in post")
            .description("Simulate defendent PIN in post")
            .grant(Permission.CRU, CLAIMANT_SOLICITOR);

        new PageBuilder(eventBuilder)
            .add(new ShowDefendantLetter());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();

        PCSCase caseData = eventPayload.caseData();
        caseData.setLinkCode(accessCodeService.createAccessCode(caseReference, UserRole.DEFENDANT));

        AddressUK propertyAddress = caseData.getPropertyAddress();
        String formattedPropertyAddress = formatAddress(propertyAddress);

        DefendantDetails defendant1 = caseData.getDefendants().getFirst().getValue();

        caseData.setDefendant1(defendant1);
        caseData.setFormattedPropertyAddress(formattedPropertyAddress);

        return caseData;
    }

    private static String formatAddress(AddressUK propertyAddress) {
        return String.format("%s, %s, %s",
            propertyAddress.getAddressLine1(), propertyAddress.getPostTown(), propertyAddress.getPostCode());
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
    }

}
