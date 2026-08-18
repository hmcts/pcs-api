package uk.gov.hmcts.reform.pcs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.DynamicListElement;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeServiceTest {

    private static final long CASE_REFERENCE = 1234L;

    @Mock
    private PartyRepository partyRepository;

    private LegalRepresentativeService underTest;

    @BeforeEach
    void setUp() {
        underTest = new LegalRepresentativeService(partyRepository);
    }

    @Test
    void shouldReturnOptionalEmptyWhenOrgIdIsNotLegalRep() {
        // Given
        String orgId = "org";
        when(partyRepository.findAllPartiesByOrganisationIdAndCaseReference(orgId, CASE_REFERENCE))
            .thenReturn(List.of());

        // When
        DynamicList dynamicList = underTest.getRepresentedPartiesDynamicList(orgId, CASE_REFERENCE);

        // Then
        assertThat(dynamicList.getListItems()).isEmpty();
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

        when(partyRepository.findAllPartiesByOrganisationIdAndCaseReference(orgId, CASE_REFERENCE))
            .thenReturn(List.of(casePartyEntity));

        // When
        DynamicList dynamicList = underTest.getRepresentedPartiesDynamicList(orgId, CASE_REFERENCE);

        // Then
        DynamicListElement expectedListValue = DynamicListElement.builder()
            .code(partyEntityId)
            .label("Richard Represented")
            .build();

        assertThat(dynamicList.getListItems())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(expectedListValue);
    }

}
