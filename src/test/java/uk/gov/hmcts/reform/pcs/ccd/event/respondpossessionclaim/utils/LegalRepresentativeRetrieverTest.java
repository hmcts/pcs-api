package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.LegalRepresentativeOrganisationRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeRetrieverTest {
    @Mock
    private LegalRepresentativeOrganisationRepository legalRepresentativeOrganisationRepository;
    @InjectMocks
    private LegalRepresentativeRetriever legalRepresentativeRetriever;

    @Test
    void getLegalRepOrganisationIdForUser_LegalRepOrgFound_ReturnsID() {
        // given
        long caseReference = 0;
        String organisationId = "org";
        UUID id = UUID.randomUUID();
        LegalRepresentativeOrganisationEntity legalRepresentativeOrganisationEntity =
            LegalRepresentativeOrganisationEntity.builder().id(id).build();
        when(legalRepresentativeOrganisationRepository
                         .findByOrganisationIdAndCaseReference(organisationId,caseReference)).thenReturn(
            Optional.ofNullable(legalRepresentativeOrganisationEntity));

        // when
        UUID legalRepOrganisationIdForUser = legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(
            caseReference,
            organisationId
        );

        // then
        assertEquals(id,legalRepOrganisationIdForUser);
    }

    @Test
    void getLegalRepOrganisationIdForUser_LegalRepOrgNotFound_ThrowsException() {
        // given
        long caseReference = 0;
        String organisationId = "org";
        when(legalRepresentativeOrganisationRepository
                         .findByOrganisationIdAndCaseReference(organisationId,caseReference))
            .thenReturn(Optional.empty());

        // when / then
        assertThat(assertThrows(
            IllegalStateException.class,
            () -> legalRepresentativeRetriever.getLegalRepOrganisationIdForUser(caseReference,organisationId)
        )).hasMessage("Legal Representative Organisation not linked to case or party");
    }

}
