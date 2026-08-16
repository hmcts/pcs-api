package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private OrganisationRepository organisationRepository;

    private LegalRepresentativeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeService(organisationRepository);
    }

    @Test
    void shouldReturnOptionalEmptyWhenIdamIdIsNotLegalRep() {
        // Given
        String orgId = "org";
        when(organisationRepository.findByOrganisationIdAndCaseReference(orgId, CASE_REFERENCE))
            .thenReturn(Optional.empty());

        // When
        Optional<DynamicList> dynamicListOptional = underTest.getRepresentedPartiesDynamicList(orgId, CASE_REFERENCE);

        // Then
        assertThat(dynamicListOptional).isEmpty();
    }

    @Test
    void shouldReturnDynamicListWithRepresentedPartyNamesForSpecifiedCase() {
        // Given
        UUID partyEntityId = UUID.randomUUID();
        String orgId = "org";

        PcsCaseEntity caseEntity = PcsCaseEntity.builder().caseReference(CASE_REFERENCE).build();
        PartyEntity casePartyEntity = PartyEntity.builder()
            .id(partyEntityId)
            .pcsCase(caseEntity)
            .firstName("Richard")
            .lastName("Represented")
            .build();
        ClaimPartyOrganisationEntity casePartyLegalRepOrgEntity =
            ClaimPartyOrganisationEntity.builder()
                .party(casePartyEntity)
                .build();
        OrganisationEntity legalRepEntity = OrganisationEntity.builder()
            .claimPartyOrganisationList(List.of(casePartyLegalRepOrgEntity))
            .build();

        when(organisationRepository.findByOrganisationIdAndCaseReference(orgId, CASE_REFERENCE))
            .thenReturn(Optional.of(legalRepEntity));

        // When
        Optional<DynamicList> dynamicListOptional = underTest.getRepresentedPartiesDynamicList(orgId, CASE_REFERENCE);

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
