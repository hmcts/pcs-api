package uk.gov.hmcts.reform.pcs.testingsupport.bulkprint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ClaimPartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.BulkPrintService;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.LetterType;
import uk.gov.hmcts.reform.pcs.ccd.service.bulkprint.ResolvedRecipient;
import uk.gov.hmcts.reform.pcs.ccd.service.form.RecipientAddressResolver;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestBulkPrintSenderTest {

    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private RecipientAddressResolver recipientAddressResolver;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private BulkPrintService bulkPrintService;
    @InjectMocks
    private TestBulkPrintSender underTest;

    private final PartyEntity claimant = PartyEntity.builder().id(UUID.randomUUID()).build();
    private final PartyEntity defendant = PartyEntity.builder().id(UUID.randomUUID()).build();

    @Test
    void shouldAddressTheLetterOfUploadsToTheFirstClaimant() {
        AddressEntity propertyAddress = AddressEntity.builder().build();
        AddressEntity claimantAddress = AddressEntity.builder().build();
        AddressUK postal = AddressUK.builder().postCode("NW1 6XE").build();
        PcsCaseEntity pcsCase = caseWith(propertyAddress,
            claimParty(defendant, PartyRole.DEFENDANT), claimParty(claimant, PartyRole.CLAIMANT));
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCase));
        when(recipientAddressResolver.resolveDisplayName(claimant)).thenReturn("Claire Claimant");
        when(recipientAddressResolver.resolvePostalAddress(claimant, PartyRole.CLAIMANT, propertyAddress))
            .thenReturn(claimantAddress);
        when(addressMapper.toAddressUK(claimantAddress)).thenReturn(postal);
        UUID photo = UUID.randomUUID();

        ResolvedRecipient resolved = underTest.resolveClaimant(new TestBulkPrintTaskData(CASE_REFERENCE,
            List.of(new TestBulkPrintTaskData.Upload(photo, "photo.jpg"))));

        assertThat(resolved.recipient()).isSameAs(claimant);
        assertThat(resolved.letterType()).isEqualTo(LetterType.TEST_SUPPORT_PACK);
        assertThat(resolved.recipientName()).isEqualTo("Claire Claimant");
        assertThat(resolved.address()).isSameAs(postal);
        assertThat(resolved.documents()).singleElement().satisfies(document -> {
            assertThat(document.getDocumentId()).isEqualTo(photo);
            assertThat(document.getFileName()).isEqualTo("photo.jpg");
        });
    }

    @Test
    void shouldRefuseACaseThatDoesNotExistOrHasNoClaimant() {
        TestBulkPrintTaskData request = new TestBulkPrintTaskData(CASE_REFERENCE, List.of());
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> underTest.resolveClaimant(request)).hasMessageContaining("No case");

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(
            caseWith(null, claimParty(defendant, PartyRole.DEFENDANT))));
        assertThatThrownBy(() -> underTest.resolveClaimant(request)).hasMessageContaining("no claimant");
    }

    @Test
    void shouldPostTheResolvedLetterThroughBulkPrint() {
        PcsCaseEntity pcsCase = caseWith(null, claimParty(claimant, PartyRole.CLAIMANT));
        AddressUK address = AddressUK.builder().build();
        ResolvedRecipient resolved = new ResolvedRecipient(pcsCase, claimant, LetterType.TEST_SUPPORT_PACK,
            List.of(), "Claire Claimant", address);
        UUID letterId = UUID.randomUUID();
        when(bulkPrintService.sendPack(pcsCase, claimant, LetterType.TEST_SUPPORT_PACK, "Claire Claimant",
            address, List.of())).thenReturn(letterId);

        assertThat(underTest.post(resolved)).isEqualTo(letterId);
        verify(bulkPrintService).sendPack(pcsCase, claimant, LetterType.TEST_SUPPORT_PACK, "Claire Claimant",
            address, List.of());
    }

    private static PcsCaseEntity caseWith(AddressEntity propertyAddress, ClaimPartyEntity... claimParties) {
        ClaimEntity claim = ClaimEntity.builder().claimParties(List.of(claimParties)).build();
        return PcsCaseEntity.builder()
            .caseReference(CASE_REFERENCE)
            .propertyAddress(propertyAddress)
            .claims(List.of(claim))
            .build();
    }

    private static ClaimPartyEntity claimParty(PartyEntity party, PartyRole role) {
        return ClaimPartyEntity.builder().party(party).role(role).rank(1).build();
    }
}
