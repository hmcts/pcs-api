package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ClaimResponseService;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimResponseServiceTest {

    private static final UUID TEST_IDAM_ID = UUID.randomUUID();
    private static final UUID TEST_PARTY_ID = UUID.randomUUID();
    private static final AddressUK TEST_ADDRESS = AddressUK.builder()
        .addressLine1("123 Test Street")
        .postTown("London")
        .postCode("SW1A 1AA")
        .build();

    @Mock
    private ModelMapper modelMapper;

    private ClaimResponseService underTest;

    private PartyEntity testParty;

    @BeforeEach
    void setUp() {
        testParty = new PartyEntity();
        testParty.setId(TEST_PARTY_ID);
        testParty.setIdamId(TEST_IDAM_ID);

        underTest = new ClaimResponseService(modelMapper);
    }

    @Test
    void shouldSaveDraftDataWithAllFieldsProvided() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .emailAddress("defendant@example.com")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .contactByPost(VerticalYesNo.NO)
                .contactByPhone(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .propertyAddressConfirmation(VerticalYesNo.YES)
                .build()
        );

        final AddressEntity addressEntity = new AddressEntity();
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(testParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(testParty.getAddress()).isEqualTo(addressEntity);

        ContactPreferencesEntity savedPrefs = testParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void saveDraftDataWithDateOfBirth() {
        // Given
        LocalDate dob = LocalDate.now();
        PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .emailAddress("defendant@example.com")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .contactByPost(VerticalYesNo.NO)
                .contactByPhone(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .dateOfBirth(dob)
                .propertyAddressConfirmation(VerticalYesNo.YES)
                .build()
        );

        AddressEntity addressEntity = new AddressEntity();
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(testParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(testParty.getAddress()).isEqualTo(addressEntity);
        assertThat(testParty.getDateOfBirth()).isEqualTo(dob);

        ContactPreferencesEntity savedPrefs = testParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldSaveOnlyPhoneNumber() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByPhone(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(testParty.getEmailAddress()).isNull();
    }

    @Test
    void shouldSaveOnlyEmail() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .emailAddress("defendant@example.com")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getEmailAddress()).isEqualTo("defendant@example.com");
        assertThat(testParty.getPhoneNumber()).isNull();
    }

    @Test
    void shouldNotUpdatePartyWhenContactDetailsAreBlank() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("")
                .emailAddress("   ")
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getContactPreferences()).isNotNull();
        assertThat(testParty.getContactPreferences().getContactByEmail()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldThrowExceptionWhenPartyIsNull() {
        // Given
        PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().build()
        );

        // When / Then
        assertThatThrownBy(() -> underTest.saveDraftDataForParty(response, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("defendant party is null");
    }

    @Test
    void shouldHandleAllPreferencesSetToNo() {
        // Given
        final PossessionClaimResponse response = buildResponse(Party.builder()
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .contactByPhone(VerticalYesNo.NO)
                //no text option is possible when contact by phone = no
                .contactByPost(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        ContactPreferencesEntity savedPrefs = testParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.NO);
        assertThat(savedPrefs.getContactByText()).isEqualTo(null);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldHandleAllPreferencesSetToYes() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder()
                .phoneNumber("07123456789")
                .emailAddress("test@example.com")
                .address(TEST_ADDRESS)
                .build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .contactByPhone(VerticalYesNo.YES)
                .contactByText(VerticalYesNo.YES)
                .contactByPost(VerticalYesNo.YES)
                .propertyAddressConfirmation(VerticalYesNo.YES)
                .build()
        );

        final AddressEntity addressEntity = AddressEntity.builder()
            .addressLine1("123 Test Street")
            .postTown("London")
            .postcode("SW1A 1AA")
            .build();
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getAddress()).isNotNull();
        assertThat(testParty.getAddress()).isEqualTo(addressEntity);
        assertThat(testParty.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(testParty.getAddress().getPostTown()).isEqualTo("London");
        assertThat(testParty.getAddress().getPostcode()).isEqualTo("SW1A 1AA");

        assertThat(testParty.getPhoneNumber()).isEqualTo("07123456789");
        assertThat(testParty.getEmailAddress()).isEqualTo("test@example.com");

        ContactPreferencesEntity savedPrefs = testParty.getContactPreferences();
        assertThat(savedPrefs.getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByText()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByEmail()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedPrefs.getContactByPost()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldUpdateNameWhenClaimantDidNotProvideIt() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder().firstName("John").lastName("Doe").build(),
            DefendantResponses.builder().contactByEmail(VerticalYesNo.YES).build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getFirstName()).isEqualTo("John");
        assertThat(testParty.getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldNotUpdateNameWhenClaimantProvided() {
        // Given
        testParty.setFirstName("ClaimantFirst");
        testParty.setLastName("ClaimantLast");

        final PossessionClaimResponse response = buildResponse(
            Party.builder().firstName("DefendantFirst").lastName("DefendantLast").build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .defendantNameConfirmation(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getFirstName()).isEqualTo("ClaimantFirst");
        assertThat(testParty.getLastName()).isEqualTo("ClaimantLast");
    }

    @Test
    void shouldUpdateAddressWhenClaimantDidNotProvideIt() {
        // Given
        final PossessionClaimResponse response = buildResponse(
            Party.builder().address(TEST_ADDRESS).build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .propertyAddressConfirmation(VerticalYesNo.NO)
                .build()
        );

        final AddressEntity addressEntity = new AddressEntity();
        when(modelMapper.map(TEST_ADDRESS, AddressEntity.class)).thenReturn(addressEntity);

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getAddress()).isEqualTo(addressEntity);
        assertThat(testParty.getAddressKnown()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldNotUpdateAddressWhenClaimantProvided() {
        // Given
        AddressEntity existingAddress = AddressEntity.builder().addressLine1("Claimant Street").build();
        testParty.setAddress(existingAddress);

        final PossessionClaimResponse response = buildResponse(
            Party.builder().address(AddressUK.builder().addressLine1("Defendant Street").build()).build(),
            DefendantResponses.builder()
                .contactByEmail(VerticalYesNo.YES)
                .correspondenceAddressConfirmation(VerticalYesNo.YES)
                .build()
        );

        // When
        underTest.saveDraftDataForParty(response, testParty);

        // Then
        assertThat(testParty.getAddress().getAddressLine1()).isEqualTo("Claimant Street");
    }

    @Test
    void shouldSetAddressSameAsPropertyToNoWhenCorrespondenceAddressConfirmationIsNo() {
        testParty.setAddressSameAsProperty(VerticalYesNo.YES);

        final PossessionClaimResponse response = buildResponse(
            Party.builder().address(TEST_ADDRESS).build(),
            DefendantResponses.builder().correspondenceAddressConfirmation(VerticalYesNo.NO).build()
        );

        underTest.saveDraftDataForParty(response, testParty);

        assertThat(testParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldLeaveAddressSameAsPropertyAsYesWhenCorrespondenceAddressConfirmationIsYes() {
        testParty.setAddressSameAsProperty(VerticalYesNo.YES);

        final PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().correspondenceAddressConfirmation(VerticalYesNo.YES).build()
        );

        underTest.saveDraftDataForParty(response, testParty);

        assertThat(testParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldLeaveAddressSameAsPropertyAsNoWhenClaimantTypedAddressAndCorrespondenceAddressConfirmationIsYes() {
        testParty.setAddressSameAsProperty(VerticalYesNo.NO);

        final PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().correspondenceAddressConfirmation(VerticalYesNo.YES).build()
        );

        underTest.saveDraftDataForParty(response, testParty);

        assertThat(testParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.NO);
    }

    @Test
    void shouldLeaveAddressSameAsPropertyUnchangedWhenCorrespondenceAddressConfirmationIsAbsent() {
        testParty.setAddressSameAsProperty(VerticalYesNo.YES);

        final PossessionClaimResponse response = buildResponse(
            Party.builder().build(),
            DefendantResponses.builder().build()
        );

        underTest.saveDraftDataForParty(response, testParty);

        assertThat(testParty.getAddressSameAsProperty()).isEqualTo(VerticalYesNo.YES);
    }

    private PossessionClaimResponse buildResponse(Party party, DefendantResponses defendantResponses) {
        return PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(defendantResponses)
            .build();
    }


}
