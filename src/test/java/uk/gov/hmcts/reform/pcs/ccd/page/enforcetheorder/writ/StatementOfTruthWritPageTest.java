package uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.writ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class StatementOfTruthWritPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new StatementOfTruthWritPage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "writStatementOfTruthWrit-certification-details-suspended | enforcementIsSuspendedOrder=\"Yes\"",
        "writStatementOfTruthWrit-certification-details           | enforcementIsSuspendedOrder!=\"Yes\""
    })
    void shouldCertifyWhatTheLatestIssuedOrderWas(String labelId, String showCondition) {
        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build))
            .filteredOn(field -> labelId.equals(field.getId()))
            .singleElement()
            .satisfies(field -> assertThat(field.getShowCondition()).isEqualTo(showCondition));
    }
}
