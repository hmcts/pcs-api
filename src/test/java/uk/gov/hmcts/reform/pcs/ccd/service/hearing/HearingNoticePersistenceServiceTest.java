package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.hearing.HearingEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.HearingRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;
import uk.gov.hmcts.reform.pcs.location.model.CourtVenue;
import uk.gov.hmcts.reform.pcs.location.service.LocationReferenceService;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingNoticePersistenceServiceTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final int HEARING_ID = 7;
    private static final int BASE_LOCATION = 20262;
    private static final String COURT_NAME = "Cardiff Civil and Family Justice Centre";
    private static final String PORTAL_URL = "https://pcs.aat.platform.hmcts.net";

    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private LocationReferenceService locationReferenceService;
    @Mock
    private DocumentImportService documentImportService;
    @Mock
    private DocumentNameService documentNameService;
    @Mock
    private ClaimActivityLogService claimActivityLogService;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private NotificationService notificationService;

    private HearingNoticePersistenceService underTest;

    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private final AddressUK defendantAddress = AddressUK.builder().addressLine1("1 Renters Way")
        .postTown("Cardiff").postCode("CF10 1AA").build();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new HearingNoticePersistenceService(hearingRepository, locationReferenceService,
            documentImportService, documentNameService, claimActivityLogService, new CaseReferenceFormatter(),
            recipientAddressResolver, defenceCorrespondenceAddressResolver, addressMapper, notificationService,
            PORTAL_URL);
        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build()
        )).build();
        pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).claims(List.of(claim))
            .baseLocation(BASE_LOCATION).documents(new ArrayList<>()).build();

        CourtVenue courtVenue = mock(CourtVenue.class);
        lenient().when(courtVenue.courtName()).thenReturn(COURT_NAME);
        lenient().when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION)))
            .thenReturn(List.of(courtVenue));
        lenient().when(documentNameService.appendPartyPostfix(anyString(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), any(), any()))
            .thenAnswer(invocation -> DocumentEntity.builder().id(UUID.randomUUID()).build());
    }

    @Test
    void shouldBuildNoContextsWhenNoticeIsNotIssued() {
        HearingEntity hearing = hearing(VerticalYesNo.NO, null);

        assertThat(buildNoticeContexts(hearing)).isEmpty();
        verifyNoInteractions(locationReferenceService);
    }

    @Test
    void shouldThrowWhenNoCourtVenueIsFound() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);
        when(locationReferenceService.getCourtVenues(List.of(BASE_LOCATION))).thenReturn(List.of());

        assertThatThrownBy(() -> buildNoticeContexts(hearing))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No court venue found");
    }

    @Test
    void shouldThrowWhenCaseHasNoBaseLocation() {
        pcsCase.setBaseLocation(null);
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        assertThatThrownBy(() -> buildNoticeContexts(hearing))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No base location");
        verifyNoInteractions(locationReferenceService);
    }

    @Test
    void shouldBuildOneContextPerPartyForWithNoticeHearing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        List<HearingNoticeRenderContext> contexts = buildNoticeContexts(hearing);

        assertThat(contexts).extracting(HearingNoticeRenderContext::partyId)
            .containsExactly(claimant.getId(), defendant1.getId(), defendant2.getId());
        assertThat(contexts).extracting(HearingNoticeRenderContext::filename).containsOnly("Notice of hearing 1");
    }

    @Test
    void shouldOnlyBuildContextsForSelectedPartiesOnWithoutNoticeHearing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant2);

        assertThat(buildNoticeContexts(hearing)).extracting(HearingNoticeRenderContext::partyId)
            .containsExactly(defendant2.getId());
    }

    @Test
    void shouldNameNoticesWithTheHearingsNoticeNumber() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.setNoticeNumber(3);
        hearing.addParty(defendant1);

        assertThat(buildNoticeContexts(hearing)).extracting(HearingNoticeRenderContext::filename)
            .containsExactly("Notice of hearing 3");
    }

    @Test
    void shouldBuildDefendantNoticePayload() {
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolveDisplayName(defendant1)).thenReturn("Bob Tenant");
        when(recipientAddressResolver.resolveDisplayName(defendant2)).thenReturn("Jo Tenant");
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant2, null))
            .thenReturn(defendantAddress);
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant2);

        HearingNoticePayload payload = buildNoticeContexts(hearing).getFirst().payload();

        assertThat(payload.getRecipientName()).isEqualTo("Jo Tenant");
        assertThat(payload.getRecipientAddressLine1()).isEqualTo("1 Renters Way");
        assertThat(payload.getRecipientPostTown()).isEqualTo("Cardiff");
        assertThat(payload.getRecipientPostcode()).isEqualTo("CF10 1AA");
        assertThat(payload.isHasAddressLine2()).isFalse();
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getDefendantName()).isEqualTo("Jo Tenant");
        assertThat(payload.getCourtName()).isEqualTo(COURT_NAME);
        assertThat(payload.getHearingTime()).isEqualTo("3:40pm");
        assertThat(payload.getHearingDate()).isEqualTo("15th August 2026");
        assertThat(payload.getUrl()).isEqualTo(PORTAL_URL);
    }

    @Test
    void shouldBuildClaimantNoticePayload() {
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolveDisplayName(defendant1)).thenReturn("Bob Tenant");
        when(recipientAddressResolver.resolveDisplayName(defendant2)).thenReturn("Jo Tenant");
        AddressEntity claimantAddress = AddressEntity.builder().addressLine1("1 Firm Street").build();
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, null))
            .thenReturn(claimantAddress);
        when(addressMapper.toAddressUK(claimantAddress))
            .thenReturn(AddressUK.builder().addressLine1("1 Firm Street").build());
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(claimant);

        HearingNoticePayload payload = buildNoticeContexts(hearing).getFirst().payload();

        assertThat(payload.getRecipientAddressLine1()).isEqualTo("1 Firm Street");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getDefendantName()).isEqualTo("Bob Tenant, Jo Tenant");
        assertThat(payload.getUrl()).isEqualTo(PORTAL_URL);
    }

    @Test
    void shouldAttachEachNoticeToTheHearingAndItsRecipient() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        when(hearingRepository.findById(HEARING_ID)).thenReturn(Optional.of(hearing));
        underTest.attachNoticesAndQueueEmails(HEARING_ID, documentUrls(claimant, defendant1, defendant2));

        verify(documentImportService)
            .addDocumentToCase(pcsCase, url(claimant), CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        verify(documentImportService)
            .addDocumentToCase(pcsCase, url(defendant2), CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant1);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant2);

        assertThat(hearing.getNoticeDocuments()).hasSize(3)
            .allSatisfy(notice -> {
                assertThat(notice.getType()).isEqualTo(DocumentType.NOTICE_OF_HEARING);
                assertThat(notice.getHearing()).isEqualTo(hearing);
            })
            .extracting(DocumentEntity::getParty)
            .containsExactly(claimant, defendant1, defendant2);
    }

    @Test
    void shouldEmailEveryRecipientWithTheirRole() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        when(hearingRepository.findById(HEARING_ID)).thenReturn(Optional.of(hearing));
        underTest.attachNoticesAndQueueEmails(HEARING_ID, documentUrls(claimant, defendant1, defendant2));

        verify(notificationService).sendHearingNoticeEmail(pcsCase, claimant, PartyRole.CLAIMANT);
        verify(notificationService).sendHearingNoticeEmail(pcsCase, defendant1, PartyRole.DEFENDANT);
        verify(notificationService).sendHearingNoticeEmail(pcsCase, defendant2, PartyRole.DEFENDANT);
    }

    @Test
    void shouldThrowWhenHearingIsNotFound() {
        when(hearingRepository.findById(HEARING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.buildNoticeContexts(HEARING_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No hearing found for id: " + HEARING_ID);
    }

    @Test
    void shouldBuildNoContextsWhenHearingIsCancelled() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);
        hearing.setCancelled(true);

        assertThat(buildNoticeContexts(hearing)).isEmpty();
        verifyNoInteractions(locationReferenceService);
    }

    @Test
    void shouldSkipRemovedPartiesOnWithoutNoticeHearing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant1);
        hearing.addParty(defendant2);
        defendant1.setRemoved(true);

        assertThat(buildNoticeContexts(hearing)).extracting(HearingNoticeRenderContext::partyId)
            .containsExactly(defendant2.getId());
    }

    private List<HearingNoticeRenderContext> buildNoticeContexts(HearingEntity hearing) {
        when(hearingRepository.findById(HEARING_ID)).thenReturn(Optional.of(hearing));
        return underTest.buildNoticeContexts(HEARING_ID);
    }

    private static Map<UUID, String> documentUrls(PartyEntity... parties) {
        Map<UUID, String> documentUrls = new LinkedHashMap<>();
        for (PartyEntity party : parties) {
            documentUrls.put(party.getId(), url(party));
        }
        return documentUrls;
    }

    private static String url(PartyEntity party) {
        return "document url " + party.getId();
    }

    private HearingEntity hearing(VerticalYesNo issueNotice, VerticalYesNo isWithoutNotice) {
        return HearingEntity.builder()
            .id(HEARING_ID)
            .pcsCase(pcsCase)
            .noticeNumber(1)
            .issueNotice(issueNotice)
            .isWithoutNotice(isWithoutNotice)
            .hearingDate(LocalDateTime.of(2026, 8, 15, 15, 40))
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
