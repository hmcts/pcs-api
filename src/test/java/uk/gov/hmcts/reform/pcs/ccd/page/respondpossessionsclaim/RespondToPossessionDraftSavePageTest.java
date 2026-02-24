package uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionsclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.page.respondpossessionclaim.page.RespondToPossessionDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ImmutablePartyFieldValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespondToPossessionDraftSavePageTest extends BasePageTest {

    @Mock
    private ImmutablePartyFieldValidator immutableFieldValidator;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new RespondToPossessionDraftSavePage(immutableFieldValidator));
    }

    @Test
    void shouldReturnPartialUpdateWhenNoViolations() {
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("Jack").lastName("Smith").build())
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .noticeReceived(YesNoNotSure.NO)
            .build();

        PCSCase caseData = buildCaseData(contactDetails, responses);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        assertThat(response.getData().getPossessionClaimResponse().getDefendantContactDetails())
            .isEqualTo(contactDetails);
        assertThat(response.getData().getPossessionClaimResponse().getDefendantResponses())
            .isEqualTo(responses);
    }

    @Test
    void shouldReturnErrorsWhenImmutableFieldViolationsFound() {
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .nameKnown(VerticalYesNo.YES)
                       .addressKnown(VerticalYesNo.YES)
                       .addressSameAsProperty(VerticalYesNo.NO)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(contactDetails, null);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of("nameKnown", "addressKnown", "addressSameAsProperty"));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).containsExactly(
            "Invalid submission: immutable field must not be sent: nameKnown",
            "Invalid submission: immutable field must not be sent: addressKnown",
            "Invalid submission: immutable field must not be sent: addressSameAsProperty"
        );
        assertThat(response.getData()).isNull();
    }

    @Test
    void shouldSkipValidationWhenPartyIsNull() {
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(null)
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .oweRentArrears(YesNoNotSure.NO)
            .build();

        PCSCase caseData = buildCaseData(contactDetails, responses);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
    }

    @Test
    void shouldSaveCompletePartyDataInDraft() {
        AddressUK address = AddressUK.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postCode("SW1A 1AA")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .firstName("John")
                       .lastName("Doe")
                       .emailAddress("john.doe@example.com")
                       .phoneNumber("07700900000")
                       .address(address)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(contactDetails, null);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        Party savedParty = response.getData().getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isEqualTo("Doe");
        assertThat(savedParty.getEmailAddress()).isEqualTo("john.doe@example.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07700900000");
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(savedParty.getAddress().getPostTown()).isEqualTo("London");
        assertThat(savedParty.getAddress().getPostCode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldSaveDefendantResponsesOnly() {
        DefendantResponses responses = DefendantResponses.builder()
            .tenancyTypeCorrect(YesNoNotSure.YES)
            .oweRentArrears(YesNoNotSure.NO)
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .contactByEmail(VerticalYesNo.YES)
            .contactByPhone(VerticalYesNo.NO)
            .build();

        PCSCase caseData = buildCaseData(null, responses);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        DefendantResponses savedResponses = response.getData()
            .getPossessionClaimResponse().getDefendantResponses();
        assertThat(savedResponses.getTenancyTypeCorrect()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponses.getOweRentArrears()).isEqualTo(YesNoNotSure.NO);
        assertThat(savedResponses.getReceivedFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
        assertThat(savedResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponses.getContactByPhone()).isEqualTo(VerticalYesNo.NO);
        verifyNoInteractions(immutableFieldValidator);
    }

    @Test
    void shouldSaveMixedContactDetailsAndResponses() {
        AddressUK address = AddressUK.builder()
            .addressLine1("456 Another Road")
            .postCode("M1 1AA")
            .build();

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .emailAddress("jane.smith@example.com")
                       .phoneNumber("07123456789")
                       .address(address)
                       .build())
            .build();

        DefendantResponses responses = DefendantResponses.builder()
            .contactByEmail(VerticalYesNo.YES)
            .contactByText(VerticalYesNo.NO)
            .build();

        PCSCase caseData = buildCaseData(contactDetails, responses);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        Party savedParty = response.getData().getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
        assertThat(savedParty.getEmailAddress()).isEqualTo("jane.smith@example.com");
        assertThat(savedParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(savedParty.getAddress().getAddressLine1()).isEqualTo("456 Another Road");

        DefendantResponses savedResponses = response.getData()
            .getPossessionClaimResponse().getDefendantResponses();
        assertThat(savedResponses.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponses.getContactByText()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldOmitAllNullFieldsFromParty() {
        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                       .firstName("John")
                       .lastName(null)
                       .orgName(null)
                       .nameKnown(null)
                       .emailAddress(null)
                       .address(null)
                       .addressKnown(null)
                       .addressSameAsProperty(null)
                       .phoneNumber(null)
                       .build())
            .build();

        PCSCase caseData = buildCaseData(contactDetails, null);

        when(immutableFieldValidator.findImmutableFieldViolations(any(), anyLong()))
            .thenReturn(List.of());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        Party savedParty = response.getData().getPossessionClaimResponse()
            .getDefendantContactDetails().getParty();
        assertThat(savedParty.getFirstName()).isEqualTo("John");
        assertThat(savedParty.getLastName()).isNull();
        assertThat(savedParty.getEmailAddress()).isNull();
        assertThat(savedParty.getPhoneNumber()).isNull();
        assertThat(savedParty.getAddress()).isNull();
    }

    @Test
    void shouldAllowNullPartyInPartialUpdate() {
        PCSCase caseData = buildCaseData(
            DefendantContactDetails.builder().party(null).build(),
            null
        );

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
    }

    @Test
    void shouldSkipValidationWhenDefendantContactDetailsIsNull() {
        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.NO)
            .contactByPost(VerticalYesNo.YES)
            .build();

        PCSCase caseData = buildCaseData(null, responses);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNull();
        verifyNoInteractions(immutableFieldValidator);
    }

    private PCSCase buildCaseData(DefendantContactDetails contactDetails, DefendantResponses responses) {
        return PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                                         .defendantContactDetails(contactDetails)
                                         .defendantResponses(responses)
                                         .build())
            .build();
    }
}

