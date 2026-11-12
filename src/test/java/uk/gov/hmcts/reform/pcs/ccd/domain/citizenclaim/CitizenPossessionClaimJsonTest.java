package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.PrivateLandlordAssuredGround;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitizenPossessionClaimJsonTest {

    private final ObjectMapper objectMapper = CitizenPossessionClaimFixtures.draftObjectMapper();

    @Test
    void shouldReadTheFrontendShapedClaim() {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();

        assertThat(claim.getEligibility().getClaimantType()).isEqualTo(ClaimantType.PRIVATE_LANDLORD);
        assertThat(claim.getClaimant().getContactByPhone()).isEqualTo(VerticalYesNo.YES);
        assertThat(claim.getDefendants()).hasSize(2);
        assertThat(claim.getGrounds().getRentArrearsGrounds())
            .containsExactlyInAnyOrder(PrivateLandlordAssuredGround.GROUND_8, PrivateLandlordAssuredGround.GROUND_10);
        assertThat(claim.getNotice().getNoticeMethods())
            .containsExactlyInAnyOrder(CitizenClaimNoticeMethod.FIRST_CLASS_POST, CitizenClaimNoticeMethod.EMAIL);

        CitizenClaimRentArrears rentArrears = claim.getRentArrears();
        assertThat(rentArrears.getRentAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(rentArrears.getRentFrequency()).isEqualTo(RentPaymentFrequency.MONTHLY);
        assertThat(rentArrears.getRentHistoryLedger()).hasSize(2);
        assertThat(rentArrears.getRentHistoryLedger().getFirst().getValue().getTransactionTypes())
            .containsExactly(CitizenClaimRentTransactionType.RENT_CHARGE);
        assertThat(rentArrears.getProofType()).isEqualTo(CitizenClaimRentProofType.LEDGER);
        assertThat(rentArrears.getRentHistoryLedger().get(1).getValue().getTransactionDate())
            .isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(rentArrears.getRentHistoryTable()).isNull();
    }

    @Test
    void shouldWriteMoneyAsPenceAndEnumsByName() throws Exception {
        PCSCase pcsCase = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaimFixtures.completeClaim())
            .build();

        JsonNode json = objectMapper.valueToTree(pcsCase).get("citizenPossessionClaim");

        assertThat(json.at("/rentArrears/currentArrears").asText()).isEqualTo("500000");
        assertThat(json.at("/rentArrears/rentHistoryLedger/0/value/amountCharged").asText()).isEqualTo("125000");
        assertThat(json.at("/tenancy/tenancyType").asText()).isEqualTo("ASSURED");
        assertThat(json.at("/tenancy/tenancyStartDate").asText()).isEqualTo("2023-04-01");
        assertThat(json.at("/submission/statementOfTruthAgreed").asText()).isEqualTo("YES");
    }

    @Test
    void shouldRoundTripThroughTheDraftStore() throws Exception {
        PCSCase pcsCase = PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaimFixtures.completeClaim())
            .build();

        String stored = objectMapper.writeValueAsString(pcsCase);
        PCSCase restored = objectMapper.readValue(stored, PCSCase.class);

        assertThat(restored.getCitizenPossessionClaim()).isEqualTo(pcsCase.getCitizenPossessionClaim());
    }

    @Test
    void shouldRoundTripDefendantsConfirmedThroughTheDraftStore() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setDefendantsConfirmed(VerticalYesNo.YES);
        PCSCase pcsCase = PCSCase.builder().citizenPossessionClaim(claim).build();

        String stored = objectMapper.writeValueAsString(pcsCase);
        PCSCase restored = objectMapper.readValue(stored, PCSCase.class);

        assertThat(objectMapper.readTree(stored).at("/citizenPossessionClaim/defendantsConfirmed").asText())
            .isEqualTo("YES");
        assertThat(restored.getCitizenPossessionClaim().getDefendantsConfirmed()).isEqualTo(VerticalYesNo.YES);
    }

    @Test
    void shouldRoundTripConfirmedSectionsAsTheFrontendStepNames() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaimFixtures.completeClaim();
        claim.setConfirmedSections(Set.of(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY));
        PCSCase pcsCase = PCSCase.builder().citizenPossessionClaim(claim).build();

        String stored = objectMapper.writeValueAsString(pcsCase);
        PCSCase restored = objectMapper.readValue(stored, PCSCase.class);

        assertThat(objectMapper.readTree(stored).at("/citizenPossessionClaim/confirmedSections"))
            .extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("claimant-cya", "tenancy-cya");
        assertThat(restored.getCitizenPossessionClaim().getConfirmedSections())
            .containsExactlyInAnyOrder(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY);
    }

    @Test
    void shouldStoreAnEmptiedConfirmedSectionsAsAnEmptyArray() throws Exception {
        CitizenPossessionClaim claim = CitizenPossessionClaim.builder().confirmedSections(Set.of()).build();
        PCSCase pcsCase = PCSCase.builder().citizenPossessionClaim(claim).build();

        String stored = objectMapper.writeValueAsString(pcsCase);

        assertThat(objectMapper.readTree(stored).at("/citizenPossessionClaim/confirmedSections").isArray()).isTrue();
        assertThat(objectMapper.readValue(stored, PCSCase.class).getCitizenPossessionClaim().getConfirmedSections())
            .isEmpty();
    }

}
