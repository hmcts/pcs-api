package uk.gov.hmcts.reform.pcs.ccd.service.hearing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.docassembly.domain.OutputType;
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
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.claimform.ClaimActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentImportService;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentNameService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.document.model.hearingnotice.HearingNoticePayload;
import uk.gov.hmcts.reform.pcs.document.service.DocAssemblyService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingNoticeDocumentGeneratorTest {

    private static final long CASE_REFERENCE = 1234567890123456L;
    private static final String COURT_NAME = "Cardiff Civil and Family Justice Centre";
    private static final String PORTAL_URL = "https://pcs.aat.platform.hmcts.net";
    private static final String EXUI_URL = "https://manage-case.aat.platform.hmcts.net";

    @Mock
    private DocAssemblyService docAssemblyService;
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
    @Captor
    private ArgumentCaptor<FormPayload> payloadCaptor;

    private HearingNoticeDocumentGenerator underTest;

    private final PartyEntity claimant = party();
    private final PartyEntity defendant1 = party();
    private final PartyEntity defendant2 = party();
    private final AddressUK defendantAddress = AddressUK.builder().addressLine1("1 Renters Way")
        .postTown("Cardiff").postCode("CF10 1AA").build();
    private PcsCaseEntity pcsCase;

    @BeforeEach
    void setUp() {
        underTest = new HearingNoticeDocumentGenerator(docAssemblyService, documentImportService, documentNameService,
            claimActivityLogService, new CaseReferenceFormatter(), recipientAddressResolver,
            defenceCorrespondenceAddressResolver, addressMapper, PORTAL_URL, EXUI_URL);

        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(
            ClaimPartyEntity.builder().party(claimant).role(PartyRole.CLAIMANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant1).role(PartyRole.DEFENDANT).rank(1).build(),
            ClaimPartyEntity.builder().party(defendant2).role(PartyRole.DEFENDANT).rank(2).build()
        )).build();
        pcsCase = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).claims(List.of(claim))
            .documents(new ArrayList<>()).build();

        lenient().when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        lenient().when(recipientAddressResolver.resolveDisplayName(defendant1)).thenReturn("Bob Tenant");
        lenient().when(recipientAddressResolver.resolveDisplayName(defendant2)).thenReturn("Jo Tenant");
        lenient().when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(any(), any()))
            .thenReturn(defendantAddress);
        lenient().when(documentNameService.appendPartyPostfix(anyString(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(docAssemblyService.generateDocument(any(), any(), any(), any())).thenReturn("document url");
        lenient().when(documentImportService.addDocumentToCase(any(PcsCaseEntity.class), any(), any()))
            .thenAnswer(invocation -> DocumentEntity.builder().id(UUID.randomUUID()).build());
    }

    @Test
    void shouldDoNothingWhenNoticeIsNotIssued() {
        HearingEntity hearing = hearing(VerticalYesNo.NO, null);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        verifyNoInteractions(docAssemblyService, documentImportService, claimActivityLogService);
    }

    @Test
    void shouldThrowWhenCourtNameIsMissing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        assertThatThrownBy(() -> underTest.generateNotices(pcsCase, hearing, " "))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No hearing location");
    }

    @Test
    void shouldGenerateOneNoticePerPartyForWithNoticeHearing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.NO);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        verify(docAssemblyService).generateDocument(any(), eq(HearingNoticeDocumentGenerator.TEMPLATE_ID),
            eq(OutputType.PDF), eq("Notice of hearing 1"));
        verify(docAssemblyService).generateDocument(any(), any(), any(), eq("Notice of hearing 2"));
        verify(docAssemblyService).generateDocument(any(), any(), any(), eq("Notice of hearing 3"));
        verify(documentImportService, times(3))
            .addDocumentToCase(pcsCase, "document url", CaseFileCategory.ORDERS_AND_NOTICE_OF_HEARINGS);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, claimant);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant1);
        verify(claimActivityLogService).logGenerationSuccess(pcsCase, defendant2);

        assertThat(hearing.getNoticeDocuments()).hasSize(3)
            .allSatisfy(notice -> {
                assertThat(notice.getType()).isEqualTo(DocumentType.HEARING_NOTICE);
                assertThat(notice.getHearing()).isEqualTo(hearing);
            })
            .extracting(DocumentEntity::getParty)
            .containsExactly(claimant, defendant1, defendant2);
    }

    @Test
    void shouldOnlyGenerateNoticesForSelectedPartiesOnWithoutNoticeHearing() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant2);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        assertThat(hearing.getNoticeDocuments()).extracting(DocumentEntity::getParty).containsExactly(defendant2);
    }

    @Test
    void shouldContinueNumberingFromExistingGeneratedNotices() {
        pcsCase.getDocuments().add(DocumentEntity.builder().type(DocumentType.HEARING_NOTICE).build());
        pcsCase.getDocuments().add(DocumentEntity.builder().type(DocumentType.NOTICE_OF_HEARING).build());
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant1);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        verify(docAssemblyService).generateDocument(any(), any(), any(), eq("Notice of hearing 2"));
    }

    @Test
    void shouldBuildDefendantNoticePayload() {
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(defendant2);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        verify(docAssemblyService).generateDocument(payloadCaptor.capture(), any(), any(), any());
        HearingNoticePayload payload = (HearingNoticePayload) payloadCaptor.getValue();
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
        AddressEntity claimantAddress = AddressEntity.builder().addressLine1("1 Firm Street").build();
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, null))
            .thenReturn(claimantAddress);
        when(addressMapper.toAddressUK(claimantAddress))
            .thenReturn(AddressUK.builder().addressLine1("1 Firm Street").build());
        HearingEntity hearing = hearing(VerticalYesNo.YES, VerticalYesNo.YES);
        hearing.addParty(claimant);

        underTest.generateNotices(pcsCase, hearing, COURT_NAME);

        verify(docAssemblyService).generateDocument(payloadCaptor.capture(), any(), any(), any());
        HearingNoticePayload payload = (HearingNoticePayload) payloadCaptor.getValue();
        assertThat(payload.getRecipientAddressLine1()).isEqualTo("1 Firm Street");
        assertThat(payload.getClaimantName()).isEqualTo("Acme Ltd");
        assertThat(payload.getDefendantName()).isEqualTo("Bob Tenant, Jo Tenant");
        assertThat(payload.getUrl()).isEqualTo(EXUI_URL);
    }

    private static HearingEntity hearing(VerticalYesNo issueNotice, VerticalYesNo isWithoutNotice) {
        return HearingEntity.builder()
            .issueNotice(issueNotice)
            .isWithoutNotice(isWithoutNotice)
            .hearingDate(LocalDateTime.of(2026, 8, 15, 15, 40))
            .build();
    }

    private static PartyEntity party() {
        return PartyEntity.builder().id(UUID.randomUUID()).build();
    }
}
