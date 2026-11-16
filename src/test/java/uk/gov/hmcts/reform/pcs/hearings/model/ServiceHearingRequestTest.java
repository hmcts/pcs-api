package uk.gov.hmcts.reform.pcs.hearings.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceHearingRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptSixteenDigitCaseReference() {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId("1234567890123456")
            .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void shouldRejectMissingCaseReference() {
        Set<ConstraintViolation<ServiceHearingRequest>> violations =
            validator.validate(new ServiceHearingRequest());

        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("caseReference is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789012345", "12345678901234567", "123456789012345a", "1234-5678-9012-3456"})
    void shouldRejectCaseReferenceThatIsNotSixteenDigits(String caseReference) {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId(caseReference)
            .build();

        assertThat(validator.validate(request))
            .extracting(ConstraintViolation::getMessage)
            .containsExactly("caseReference must be a 16-digit case reference");
    }

    @Test
    void shouldRejectBlankCaseReferenceWithBothMessages() {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId("   ")
            .build();

        assertThat(validator.validate(request))
            .extracting(ConstraintViolation::getMessage)
            .containsExactlyInAnyOrder(
                "caseReference is required",
                "caseReference must be a 16-digit case reference");
    }
}
