package uk.gov.hmcts.reform.pcs.ccd.view.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GroundsBuilderTest {

    private GroundsBuilder groundsBuilder;

    @BeforeEach
    void setUp() {
        groundsBuilder = new GroundsBuilder();
    }

    @Test
    void shouldGroupSection84AConditionsUnderAntisocialBehaviourInGrounds() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Antisocial behaviour", "Antisocial reason"),
                groundSummary("Nuisance, annoyance, illegal or immoral use of the property (ground 2)",
                              "Nuisance reason"),
                groundSummary("Condition 3 of Section 84A of the Housing Act 1985", "Condition 3 reason"),
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Condition 5 of Section 84A of the Housing Act 1985", "Condition 5 reason"),
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 4 of Section 84A of the Housing Act 1985", "Condition 4 reason"),
                groundSummary("Landlord’s works (ground 10)", "Works reason")
            ))
            .build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isEqualTo(String.join(
            "\n",
            "Antisocial behaviour: " + String.join(
                ", ",
                       "Condition 1 of Section 84A of the Housing Act 1985",
                       "Condition 2 of Section 84A of the Housing Act 1985",
                       "Condition 3 of Section 84A of the Housing Act 1985",
                       "Condition 4 of Section 84A of the Housing Act 1985",
                       "Condition 5 of Section 84A of the Housing Act 1985"
            ),
            "Nuisance, annoyance, illegal or immoral use of the property (ground 2)",
            "Landlord’s works (ground 10)"
        ));
    }

    @Test
    void shouldGroupSection84AConditionsUnderAntisocialBehaviourWhenParentGroundIsMissing() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Condition 2 of Section 84A of the Housing Act 1985", "Condition 2 reason"),
                groundSummary("Condition 1 of Section 84A of the Housing Act 1985", "Condition 1 reason"),
                groundSummary("Landlord’s works (ground 10)", "Works reason")
            ))
            .build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isEqualTo(String.join(
            "\n",
            "Antisocial behaviour: " + String.join(
                ", ",
               "Condition 1 of Section 84A of the Housing Act 1985",
               "Condition 2 of Section 84A of the Housing Act 1985"
            ),
            "Landlord’s works (ground 10)"
        ));
    }

    @Test
    void shouldNotReturnSection84AConditionsIfNotPresent() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Landlord’s works (ground 10)", "Works reason")
            ))
            .build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isEqualTo("Landlord’s works (ground 10)");
    }

    @Test
    void shouldGroupEstateManagementGroundsUnderEstateManagementGrounds() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Estate management grounds (section 160)", "Estate reason"),
                groundSummary("Reserve successors (ground G)", "Ground G reason"),
                groundSummary("Building works (ground A)", "Ground A reason"),
                groundSummary("Other breach of contract (section 157)", "Other breach reason"),
                groundSummary("Charities (ground C)", "Ground C reason")
            ))
            .build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isEqualTo(String.join(
            "\n",
            "Estate management grounds (section 160): " + String.join(
                ", ",
                "Building works (ground A)",
                "Charities (ground C)",
                "Reserve successors (ground G)"
            ),
            "Other breach of contract (section 157)"
        ));
    }

    @Test
    void shouldGroupEstateManagementGroundsWhenParentGroundIsMissing() {
        // Given
        PCSCase pcsCase = PCSCase.builder()
            .claimGroundSummaries(List.of(
                groundSummary("Other estate management reasons (ground I)", "Ground I reason"),
                groundSummary("Redevelopment schemes (ground B)", "Ground B reason")
            ))
            .build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isEqualTo(
            "Estate management grounds (section 160): "
                + "Redevelopment schemes (ground B), Other estate management reasons (ground I)"
        );
    }

    @Test
    void shouldReturnZeroWhenEstateManagementGroundLabelIsUnknown() throws Exception {
        // Given
        Method method = GroundsBuilder.class.getDeclaredMethod("getEstateManagementGroundNumber", String.class);
        method.setAccessible(true);

        // When
        int groundNumber = (int) method.invoke(groundsBuilder, "Unknown ground");

        // Then
        assertThat(groundNumber).isZero();
    }

    @Test
    void shouldReturnNullWhenThereAreNoGroundSummaries() {
        PCSCase pcsCase = PCSCase.builder().build();

        // When
        String grounds = groundsBuilder.getGrounds(pcsCase);

        // Then
        assertThat(grounds).isNull();
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
