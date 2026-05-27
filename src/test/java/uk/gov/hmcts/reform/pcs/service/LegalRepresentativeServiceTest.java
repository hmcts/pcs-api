package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;

    private LegalRepresentativeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeService(legalRepresentativeOrganisationRepository);
    }

    @Test
    void shouldReturnOptionalEmptyWhenIdamIdIsNotLegalRep() {
        // Given
        UUID idamId = UUID.randomUUID();
        when(legalRepresentativeOrganisationRepository.findByIdamId(idamId)).thenReturn(Optional.empty());

        // When
        Optional<DynamicList> dynamicListOptional = underTest.getRepresentedPartiesDynamicList(idamId, CASE_REFERENCE);

        // Then
        assertThat(dynamicListOptional).isEmpty();
    }

    @Test
    void shouldReturnDynamicListWithNoItemsWhenNoPartiesRepesentedOnCase() {
        // Given
        UUID idamId = UUID.randomUUID();

        PcsCaseEntity otherCaseEntity = PcsCaseEntity.builder().caseReference(9876L).build();
        PartyEntity otherCasePartyEntity = PartyEntity.builder().pcsCase(otherCaseEntity).build();
        PartyLegalRepresentativeOrganisationEntity otherCaseClaimPartyEntity = PartyLegalRepresentativeOrganisationEntity.builder()
            .party(otherCasePartyEntity)
            .build();
        LegalRepresentativeOrganisationEntity legalRepEntity = LegalRepresentativeOrganisationEntity.builder()
            .partyLegalRepresentativeOrganisationList(List.of(otherCaseClaimPartyEntity))
            .build();

        when(legalRepresentativeOrganisationRepository.findByIdamId(idamId)).thenReturn(Optional.of(legalRepEntity));

        // When
        Optional<DynamicList> dynamicListOptional = underTest.getRepresentedPartiesDynamicList(idamId, CASE_REFERENCE);

        // Then
        assertThat(dynamicListOptional)
            .hasValueSatisfying(dynamicList -> assertThat(dynamicList.getListItems()).isEmpty());
    }

    @Test
    void shouldReturnDynamicListWithRepresentedPartyNamesForSpecifiedCase() {
        // Given
        UUID partyEntityId = UUID.randomUUID();
        UUID idamId = UUID.randomUUID();

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity casePartyEntity = PartyEntity.builder()
            .id(partyEntityId)
            .pcsCase(caseEntity)
            .firstName("Richard")
            .lastName("Represented")
            .build();
        PartyLegalRepresentativeOrganisationEntity caseClaimPartyEntity = PartyLegalRepresentativeOrganisationEntity.builder()
            .party(casePartyEntity)
            .build();
        LegalRepresentativeOrganisationEntity legalRepEntity = LegalRepresentativeOrganisationEntity.builder()
            .partyLegalRepresentativeOrganisationList(List.of(caseClaimPartyEntity))
            .build();

        when(legalRepresentativeOrganisationRepository.findByIdamId(idamId)).thenReturn(Optional.of(legalRepEntity));

        // When
        Optional<DynamicList> dynamicListOptional = underTest.getRepresentedPartiesDynamicList(idamId, CASE_REFERENCE);

        // Then
        DynamicListElement expectedListValue = DynamicListElement.builder()
            .code(partyEntityId)
            .label("Richard Represented")
            .build();

        assertThat(dynamicListOptional)
            .hasValueSatisfying(
                dynamicList -> assertThat(dynamicList.getListItems())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(expectedListValue)
            );
    }


}
