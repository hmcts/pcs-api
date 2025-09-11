package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsDiscretionaryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoRentArrearsMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.model.NoRentArrearsReasonForGrounds;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimGroundEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class ClaimGroundServiceTest {

    @InjectMocks
    private ClaimGroundService claimGroundService;

    @Test
    void shouldReturnClaimGroundEntitiesForMandatoryAndDiscretionaryGrounds() {
        ClaimEntity claim = ClaimEntity.builder().build();
        NoRentArrearsReasonForGrounds grounds = NoRentArrearsReasonForGrounds.builder()
            .ownerOccupierTextArea("Owner occupier reason")
            .repossessionByLenderTextArea("Repossession reason")
            .rentArrearsTextArea("Rent arrears reason")
            .falseStatementTextArea("False statement reason")
            .build();

        Set<NoRentArrearsMandatoryGrounds> mandatory = Set.of(
            NoRentArrearsMandatoryGrounds.OWNER_OCCUPIER,
            NoRentArrearsMandatoryGrounds.REPOSSESSION_BY_LENDER,
            NoRentArrearsMandatoryGrounds.HOLIDAY_LET // no text, should be skipped
        );

        Set<NoRentArrearsDiscretionaryGrounds> discretionary = Set.of(
            NoRentArrearsDiscretionaryGrounds.RENT_ARREARS,
            NoRentArrearsDiscretionaryGrounds.FALSE_STATEMENT,
            NoRentArrearsDiscretionaryGrounds.NUISANCE_OR_ILLEGAL_USE // no text, skipped
        );

        List<ClaimGroundEntity> entities = claimGroundService.getGroundsWithReason(
            mandatory,
            discretionary,
            grounds,
            claim
        );

        // Check size
        assertThat(entities.size()).isEqualTo(4);

        // Check mandatory grounds using streams
        assertThat(entities.stream()
                       .anyMatch(e -> e.getGroundsId().equals("OWNER_OCCUPIER")
                           && e.getClaimsReasonText().equals("Owner occupier reason"))).isTrue();

        assertThat(entities.stream()
                       .anyMatch(e -> e.getGroundsId().equals("REPOSSESSION_BY_LENDER")
                           && e.getClaimsReasonText().equals("Repossession reason"))).isTrue();

        // Check discretionary grounds using streams
        assertThat(entities.stream()
                       .anyMatch(e -> e.getGroundsId().equals("RENT_ARREARS")
                           && e.getClaimsReasonText().equals("Rent arrears reason"))).isTrue();

        assertThat(entities.stream()
                       .anyMatch(e -> e.getGroundsId().equals("FALSE_STATEMENT")
                           && e.getClaimsReasonText().equals("False statement reason"))).isTrue();
    }
}

