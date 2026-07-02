package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.AccessCodeActivityLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.form.DefenceCorrespondenceAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefencePackSenderTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private DefencePackSelector defencePackSelector;
    @Mock
    private DefenceCorrespondenceAddressResolver defenceCorrespondenceAddressResolver;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private AccessCodeActivityLogService accessCodeActivityLogService;

    @InjectMocks
    private DefencePackSender underTest;

    private final PcsCaseEntity pcsCase = PcsCaseEntity.builder().caseReference(1234567890123456L).build();
    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity defenceForm = DocumentEntity.builder().id(UUID.randomUUID()).build();
    private final DocumentEntity counterClaim = DocumentEntity.builder().id(UUID.randomUUID()).build();

    @Test
    @DisplayName("Does nothing when the case is not found")
    void shouldDoNothingWhenCaseNotFound() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.empty());

        underTest.sendDefencePacks(CASE_ID);

        verifyNoInteractions(bulkPrintService, defencePackSelector);
    }

    @Test
    @DisplayName("Sends a defendant their defence via the correspondence address and records each document")
    void shouldSendDefendantDefenceAndRecordEachDocument() {
        AddressUK address = AddressUK.builder().addressLine1("42 Renters Way").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase)).thenReturn(List.of(
            new DefencePackCandidate(PartyRole.DEFENDANT, defendant, List.of(defenceForm, counterClaim))));
        when(recipientAddressResolver.resolveDisplayName(defendant)).thenReturn("Bob Tenant");
        when(defenceCorrespondenceAddressResolver.resolveCorrespondenceAddress(defendant, pcsCase.getPropertyAddress()))
            .thenReturn(address);
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendDefencePacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, defendant, LetterType.DEFENCE_PACK, "Bob Tenant", address,
            List.of(defenceForm, counterClaim));
        verify(accessCodeActivityLogService).recordDocumentSent(pcsCase, defendant, defenceForm);
        verify(accessCodeActivityLogService).recordDocumentSent(pcsCase, defendant, counterClaim);
    }

    @Test
    @DisplayName("Serves the counter-claim on the claimant using the claimant address")
    void shouldServeCounterClaimOnClaimant() {
        AddressEntity postalAddress = AddressEntity.builder().addressLine1("1 Landlord Lane").build();
        AddressUK address = AddressUK.builder().addressLine1("1 Landlord Lane").build();
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase)).thenReturn(List.of(
            new DefencePackCandidate(PartyRole.CLAIMANT, claimant, List.of(counterClaim))));
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Acme Ltd");
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, pcsCase.getPropertyAddress()))
            .thenReturn(postalAddress);
        when(addressMapper.toAddressUK(postalAddress)).thenReturn(address);
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

        underTest.sendDefencePacks(CASE_ID);

        verify(bulkPrintService).sendPack(pcsCase, claimant, LetterType.DEFENCE_PACK, "Acme Ltd", address,
            List.of(counterClaim));
        verify(accessCodeActivityLogService).recordDocumentSent(pcsCase, claimant, counterClaim);
    }

    @Test
    @DisplayName("Records a document-send failure when the send throws a missing address")
    void shouldRecordFailureWhenSendThrowsMissingAddress() {
        when(pcsCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(pcsCase));
        when(defencePackSelector.findDefencePackCandidates(pcsCase)).thenReturn(List.of(
            new DefencePackCandidate(PartyRole.DEFENDANT, defendant, List.of(defenceForm))));
        when(bulkPrintService.sendPack(any(), any(), any(), any(), any(), any()))
            .thenThrow(new MissingPostalAddressException("no address"));

        underTest.sendDefencePacks(CASE_ID);

        verify(accessCodeActivityLogService).recordDocumentSendFailure(pcsCase, defendant, defenceForm);
    }
}
