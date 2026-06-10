package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.shared.ReasonsForPossessionTabDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReasonsForPossessionTabDetailsBuilderTest {

    private ReasonsForPossessionTabDetailsBuilder reasonsForPossessionTabDetailsBuilder;

    @BeforeEach
    void setUp() {
        reasonsForPossessionTabDetailsBuilder = new ReasonsForPossessionTabDetailsBuilder();
    }

    @Test
    void shouldSetEachGroundReason() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                listValue(ClaimGroundSummary.builder()
                              .label("Antisocial behaviour")
                              .reason("Antisocial reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Premium paid in connection with mutual exchange (ground 6)")
                              .reason("Premium reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Offence during a riot (ground 2ZA)")
                              .reason("Riot reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Condition 1 of Section 84A of the Housing Act 1985")
                              .reason("Condition 1 reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Landlord’s works (ground 10)")
                              .reason("Works reason")
                              .build()),
                listValue(ClaimGroundSummary.builder()
                              .label("Housing association special circumstances accommodation (ground 14)")
                              .reason("Housing reason")
                              .build())
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getAntisocialBehaviour()).isEqualTo("Antisocial reason");
        assertThat(reasons.getGround6()).isEqualTo("Premium reason");
        assertThat(reasons.getGround2ZA()).isEqualTo("Riot reason");
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getGround10()).isEqualTo("Works reason");
        assertThat(reasons.getGround14()).isEqualTo("Housing reason");
    }

    @Test
    void shouldSetEachGroundNumberReason() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Ground label (ground 1)", "Reason 1"),
                groundSummary("Ground label (ground 2)", "Reason 2"),
                groundSummary("Ground label (ground 2A)", "Reason 2A"),
                groundSummary("Ground label (ground 2ZA)", "Reason 2ZA"),
                groundSummary("Ground label (ground 3)", "Reason 3"),
                groundSummary("Ground label (ground 4)", "Reason 4"),
                groundSummary("Ground label (ground 5)", "Reason 5"),
                groundSummary("Ground label (ground 6)", "Reason 6"),
                groundSummary("Ground label (ground 7)", "Reason 7"),
                groundSummary("Ground label (ground 7A)", "Reason 7A"),
                groundSummary("Ground label (ground 7B)", "Reason 7B"),
                groundSummary("Ground label (ground 8)", "Reason 8"),
                groundSummary("Ground label (ground 9)", "Reason 9"),
                groundSummary("Ground label (ground 10)", "Reason 10"),
                groundSummary("Ground label (ground 10A)", "Reason 10A"),
                groundSummary("Ground label (ground 11)", "Reason 11"),
                groundSummary("Ground label (ground 12)", "Reason 12"),
                groundSummary("Ground label (ground 13)", "Reason 13"),
                groundSummary("Ground label (ground 14)", "Reason 14"),
                groundSummary("Ground label (ground 14A)", "Reason 14A"),
                groundSummary("Ground label (ground 14ZA)", "Reason 14ZA"),
                groundSummary("Ground label (ground 15)", "Reason 15"),
                groundSummary("Ground label (ground 15A)", "Reason 15A"),
                groundSummary("Ground label (ground 16)", "Reason 16"),
                groundSummary("Ground label (ground 17)", "Reason 17"),
                groundSummary("Ground label (ground A)", "Reason A"),
                groundSummary("Ground label (ground B)", "Reason B"),
                groundSummary("Ground label (ground C)", "Reason C"),
                groundSummary("Ground label (ground D)", "Reason D"),
                groundSummary("Ground label (ground E)", "Reason E"),
                groundSummary("Ground label (ground F)", "Reason F"),
                groundSummary("Ground label (ground G)", "Reason G"),
                groundSummary("Ground label (ground H)", "Reason H"),
                groundSummary("Ground label (ground I)", "Reason I"),
                groundSummary("Ground label (ground Z)", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getGround1()).isEqualTo("Reason 1");
        assertThat(reasons.getGround2()).isEqualTo("Reason 2");
        assertThat(reasons.getGround2A()).isEqualTo("Reason 2A");
        assertThat(reasons.getGround2ZA()).isEqualTo("Reason 2ZA");
        assertThat(reasons.getGround3()).isEqualTo("Reason 3");
        assertThat(reasons.getGround4()).isEqualTo("Reason 4");
        assertThat(reasons.getGround5()).isEqualTo("Reason 5");
        assertThat(reasons.getGround6()).isEqualTo("Reason 6");
        assertThat(reasons.getGround7()).isEqualTo("Reason 7");
        assertThat(reasons.getGround7A()).isEqualTo("Reason 7A");
        assertThat(reasons.getGround7B()).isEqualTo("Reason 7B");
        assertThat(reasons.getGround8()).isEqualTo("Reason 8");
        assertThat(reasons.getGround9()).isEqualTo("Reason 9");
        assertThat(reasons.getGround10()).isEqualTo("Reason 10");
        assertThat(reasons.getGround10A()).isEqualTo("Reason 10A");
        assertThat(reasons.getGround11()).isEqualTo("Reason 11");
        assertThat(reasons.getGround12()).isEqualTo("Reason 12");
        assertThat(reasons.getGround13()).isEqualTo("Reason 13");
        assertThat(reasons.getGround14()).isEqualTo("Reason 14");
        assertThat(reasons.getGround14A()).isEqualTo("Reason 14A");
        assertThat(reasons.getGround14ZA()).isEqualTo("Reason 14ZA");
        assertThat(reasons.getGround15()).isEqualTo("Reason 15");
        assertThat(reasons.getGround15A()).isEqualTo("Reason 15A");
        assertThat(reasons.getGround16()).isEqualTo("Reason 16");
        assertThat(reasons.getGround17()).isEqualTo("Reason 17");
        assertThat(reasons.getGroundA()).isEqualTo("Reason A");
        assertThat(reasons.getGroundB()).isEqualTo("Reason B");
        assertThat(reasons.getGroundC()).isEqualTo("Reason C");
        assertThat(reasons.getGroundD()).isEqualTo("Reason D");
        assertThat(reasons.getGroundE()).isEqualTo("Reason E");
        assertThat(reasons.getGroundF()).isEqualTo("Reason F");
        assertThat(reasons.getGroundG()).isEqualTo("Reason G");
        assertThat(reasons.getGroundH()).isEqualTo("Reason H");
        assertThat(reasons.getGroundI()).isEqualTo("Reason I");
    }

    @Test
    void shouldSetEachSectionReason() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Ground label (section 157)", "Reason 157"),
                groundSummary("Ground label (section 170)", "Reason 170"),
                groundSummary("Ground label (section 178)", "Reason 178"),
                groundSummary("Ground label (section 181)", "Reason 181"),
                groundSummary("Ground label (section 186)", "Reason 186"),
                groundSummary("Ground label (section 187)", "Reason 187"),
                groundSummary("Ground label (section 191)", "Reason 191"),
                groundSummary("Ground label (section 199)", "Reason 199"),
                groundSummary("Ground label (section 999)", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getSection157()).isEqualTo("Reason 157");
        assertThat(reasons.getSection170()).isEqualTo("Reason 170");
        assertThat(reasons.getSection178()).isEqualTo("Reason 178");
        assertThat(reasons.getSection181()).isEqualTo("Reason 181");
        assertThat(reasons.getSection186()).isEqualTo("Reason 186");
        assertThat(reasons.getSection187()).isEqualTo("Reason 187");
        assertThat(reasons.getSection191()).isEqualTo("Reason 191");
        assertThat(reasons.getSection199()).isEqualTo("Reason 199");
    }

    @Test
    void shouldSetNonNumberedGroundReasons() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 3 of Section 84A of the Housing Act 1985", "Condition 3 reason"),
                groundSummary("Condition 4 of Section 84A of the Housing Act 1985", "Condition 4 reason"),
                groundSummary("Condition 5 of Section 84A of the Housing Act 1985", "Condition 5 reason"),
                groundSummary("Antisocial behaviour", "Antisocial reason"),
                groundSummary("Breach of the tenancy", "Breach reason"),
                groundSummary("Absolute grounds", "Absolute reason"),
                groundSummary("Other", "Other reason"),
                groundSummary("Other grounds", "Other grounds reason"),
                groundSummary("No grounds", "No grounds reason"),
                groundSummary("Converted contract paragraph 25B(2) of Schedule 12", "Paragraph reason"),
                groundSummary("Unmapped label", "Unmapped reason")
            ))
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getCondition2OfSection84A()).isEqualTo("Condition 2 reason");
        assertThat(reasons.getCondition3OfSection84A()).isEqualTo("Condition 3 reason");
        assertThat(reasons.getCondition4OfSection84A()).isEqualTo("Condition 4 reason");
        assertThat(reasons.getCondition5OfSection84A()).isEqualTo("Condition 5 reason");
        assertThat(reasons.getAntisocialBehaviour()).isEqualTo("Antisocial reason");
        assertThat(reasons.getBreachOfTheTenancy()).isEqualTo("Breach reason");
        assertThat(reasons.getAbsoluteGrounds()).isEqualTo("Absolute reason");
        assertThat(reasons.getOtherGrounds()).isEqualTo("Other grounds reason");
        assertThat(reasons.getNoGrounds()).isEqualTo("No grounds reason");
        assertThat(reasons.getParagraph25B2Schedule12()).isEqualTo("Paragraph reason");
    }

    @Test
    void shouldSetAdditionalReasonInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason")
            ))
            .additionalReasonsForPossession(
                AdditionalReasons.builder()
                    .hasReasons(VerticalYesNo.YES)
                    .reasons("Additional reason")
                    .build()
            )
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getHasAdditionalReasons()).isNull();
        assertThat(reasons.getAdditionalReasonsDetails()).isEqualTo("Additional reason");
    }

    @Test
    void shouldSetAdditionalReasonInDetailsTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason")
            ))
            .additionalReasonsForPossession(
                AdditionalReasons.builder()
                    .hasReasons(VerticalYesNo.YES)
                    .reasons("Additional reason")
                    .build()
            )
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildDetailsReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getCondition1OfSection84A()).isEqualTo("Condition 1 reason");
        assertThat(reasons.getHasAdditionalReasons()).isEqualTo("Yes");
        assertThat(reasons.getAdditionalReasonsDetails()).isEqualTo("Additional reason");
    }

    @Test
    void shouldReturnNullWhenThereIsNoClaimSummariesInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons).isNull();
    }

    @Test
    void shouldReturnNullWhenThereIsNoClaimSummariesInDetailsTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildDetailsReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons).isNull();
    }

    @Test
    void shouldReturnReasonForPossessionDetailsWhenThereIsOnlyAdditionalReasonsInSummaryTab() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .additionalReasonsForPossession(
                AdditionalReasons.builder()
                    .hasReasons(VerticalYesNo.YES)
                    .reasons("Additional reason")
                    .build()
            )
            .build();

        // When
        ReasonsForPossessionTabDetails reasons = reasonsForPossessionTabDetailsBuilder
            .buildSummaryReasonsForPossession(pcsCase);

        // Then
        assertThat(reasons.getAdditionalReasonsDetails()).isEqualTo("Additional reason");
    }

    private static <T> ListValue<T> listValue(T value) {
        return ListValue.<T>builder()
            .value(value)
            .build();
    }



    private static ListValue<ClaimGroundSummary> groundSummary(String label, String reason) {
        return listValue(ClaimGroundSummary.builder()
                             .label(label)
                             .reason(reason)
                             .build());
    }
}
