package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.PartyLegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeOrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalRepresentativeSummaryServiceTest {

    @InjectMocks
    private LegalRepresentativeSummaryService legalRepresentativeSummaryService;

    @Mock
    private SecurityContextService securityContextService;

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndActive_ReturnsMarkDown() {
        // given
        UUID userId = UUID.randomUUID();
        Set<PartyEntity> parties = Set.of(PartyEntity.builder()
                                            .partyLegalRepresentativeOrganisationList(List.of(
                                                PartyLegalRepresentativeOrganisationEntity.builder()
                                                    .active(YesOrNo.YES)
                                                    .legalRepresentative(LegalRepresentativeOrganisationEntity.builder()
                                                                             .idamId(userId)
                                                                             .build())
                                                    .build()))
                                            .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(parties)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isNotNull();
    }

    @Test
    void handleLegalRepresentativeSummary_WithLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        UUID userId = UUID.randomUUID();
        Set<PartyEntity> parties = Set.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .legalRepresentative(LegalRepresentativeOrganisationEntity.builder()
                                                                               .idamId(userId)
                                                                               .build())
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(parties)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndActive_ReturnsEmptyMarkDown() {
        // given
        UUID userId = UUID.randomUUID();
        Set<PartyEntity> parties = Set.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.YES)
                                                      .legalRepresentative(LegalRepresentativeOrganisationEntity.builder()
                                                                               .idamId(UUID.randomUUID())
                                                                               .build())
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(parties)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

    @Test
    void handleLegalRepresentativeSummary_WithNotLinkedAndNotActive_ReturnsEmptyMarkDown() {
        // given
        UUID userId = UUID.randomUUID();
        Set<PartyEntity> parties = Set.of(PartyEntity.builder()
                                              .partyLegalRepresentativeOrganisationList(List.of(
                                                  PartyLegalRepresentativeOrganisationEntity.builder()
                                                      .active(YesOrNo.NO)
                                                      .legalRepresentative(LegalRepresentativeOrganisationEntity.builder()
                                                                               .idamId(UUID.randomUUID())
                                                                               .build())
                                                      .build()))
                                              .build());

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder()
            .parties(parties)
            .build();

        PCSCase pcsCase = PCSCase.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(userId);

        // when
        legalRepresentativeSummaryService.handleLegalRepresentativeSummary(pcsCase, pcsCaseEntity);

        // then
        assertThat(pcsCase.getSummaryLegalRepresentativeMarkdown()).isEmpty();
    }

}
