package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils.ClaimantOrgNameListCreator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimantOrgNameListCreatorTest {

    private ClaimantOrgNameListCreator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClaimantOrgNameListCreator();
    }

    @Test
    void shouldCreateClaimantOrganisationList() {
        // given
        PCSCase pcsCase = PCSCase.builder()
            .allClaimants(List.of(
                ListValue.<Party>builder()
                    .id("1")
                    .value(Party.builder()
                               .orgName("Organisation 1")
                               .build())
                    .build(),
                ListValue.<Party>builder()
                    .id("2")
                    .value(Party.builder()
                               .orgName("Organisation 2")
                               .build())
                    .build()
            )).build();

        // when
        List<ListValue<String>> result = underTest.createClaimantOrgNameList(pcsCase);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getValue()).isEqualTo("Organisation 1");
        assertThat(result.get(1).getValue()).isEqualTo("Organisation 2");
    }

    @Test
    void shouldReturnEmptyClaimantOrganisationListWhenNoClaimants() {
        // given
        PCSCase pcsCase = PCSCase.builder().allClaimants(List.of()).build();

        // when
        List<ListValue<String>> result = underTest.createClaimantOrgNameList(pcsCase);

        // then
        assertThat(result).isEmpty();
    }
}
