package uk.gov.hmcts.reform.pcs.postalcode.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.audit.Audit;
import uk.gov.hmcts.reform.pcs.postalcode.domain.PostCode;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PostalCodeRepositoryTest {

    @Autowired
    private PostalCodeRepository postalCodeRepository;

    @Test
    @DisplayName("Test finding a postcode by value")
    void testFindByPostcode() {
        // Given
        String postcode = "W3 7RX";
        int epimid = 20262;
        final PostCode postCodeToSave = getPostcode(epimid, postcode);
        postalCodeRepository.save(postCodeToSave);

        // When
        Optional<PostCode> found = postalCodeRepository.findByPostCode(postcode);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEpimId()).isEqualTo(epimid);
        assertThat(found.get().getPostCode()).isEqualTo(postcode);
    }

    private PostCode getPostcode(int epimid, String postcode) {
        PostCode postCodeToSave = new PostCode();
        postCodeToSave.setEpimId(epimid);
        postCodeToSave.setPostCode(postcode);
        postCodeToSave.setAudit(new Audit());
        postCodeToSave.setLegislativeCountry("England");
        postCodeToSave.setEffectiveFrom(LocalDateTime.now());
        postCodeToSave.setEffectiveTo(LocalDateTime.now().plusMonths(1));
        return postCodeToSave;
    }

}
