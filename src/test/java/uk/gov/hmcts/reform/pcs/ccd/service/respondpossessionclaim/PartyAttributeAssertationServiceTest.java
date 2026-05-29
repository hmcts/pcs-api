package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeAssertionStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PartyAttributeType;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PartyAttributeAssertationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyAttributeAssertionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PartyAttributeAssertationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

    @Mock
    private PartyAttributeAssertionRepository repository;
    @Mock
    private PartyEntity partyEntity;

    @Captor
    private ArgumentCaptor<List<PartyAttributeAssertationEntity>> savedAssertionsCaptor;

    private PartyAttributeAssertationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new PartyAttributeAssertationService(repository, new ObjectMapper(), FIXED_CLOCK);
    }

    @Test
    void shouldSaveNameWhenDefendantNameConfirmationIsNo() {
        Party party = Party.builder().firstName("Jane").lastName("Doe").build();
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder().defendantNameConfirmation(VerticalYesNo.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.DEFENDANT_NAME);
        assertThat(saved.get(0).getAssertedValue()).contains("Jane").contains("Doe");
    }

    @Test
    void shouldSaveNameWhenDefendantNameConfirmationIsNullUnknownPath() {
        Party party = Party.builder().firstName("Jane").lastName("Doe").build();
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder().defendantNameConfirmation(null).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.DEFENDANT_NAME);
    }

    @Test
    void shouldNotSaveNameWhenDefendantNameConfirmationIsYes() {
        Party party = Party.builder().firstName("Jane").lastName("Doe").build();
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder().defendantNameConfirmation(VerticalYesNo.YES).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldNotSaveNameWhenDefendantContactIsNull() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSaveAddressWhenCorrespondenceAddressConfirmationIsNo() {
        AddressUK address = AddressUK.builder().addressLine1("1 High St").postCode("SW1A 1AA").build();
        Party party = Party.builder().address(address).build();
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder()
                .correspondenceAddressConfirmation(VerticalYesNo.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.CORRESPONDENCE_ADDRESS);
        assertThat(saved.get(0).getAssertedValue()).contains("1 High St").contains("SW1A 1AA");
    }

    @Test
    void shouldSaveAddressWhenCorrespondenceAddressFreshEntryPath() {
        AddressUK address = AddressUK.builder().addressLine1("1 High St").postCode("SW1A 1AA").build();
        Party party = Party.builder().address(address).build();
        PossessionClaimResponse response =  PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder().correspondenceAddressConfirmation(null).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.CORRESPONDENCE_ADDRESS);
        assertThat(saved.get(0).getAssertedValue()).contains("1 High St").contains("SW1A 1AA");
    }

    @Test
    void shouldNotSaveAddressWhenCorrespondenceAddressConfirmationIsYes() {
        Party party = Party.builder().address(AddressUK.builder().addressLine1("1 High St").build()).build();
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(DefendantContactDetails.builder().party(party).build())
            .defendantResponses(DefendantResponses.builder()
                .defendantNameConfirmation(VerticalYesNo.YES)
                .correspondenceAddressConfirmation(VerticalYesNo.YES)
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSaveTenancyTypeWhenDisputedAndTypeProvided() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .tenancyTypeConfirmation(YesNoNotSure.NO)
                .tenancyType("ASSURED_SHORTHOLD")
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.TENANCY_TYPE);
        assertThat(saved.get(0).getAssertedValue()).isEqualTo("ASSURED_SHORTHOLD");
    }

    @Test
    void shouldNotSaveTenancyTypeWhenNotDisputed() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .tenancyTypeConfirmation(YesNoNotSure.YES)
                .tenancyType("ASSURED_SHORTHOLD")
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldNotSaveTenancyTypeWhenDisputedButTypeIsNull() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().tenancyTypeConfirmation(YesNoNotSure.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSaveStartDateWhenDisputedAndDateProvided() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .tenancyStartDateConfirmation(YesNoNotSure.NO)
                .tenancyStartDate(LocalDate.of(2020, 6, 1))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.TENANCY_START_DATE);
        assertThat(saved.get(0).getAssertedValue()).isEqualTo("2020-06-01");
    }

    @Test
    void shouldSaveStartDateWhenFreshEntryPath() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .tenancyStartDateConfirmation(null)
                .tenancyStartDate(LocalDate.of(2020, 6, 1))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.TENANCY_START_DATE);
    }

    @Test
    void shouldNotSaveStartDateWhenDateIsNull() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().tenancyStartDateConfirmation(YesNoNotSure.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldNotSaveStartDateWhenConfirmedCorrect() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .tenancyStartDateConfirmation(YesNoNotSure.YES)
                .tenancyStartDate(LocalDate.of(2020, 6, 1))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSavePossessionNoticeWhenDefendantSaysNo() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().possessionNoticeReceived(YesNoNotSure.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.POSSESSION_NOTICE_RECEIVED);
        assertThat(saved.get(0).getAssertedValue()).isEqualTo("NO");
    }

    @Test
    void shouldNotSavePossessionNoticeWhenDefendantSaysYes() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().possessionNoticeReceived(YesNoNotSure.YES).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSaveNoticeDateWhenPossessionNoticeYesAndDateProvided() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .possessionNoticeReceived(YesNoNotSure.YES)
                .noticeReceivedDate(LocalDate.of(2024, 3, 15))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.NOTICE_RECEIVED_DATE);
        assertThat(saved.get(0).getAssertedValue()).isEqualTo("2024-03-15");
    }

    @Test
    void shouldNotSaveNoticeDateWhenPossessionNoticeIsNo() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .possessionNoticeReceived(YesNoNotSure.NO)
                .noticeReceivedDate(LocalDate.of(2024, 3, 15))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue())
            .extracting(PartyAttributeAssertationEntity::getAttributesName)
            .doesNotContain(PartyAttributeType.NOTICE_RECEIVED_DATE);
    }

    @Test
    void shouldNotSaveNoticeDateWhenDateIsNull() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().possessionNoticeReceived(YesNoNotSure.YES).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSaveRentArrearsWhenDisputedAndAmountProvided() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .rentArrearsAmountConfirmation(YesNoNotSure.NO)
                .rentArrearsAmount(new BigDecimal("1500.00"))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getAttributesName()).isEqualTo(PartyAttributeType.RENT_ARREARS_AMOUNT);
        assertThat(saved.get(0).getAssertedValue()).isEqualTo("1500.00");
    }

    @Test
    void shouldNotSaveRentArrearsWhenNotDisputed() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .rentArrearsAmountConfirmation(YesNoNotSure.YES)
                .rentArrearsAmount(new BigDecimal("1500.00"))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldNotSaveRentArrearsWhenAmountIsNull() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder().rentArrearsAmountConfirmation(YesNoNotSure.NO).build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        assertThat(savedAssertionsCaptor.getValue()).isEmpty();
    }

    @Test
    void savedAssertionShouldHaveCorrectMetadata() {
        PossessionClaimResponse response = PossessionClaimResponse.builder()
            .defendantContactDetails(null)
            .defendantResponses(DefendantResponses.builder()
                .rentArrearsAmountConfirmation(YesNoNotSure.NO)
                .rentArrearsAmount(new BigDecimal("500.00"))
                .build())
            .build();

        underTest.buildPartyAttributeEntities(response, partyEntity);

        verify(repository).saveAll(savedAssertionsCaptor.capture());
        List<PartyAttributeAssertationEntity> saved = savedAssertionsCaptor.getValue();
        assertThat(saved).isNotEmpty();
        PartyAttributeAssertationEntity assertion = saved.get(0);
        assertThat(assertion.getAssertedBy()).isEqualTo(PartyAttributeAssertedBy.DEFENDANT);
        assertThat(assertion.getStatus()).isEqualTo(PartyAttributeAssertionStatus.SUBMITTED);
        assertThat(assertion.getParty()).isEqualTo(partyEntity);
        assertThat(assertion.getCreatedBy()).isEqualTo(partyEntity);
        assertThat(assertion.getLastUpdatedBy()).isEqualTo(partyEntity);
        assertThat(assertion.getCreatedAt()).isEqualTo(NOW);
        assertThat(assertion.getLastUpdatedAt()).isEqualTo(NOW);
    }
}
