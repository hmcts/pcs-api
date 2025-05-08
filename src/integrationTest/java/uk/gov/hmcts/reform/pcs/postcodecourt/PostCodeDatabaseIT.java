package uk.gov.hmcts.reform.pcs.postcodecourt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("integration")
class PostCodeDatabaseIT extends AbstractPostgresContainerIT {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void shouldRemoveSpacesAndMakeUpperCase() {
        List<String> postcodesFromDb = jdbcClient.sql("SELECT postcode FROM postcode_court_mapping")
            .query(String.class)
            .list();

        assertThat(postcodesFromDb)
            .contains("TST19BC", "TST29BC", "TST39BC");
    }

}
