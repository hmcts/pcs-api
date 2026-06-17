package uk.gov.hmcts.reform.pcs.ccd.service.genapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.PinPackFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PinPackDocumentGeneratorTest {

    private static final String AUTH_TOKEN = "Bearer system-token";
    private static final Integer EPIMS_ID = 12345;
    private static final String DOC_URL = "http://dm-store/documents/pin";
    private static final String PROPERTY_ADDRESS = "1 Property Street\nLondon\nAB1 2CD";
    private static final String PARTY_ADDRESS = "5 Tenant Road\nLeeds\nLS1 1AA";
    private static final String RESPOND_ONLINE_URL = "https://pcs.aat.platform.hmcts.net/access-your-case";

    @Mock
    private DocAssemblyService docAssemblyService;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private CaseReferenceFormatter caseReferenceFormatter;

    private PinPackDocumentGenerator underTest;

    @Captor
    private ArgumentCaptor<FormPayload> payloadCaptor;

    private final AddressEntity propertyAddressEntity = AddressEntity.builder().build();
    private final AddressUK propertyAddressUk = AddressUK.builder().build();

    @BeforeEach
    void setUp() {
        Clock ukClock = Clock.fixed(Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Europe/London"));
        underTest = new PinPackDocumentGenerator(
            docAssemblyService,
            locationReferenceService,
            systemUpdateUserTokenProvider,
            addressMapper,
            addressFormatter,
            caseReferenceFormatter,
            ukClock,
            RESPOND_ONLINE_URL
        );

        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn(AUTH_TOKEN);
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(addressMapper.toAddressUK(propertyAddressEntity)).thenReturn(propertyAddressUk);
        when(addressFormatter.formatFullAddress(propertyAddressUk, AddressFormatter.NEWLINE_DELIMITER))
            .thenReturn(PROPERTY_ADDRESS);
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DOC_URL);
        when(locationReferenceService.getCountyCourts(eq(AUTH_TOKEN), eq(List.of(EPIMS_ID))))
            .thenReturn(List.of(new CourtVenue(EPIMS_ID, 1, "Central London County Court",
                                               "13-14 Park Crescent", "W1B 1HT")));
    }

    @Test
    void shouldUsePersonsUnknownWhenDefendantNameNotProvided() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1");

        assertThat(capturedPayload().getDefendantName()).isEqualTo("Persons unknown");
    }

    @Test
    void shouldUseDefendantNameWhenProvided() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1");

        assertThat(capturedPayload().getDefendantName()).isEqualTo("John Doe");
    }

    @Test
    void shouldUsePropertyAddressWhenDefendantAddressUnknown() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1");

        PinPackFormPayload payload = capturedPayload();
        assertThat(payload.getDefendantAddress()).isEqualTo(PROPERTY_ADDRESS);
        assertThat(payload.getPropertyAddress()).isEqualTo(PROPERTY_ADDRESS);
    }

    @Test
    void shouldUseDefendantAddressWhenKnownAndDifferentToProperty() {
        AddressEntity partyAddressEntity = AddressEntity.builder().build();
        AddressUK partyAddressUk = AddressUK.builder().build();
        when(addressMapper.toAddressUK(partyAddressEntity)).thenReturn(partyAddressUk);
        when(addressFormatter.formatFullAddress(partyAddressUk, AddressFormatter.NEWLINE_DELIMITER))
            .thenReturn(PARTY_ADDRESS);

        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("Jane").lastName("Roe")
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(partyAddressEntity)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1");

        assertThat(capturedPayload().getDefendantAddress()).isEqualTo(PARTY_ADDRESS);
    }

    @Test
    void shouldResolveRespondByPostCourtFromCaseManagementLocation() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1");

        PinPackFormPayload payload = capturedPayload();
        assertThat(payload.getRespondByPostCourtName()).isEqualTo("Central London County Court");
        assertThat(payload.getRespondByPostCourtAddress()).isEqualTo("13-14 Park Crescent\nW1B 1HT");
    }

    @Test
    void shouldThrowWhenNoCaseManagementLocation() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);
        caseEntity.setCaseManagementLocation(null);

        assertThatThrownBy(() ->
            underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("case management location")
            .hasMessageContaining("AC06");
        verify(docAssemblyService, never()).generateDocument(any(), anyString(), any(), anyString());
    }

    @Test
    void shouldThrowWhenLocationReferenceReturnsNoVenue() {
        when(locationReferenceService.getCountyCourts(eq(AUTH_TOKEN), eq(List.of(EPIMS_ID))))
            .thenReturn(List.of());

        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        assertThatThrownBy(() ->
            underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No court venue found")
            .hasMessageContaining("AC06");
        verify(docAssemblyService, never()).generateDocument(any(), anyString(), any(), anyString());
    }

    @Test
    void shouldPlacePlaintextAccessCodeOnPayloadAndUseTemplateAndPdf() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        String result = underTest.generatePinPack(caseEntity, caseEntity.getClaims().getFirst(),
                                                  defendant, "PLAINTEXTPIN1");

        assertThat(result).isEqualTo(DOC_URL);
        assertThat(capturedPayload().getAccessCode()).isEqualTo("PLAINTEXTPIN1");
        assertThat(capturedPayload().getUrl()).isEqualTo(RESPOND_ONLINE_URL);
        verify(docAssemblyService).generateDocument(any(),
            eq(PinPackDocumentGenerator.PIN_PACK_TEMPLATE_ID), eq(OutputType.PDF), anyString());
    }

    private PinPackFormPayload capturedPayload() {
        verify(docAssemblyService).generateDocument(payloadCaptor.capture(), anyString(), any(), anyString());
        return (PinPackFormPayload) payloadCaptor.getValue();
    }

    private PcsCaseEntity caseWith(PartyEntity defendant) {
        PartyEntity claimant = PartyEntity.builder().nameKnown(VerticalYesNo.YES)
            .firstName("Acme").lastName("Landlord").build();

        ClaimEntity mainClaim = ClaimEntity.builder()
            .claimParties(List.of(
                ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).build(),
                ClaimPartyEntity.builder().party(defendant).role(PartyRole.DEFENDANT).build()
            ))
            .build();

        return PcsCaseEntity.builder()
            .caseReference(1234567890123456L)
            .caseManagementLocation(EPIMS_ID)
            .propertyAddress(propertyAddressEntity)
            .claims(List.of(mainClaim))
            .build();
    }
}
