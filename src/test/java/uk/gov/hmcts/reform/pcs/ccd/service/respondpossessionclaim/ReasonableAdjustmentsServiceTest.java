package uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ReasonableAdjustments;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.ReasonableAdjustmentEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReasonableAdjustmentsServiceTest {

    private ReasonableAdjustmentsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ReasonableAdjustmentsService();
    }

    @Test
    void shouldMapReasonableAdjustmentsRequiredField() {
        //Given
        ReasonableAdjustments model = ReasonableAdjustments.builder()
            .reasonableAdjustmentRequired("Wheelchair access")
            .build();

        //When
        ReasonableAdjustmentEntity entity = underTest.createReasonableAdjustmentEntity(model);

        //Then
        assertThat(entity.getReasonableAdjustmentsRequired()).isEqualTo("Wheelchair access");
    }

}

