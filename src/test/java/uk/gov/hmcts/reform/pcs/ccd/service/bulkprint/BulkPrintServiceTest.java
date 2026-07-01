package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReferenceFormatter;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DocumentIdExtractor;
import uk.gov.hmcts.reform.pcs.document.model.coversheet.CoversheetPayload;
import uk.gov.hmcts.reform.pcs.security.IdamTokenProvider;
import uk.gov.hmcts.reform.sendletter.api.SendLetterApi;
import uk.gov.hmcts.reform.sendletter.api.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.api.model.v3.LetterV3;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulkPrintServiceTest {

    private static final UUID LETTER_ID = UUID.randomUUID();

    @Mock
    private CoversheetPayloadBuilder coversheetPayloadBuilder;
    @Mock
    private CoversheetDocumentGenerator coversheetDocumentGenerator;
    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private SendLetterApi sendLetterApi;
    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private IdamTokenProvider systemUpdateUserTokenProvider;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;
    @Mock
    private DocumentIdExtractor documentIdExtractor;
    @Mock
    private CaseReferenceFormatter caseReferenceFormatter;

    private BulkPrintService underTest;

    @Captor
    private ArgumentCaptor<LetterV3> letterCaptor;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        underTest = new BulkPrintService(coversheetPayloadBuilder, coversheetDocumentGenerator, caseDocumentClientApi,
            sendLetterApi, authTokenGenerator, systemUpdateUserTokenProvider, accessCodeActivityLogService,
            documentIdExtractor, caseReferenceFormatter);

        when(authTokenGenerator.generate()).thenReturn("s2s");
        when(systemUpdateUserTokenProvider.getAuthToken()).thenReturn("user-token");
        when(caseReferenceFormatter.formatCaseReferenceWithDashes(any())).thenReturn("1234-5678-9012-3456");
        when(coversheetPayloadBuilder.build(any(), any(), any())).thenReturn(CoversheetPayload.builder().build());
        when(coversheetDocumentGenerator.generate(any())).thenReturn("http://dm-store/documents/cover");
        when(documentIdExtractor.extractDocumentId(any())).thenReturn(UUID.randomUUID());
        when(caseDocumentClientApi.getDocumentBinary(any(), any(), any()))
            .thenReturn(ResponseEntity.ok(new ByteArrayResource("pdf".getBytes())));
        when(sendLetterApi.sendLetter(any(), any(LetterV3.class))).thenReturn(new SendLetterResponse(LETTER_ID));
    }

    @Test
    void sendsPackWithCoversheetPrependedAndLogsSuccess() {
        DocumentEntity claimForm = DocumentEntity.builder().documentId(UUID.randomUUID()).build();
        AddressUK address = AddressUK.builder()
            .addressLine1("1 High Street").postTown("London").postCode("W1 1AA").build();

        UUID letterId = underTest.sendPack(pcsCase, recipient, ClaimActivityType.CLAIMANT_PACK_SENT,
            LetterType.CLAIMANT_CLAIM_PACK, "Jane Doe", address, List.of(claimForm));

        assertThat(letterId).isEqualTo(LETTER_ID);
        verify(sendLetterApi).sendLetter(eq("s2s"), letterCaptor.capture());
        LetterV3 letter = letterCaptor.getValue();
        assertThat(letter.type).isEqualTo("CPC-01-IN1_pcs_api");
        assertThat(letter.documents).hasSize(2);            // coversheet + claim form
        assertThat(letter.additionalData)
            .containsEntry("caseReference", "1234-5678-9012-3456").containsKey("recipients");
        verify(caseDocumentClientApi, times(2)).getDocumentBinary(eq("user-token"), eq("s2s"), any());
        verify(accessCodeActivityLogService)
            .logSuccess(pcsCase, recipient, ClaimActivityType.CLAIMANT_PACK_SENT);
    }

    @Test
    void throwsAndSendsNothingWhenNoPostalAddress() {
        AddressUK blank = AddressUK.builder().build();

        assertThatThrownBy(() -> underTest.sendPack(pcsCase, recipient, ClaimActivityType.DEFENDANT_PACK_SENT,
            LetterType.DEFENDANT_CLAIM_PACK, "Jane Doe", blank, List.of()))
            .isInstanceOf(MissingPostalAddressException.class);

        verifyNoInteractions(sendLetterApi);
        verify(accessCodeActivityLogService, never()).logSuccess(any(), any(), any());
    }
}
