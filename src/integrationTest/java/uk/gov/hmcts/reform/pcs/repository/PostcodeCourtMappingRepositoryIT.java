package uk.gov.hmcts.reform.pcs.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.pcs.PostgresContainerTest;
import uk.gov.hmcts.reform.pcs.entity.PostcodeCourtMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Sql("/postcode-court-mappings.sql")
public class PostcodeCourtMappingRepositoryIT extends PostgresContainerTest {

    @Autowired
    private PostcodeCourtMappingRepository underTest;

    @Test
    void shouldCreateHearing() {
        List<PostcodeCourtMapping> allMappings = underTest.findAll();

        assertThat(allMappings).hasSize(2);
    }

}
