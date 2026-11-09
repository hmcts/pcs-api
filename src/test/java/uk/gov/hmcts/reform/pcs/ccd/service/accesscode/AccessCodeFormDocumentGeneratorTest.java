package uk.gov.hmcts.reform.pcs.ccd.service.accesscode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLanguage;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.accesscode.AccessCodeFormPayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLanguage.ENGLISH;
import static uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLanguage.WELSH;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessCodeFormDocumentGeneratorTest {

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
    private AddressMapper addressMapper;
    @Mock
    private AddressFormatter addressFormatter;
    @Mock
    private CaseReferenceFormatter caseReferenceFormatter;
    @Mock
    private FeatureToggleService featureToggleService;

    private AccessCodeFormDocumentGenerator underTest;

    @Captor
    private ArgumentCaptor<FormPayload> payloadCaptor;

    private final AddressEntity propertyAddressEntity = AddressEntity.builder().build();
    private final AddressUK propertyAddressUk = AddressUK.builder().build();

    @BeforeEach
    void setUp() {
        Clock ukClock = Clock.fixed(Instant.parse("2026-06-10T10:00:00Z"), ZoneId.of("Europe/London"));
        underTest = new AccessCodeFormDocumentGenerator(
            docAssemblyService,
            locationReferenceService,
            addressMapper,
            addressFormatter,
            caseReferenceFormatter,
            new RecipientAddressResolver(),
            ukClock,
            RESPOND_ONLINE_URL,
            featureToggleService
        );

        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(addressMapper.toAddressUK(propertyAddressEntity)).thenReturn(propertyAddressUk);
        when(addressFormatter.formatFullAddressWithoutCountry(propertyAddressUk, AddressFormatter.NEWLINE_DELIMITER))
            .thenReturn(PROPERTY_ADDRESS);
        when(docAssemblyService.generateDocument(any(), anyString(), any(), anyString())).thenReturn(DOC_URL);
        when(locationReferenceService.getCourtVenues(eq(List.of(EPIMS_ID))))
            .thenReturn(List.of(courtVenue("Central London County Court",
                                           "13-14 Park Crescent, Marylebone", "W1B 1HT")));
    }

    @Test
    void shouldUsePersonsUnknownWhenDefendantNameNotProvided() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);

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

        render(caseEntity, defendant, ENGLISH);

        assertThat(capturedPayload().getDefendantName()).isEqualTo("John Doe");
    }

    @Test
    void shouldUsePropertyAddressWhenDefendantAddressUnknown() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);

        AccessCodeFormPayload payload = capturedPayload();
        assertThat(payload.getDefendantAddress()).isEqualTo(PROPERTY_ADDRESS);
        assertThat(payload.getPropertyAddress()).isEqualTo(PROPERTY_ADDRESS);
    }

    @Test
    void shouldUseDefendantAddressWhenKnownAndDifferentToProperty() {
        AddressEntity partyAddressEntity = AddressEntity.builder().build();
        AddressUK partyAddressUk = AddressUK.builder().build();
        when(addressMapper.toAddressUK(partyAddressEntity)).thenReturn(partyAddressUk);
        when(addressFormatter.formatFullAddressWithoutCountry(partyAddressUk, AddressFormatter.NEWLINE_DELIMITER))
            .thenReturn(PARTY_ADDRESS);

        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("Jane").lastName("Roe")
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(partyAddressEntity)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);

        assertThat(capturedPayload().getDefendantAddress()).isEqualTo(PARTY_ADDRESS);
    }

    @Test
    void shouldResolveRespondByPostCourtFromCaseManagementLocation() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);

        AccessCodeFormPayload payload = capturedPayload();
        assertThat(payload.getRespondByPostCourtAddress())
            .isEqualTo("Central London County Court\n13-14 Park Crescent\nMarylebone\nW1B 1HT");
    }

    @Test
    void shouldThrowWhenNoCaseManagementLocation() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);
        caseEntity.setCaseManagementLocation(null);

        assertThatThrownBy(() -> underTest.resolveLetterContext(caseEntity))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("case management location")
            .hasMessageContaining("respond-by-post court");
        verify(docAssemblyService, never()).generateDocument(any(), anyString(), any(), anyString());
    }

    @Test
    void shouldThrowWhenLocationReferenceReturnsNoVenue() {
        when(locationReferenceService.getCourtVenues(eq(List.of(EPIMS_ID))))
            .thenReturn(List.of());

        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        assertThatThrownBy(() -> underTest.resolveLetterContext(caseEntity))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No court venue found")
            .hasMessageContaining("respond-by-post court");
        verify(docAssemblyService, never()).generateDocument(any(), anyString(), any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPlacePlaintextAccessCodeOnPayloadAndUseTemplateAndPdf(boolean release14Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        String result = render(caseEntity, defendant, ENGLISH);

        String expectedTemplateId = release14Enabled
            ? AccessCodeFormDocumentGenerator.TEMPLATE_ID_V2
            : AccessCodeFormDocumentGenerator.TEMPLATE_ID;

        assertThat(result).isEqualTo(DOC_URL);
        assertThat(capturedPayload().getAccessCode()).isEqualTo("PLAINTEXTPIN1");
        assertThat(capturedPayload().getUrl()).isEqualTo(RESPOND_ONLINE_URL);
        verify(docAssemblyService).generateDocument(
            any(),
            eq(expectedTemplateId),
            eq(OutputType.PDF),
            eq("Defendant Access Code " + defendant.getId())
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldIncludeCourtNameOnlyWhenRelease14Enabled(boolean release14Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);

        assertThat(capturedPayload().getCourtName()).isEqualTo(release14Enabled ? "Central London County Court" : null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldUseTheSingleWelshTemplateWhateverRelease14(boolean release14Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, WELSH);

        verify(docAssemblyService).generateDocument(
            any(),
            eq(AccessCodeFormDocumentGenerator.WELSH_TEMPLATE_ID),
            eq(OutputType.PDF),
            eq("Defendant Access Code Welsh " + defendant.getId())
        );
    }

    @Test
    void shouldUseWelshPersonsUnknownOnTheWelshLetter() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, WELSH);

        assertThat(capturedPayload().getDefendantName()).isEqualTo("Unigolion yn anhysbys");
    }

    @Test
    void shouldUseTheDefendantNameOnTheWelshLetterWhenKnown() {
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName("John")
            .lastName("Doe")
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, WELSH);

        assertThat(capturedPayload().getDefendantName()).isEqualTo("John Doe");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldRenderEnglishV1OrV2AndTheWelshTemplateForAWalesDefendant(boolean release14Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);
        render(caseEntity, defendant, WELSH);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        verify(docAssemblyService, times(2)).generateDocument(any(), templateCaptor.capture(), any(), anyString());
        String expectedEnglishTemplateId = release14Enabled
            ? AccessCodeFormDocumentGenerator.TEMPLATE_ID_V2
            : AccessCodeFormDocumentGenerator.TEMPLATE_ID;
        assertThat(templateCaptor.getAllValues())
            .containsExactly(expectedEnglishTemplateId, AccessCodeFormDocumentGenerator.WELSH_TEMPLATE_ID);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldGiveBothLettersTheSameMergeValues(boolean release14Enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(release14Enabled);
        AddressEntity partyAddressEntity = AddressEntity.builder().build();
        AddressUK partyAddressUk = AddressUK.builder().build();
        when(addressMapper.toAddressUK(partyAddressEntity)).thenReturn(partyAddressUk);
        when(addressFormatter.formatFullAddressWithoutCountry(partyAddressUk, AddressFormatter.NEWLINE_DELIMITER))
            .thenReturn(PARTY_ADDRESS);
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .nameKnown(VerticalYesNo.YES)
            .firstName("John").lastName("Doe")
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsProperty(VerticalYesNo.NO)
            .address(partyAddressEntity)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        render(caseEntity, defendant, ENGLISH);
        render(caseEntity, defendant, WELSH);

        verify(docAssemblyService, times(2))
            .generateDocument(payloadCaptor.capture(), anyString(), any(), anyString());
        AccessCodeFormPayload englishPayload = (AccessCodeFormPayload) payloadCaptor.getAllValues().get(0);
        AccessCodeFormPayload welshPayload = (AccessCodeFormPayload) payloadCaptor.getAllValues().get(1);
        assertThat(welshPayload).usingRecursiveComparison().isEqualTo(englishPayload);
        assertThat(welshPayload.getAccessCode()).isEqualTo("PLAINTEXTPIN1");
        assertThat(welshPayload.getDefendantName()).isEqualTo("John Doe");
        assertThat(welshPayload.getDefendantAddress()).isEqualTo(PARTY_ADDRESS);
        assertThat(englishPayload.getCourtName()).isEqualTo(release14Enabled ? "Central London County Court" : null);
    }

    @Test
    void shouldResolveTheCourtAndIssueDateOnceForBothLettersOfAWalesDefendant() {
        PartyEntity defendant = PartyEntity.builder()
            .id(UUID.randomUUID())
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);

        AccessCodeFormDocumentGenerator.LetterContext context = underTest.resolveLetterContext(caseEntity);
        underTest.generate(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1", ENGLISH, context);
        underTest.generate(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1", WELSH, context);

        verify(locationReferenceService, times(1)).getCourtVenues(any());
        assertThat(context.issuedOn()).isEqualTo(LocalDate.of(2026, 6, 10));
        verify(docAssemblyService, times(2)).generateDocument(payloadCaptor.capture(), anyString(), any(), anyString());
        assertThat(payloadCaptor.getAllValues())
            .extracting(payload -> ((AccessCodeFormPayload) payload).getIssuedOn(),
                        payload -> ((AccessCodeFormPayload) payload).getRespondByPostCourtAddress())
            .containsOnly(tuple(LocalDate.of(2026, 6, 10),
                                "Central London County Court\n13-14 Park Crescent\nMarylebone\nW1B 1HT"));
    }

    @Test
    void shouldRenderFromTheGivenContextWithoutLookingUpTheCourtAgain() {
        when(featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_4)).thenReturn(true);
        PartyEntity defendant = PartyEntity.builder()
            .nameKnown(VerticalYesNo.NO)
            .addressKnown(VerticalYesNo.NO)
            .build();
        PcsCaseEntity caseEntity = caseWith(defendant);
        AccessCodeFormDocumentGenerator.LetterContext context = new AccessCodeFormDocumentGenerator.LetterContext(
            courtVenue("Cardiff Civil Justice Centre", "2 Park Street", "CF10 1ET"), LocalDate.of(2026, 11, 5));

        underTest.generate(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1", ENGLISH, context);

        verifyNoInteractions(locationReferenceService);
        AccessCodeFormPayload payload = capturedPayload();
        assertThat(payload.getIssuedOn()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(payload.getRespondByPostCourtAddress())
            .isEqualTo("Cardiff Civil Justice Centre\n2 Park Street\nCF10 1ET");
        assertThat(payload.getCourtName()).isEqualTo("Cardiff Civil Justice Centre");
    }

    @Test
    void shouldNameEachLetterByItsLanguage() {
        UUID partyId = UUID.randomUUID();

        assertThat(AccessCodeFormDocumentGenerator.outputFileName(partyId, DocumentLanguage.ENGLISH))
            .isEqualTo("Defendant Access Code " + partyId);
        assertThat(AccessCodeFormDocumentGenerator.outputFileName(partyId, DocumentLanguage.WELSH))
            .isEqualTo("Defendant Access Code Welsh " + partyId);
    }

    private String render(PcsCaseEntity caseEntity, PartyEntity defendant, DocumentLanguage language) {
        return underTest.generate(caseEntity, caseEntity.getClaims().getFirst(), defendant, "PLAINTEXTPIN1", language,
                                  underTest.resolveLetterContext(caseEntity));
    }

    private AccessCodeFormPayload capturedPayload() {
        verify(docAssemblyService).generateDocument(payloadCaptor.capture(), anyString(), any(), anyString());
        return (AccessCodeFormPayload) payloadCaptor.getValue();
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

    // Builds a CourtVenue with only the fields the letter uses (court name, address, postcode);
    // every other field of the rd-location-ref-api record is left null.
    private CourtVenue courtVenue(String courtName, String courtAddress, String postcode) {
        return new CourtVenue(
            null, null, null, null, null, null, null, null, null, null,
            courtAddress,
            postcode,
            null, null, null, null, null, null, null, null,
            courtName,
            null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );
    }
}
