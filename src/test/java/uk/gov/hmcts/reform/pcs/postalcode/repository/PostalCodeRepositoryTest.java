package uk.gov.hmcts.reform.pcs.postalcode.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pcs.postalcode.domain.Postcode;

import java.time.LocalDate;
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
        final Postcode postcodeToSave = getPostcode(epimid, postcode);
        postalCodeRepository.save(postcodeToSave);

        // When
        Optional<Postcode> found = postalCodeRepository.findByPostcode(postcode);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEpimid()).isEqualTo(epimid);
        assertThat(found.get().getPostcode()).isEqualTo(postcode);
    }

    private Postcode getPostcode(int epimid, String postcode) {
        Postcode postcodeToSave = new Postcode();
        postcodeToSave.setEpimid(epimid);
        postcodeToSave.setPostcode(postcode);
        postcodeToSave.setAudit("123");
        postcodeToSave.setLegislativeCountry("UK");
        postcodeToSave.setEffectiveFrom(LocalDate.now());
        postcodeToSave.setEffectiveTo(LocalDate.now().plusMonths(1));
        return postcodeToSave;
    }

}
