package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.claimactivitylog.ClaimActivityType;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClaimPackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private ClaimPackCandidateService claimPackCandidateService;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    @InjectMocks
    private ClaimPackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity recipient = PartyEntity.builder().id(UUID.randomUUID()).build();

    @Test
    void doesNothingWhenCaseNotFound() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.empty());

        underTest.sendClaimPacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, claimPackCandidateService);
    }

    @Test
    void resolvesAddressAndSendsClaimantPack() {
        AddressEntity postalAddress = AddressEntity.builder().addressLine1("1 High Street").build();
        AddressUK addressUk = AddressUK.builder().addressLine1("1 High Street").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(claimPackCandidateService.findClaimPackCandidates(pcsCase))
            .thenReturn(List.of(new PackCandidate(PartyRole.CLAIMANT, recipient, List.of())));
        when(recipientAddressResolver.resolveDisplayName(recipient)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolvePostalAddress(recipient, PartyRole.CLAIMANT, pcsCase.getPropertyAddress()))
            .thenReturn(postalAddress);
        when(addressMapper.toAddressUK(postalAddress)).thenReturn(addressUk);
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendClaimPacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, recipient,
            LetterType.CLAIMANT_CLAIM_PACK, "Acme Ltd", addressUk, List.of());
        verify(accessCodeActivityLogService)
            .logSuccessInNewTransaction(pcsCase, recipient, ClaimActivityType.CLAIMANT_PACK_SENT);
        verify(accessCodeActivityLogService, never()).logFailure(any(), any(), any());
    }

    @Test
    void recordsFailureWhenSendThrowsMissingAddress() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(claimPackCandidateService.findClaimPackCandidates(pcsCase))
            .thenReturn(List.of(new PackCandidate(PartyRole.DEFENDANT, recipient, List.of())));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException("no address"));

        underTest.sendClaimPacks(CASE_ID);

        verify(accessCodeActivityLogService)
            .logFailure(pcsCase, recipient, ClaimActivityType.DEFENDANT_PACK_SENT);
    }
}
