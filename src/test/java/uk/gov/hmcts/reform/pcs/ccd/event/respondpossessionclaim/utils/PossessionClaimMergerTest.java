package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PossessionClaimMergerTest {

    @Mock
    private ClaimantOrgNameListCreator claimantOrgNameListCreator;

    private PossessionClaimMerger possessionClaimMerger;

    @BeforeEach
    void setUp() {
        possessionClaimMerger = new PossessionClaimMerger(claimantOrgNameListCreator);
    }

    @Test
    void shouldMergeLatestCaseData() {
        // given
        PCSCase latestCase = PCSCase.builder().build();

        List<ListValue<String>> claimantOrganisations = List.of(ListValue.<String>builder()
                                                                    .id("1")
                                                                    .value("Claimant Organisation")
                                                                    .build());

        PossessionClaimResponse savedResponses = PossessionClaimResponse.builder().build();
        UUID defendantId = UUID.randomUUID();

        when(claimantOrgNameListCreator.createClaimantOrgNameList(latestCase)).thenReturn(claimantOrganisations);

        // when
        PossessionClaimResponse result = possessionClaimMerger.mergeLatestCaseData(latestCase, savedResponses,
                                                                                   defendantId);

        // then
        assertThat(result.getClaimantOrganisations()).isEqualTo(claimantOrganisations);
        assertThat(result.getCurrentDefendantPartyId()).isEqualTo(defendantId.toString());

        verify(claimantOrgNameListCreator).createClaimantOrgNameList(latestCase);
    }

    @Test
    void shouldNotSetCurrentDefendantPartyIdWhenIdIsNull() {
        // given
        PCSCase latestCase = PCSCase.builder().build();

        List<ListValue<String>> claimantOrganisations = List.of(ListValue.<String>builder()
                                                                    .id("1")
                                                                    .value("Claimant Organisation")
                                                                    .build());

        PossessionClaimResponse savedResponses = PossessionClaimResponse.builder().build();

        when(claimantOrgNameListCreator.createClaimantOrgNameList(latestCase)).thenReturn(claimantOrganisations);

        // when
        PossessionClaimResponse result = possessionClaimMerger.mergeLatestCaseData(latestCase, savedResponses, null);

        // then
        assertThat(result.getClaimantOrganisations()).isEqualTo(claimantOrganisations);
        assertThat(result.getCurrentDefendantPartyId()).isNull();

        verify(claimantOrgNameListCreator).createClaimantOrgNameList(latestCase);
    }
}
