package uk.gov.hmcts.reform.pcs.ccd.service.coverletter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseNameFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterDocumentFetcher;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coverletter.CoveringLetterPayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoveringLetterProviderTest {

    private static final Clock UK_CLOCK =
        Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/London"));

    @Mock
    private CoveringLetterPayloadBuilder coveringLetterPayloadBuilder;
    @Mock
    private CoveringLetterDocumentGenerator coveringLetterDocumentGenerator;
    @Mock
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private LetterDocumentFetcher letterDocumentFetcher;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private CaseNameFormatter caseNameFormatter;

    private CoveringLetterProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new CoveringLetterProvider(
            coveringLetterPayloadBuilder, coveringLetterDocumentGenerator, documentIdExtractor,
            letterDocumentFetcher, locationReferenceService, caseNameFormatter, UK_CLOCK);
    }

    @Test
    void shouldResolveCourtThenGenerateAndFetch() {
        PartyEntity claimant = PartyEntity.builder().firstName("Sarah").lastName("Freeman").build();
        PartyEntity recipient = PartyEntity.builder()
            .firstName("Jane").lastName("Doe").nameKnown(VerticalYesNo.YES).build();
        PcsCaseEntity pcsCase = caseWith(42, claimant, recipient);
        AddressUK address = AddressUK.builder().addressLine1("1 High Street").build();
        CourtVenue court = court();
        CoveringLetterPayload payload = CoveringLetterPayload.builder().build();
        UUID letterId = UUID.randomUUID();
        byte[] letter = "letter".getBytes();

        when(locationReferenceService.getCourtVenues(List.of(42))).thenReturn(List.of(court));
        when(caseNameFormatter.formatCaseName(any(), any())).thenReturn("Sarah Freeman vs Jane Doe");
        when(coveringLetterPayloadBuilder.build(
            recipient, "Jane Doe", address, "1234-5678-9012-3456", "Sarah Freeman vs Jane Doe",
            court, LocalDate.of(2026, 10, 15))).thenReturn(payload);
        when(coveringLetterDocumentGenerator.generate(payload)).thenReturn("http://dm-store/documents/letter");
        when(documentIdExtractor.extractDocumentId("http://dm-store/documents/letter")).thenReturn(letterId);
        when(letterDocumentFetcher.fetchBytes(letterId)).thenReturn(letter);

        assertThat(underTest.render(pcsCase, recipient, "Jane Doe", address, "1234-5678-9012-3456"))
            .isSameAs(letter);
    }

    @Test
    void shouldThrowWhenCaseHasNoCourt() {
        PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(123L).build();
        PartyEntity recipient = PartyEntity.builder().build();
        AddressUK address = AddressUK.builder().build();

        assertThatThrownBy(() -> underTest.render(pcsCase, recipient, "Jane Doe", address, "123"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("case management location");
    }

    private PcsCaseEntity caseWith(int epimsId, PartyEntity claimant, PartyEntity defendant) {
        ClaimEntity claim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
                ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).rank(1).build()))
            .build();
        return PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .caseManagementLocation(epimsId)
            .claims(new ArrayList<>(List.of(claim)))
            .build();
    }

    private CourtVenue court() {
        return new CourtVenue(
            null, null, null, null, null, null, null, null, null, null,
            "Town Road",
            "LS1 1AA",
            null, null, null, null, null, null, null, null,
            "Leeds County Court",
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );
    }
}
