package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.entity.PostCodeCourtEntity;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class PostCodeCourtService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final PartialPostcodesGenerator partialPostcodesGenerator;
    private final Clock ukClock;

    public PostCodeCourtService(PostCodeCourtRepository postCodeCourtRepository,
                                PartialPostcodesGenerator partialPostcodesGenerator,
                                @Qualifier("ukClock") Clock ukClock) {

        this.postCodeCourtRepository = postCodeCourtRepository;
        this.partialPostcodesGenerator = partialPostcodesGenerator;
        this.ukClock = ukClock;
    }

    public Integer getCourtManagementLocation(String postCode) {
        return getCourtManagementLocation(postCode, null);
    }

    public Integer getCourtManagementLocation(String postCode, LegislativeCountry legislativeCountry) {
        List<PostCodeCourtEntity> results = getPostcodeCourtMappings(postCode, legislativeCountry);
        if (results.isEmpty()) {
            log.error("EpimId not found for postcode: {}", postCode);
            return null;
        }
        log.info("Court management location allocated for postcode {}", postCode);
        return results.getFirst().getId().getEpimsId();
    }

    private List<PostCodeCourtEntity> getPostcodeCourtMappings(String postcode,
                                                               LegislativeCountry legislativeCountry) {

        List<String> postcodes = partialPostcodesGenerator.generateForPostcode(postcode);

        LocalDate currentDate = LocalDate.now(ukClock);
        List<PostCodeCourtEntity> results = legislativeCountry == null
            ? postCodeCourtRepository.findActiveByPostCodeIn(postcodes, currentDate)
            : postCodeCourtRepository.findActiveByPostCodeIn(postcodes, legislativeCountry, currentDate);
        if (results.isEmpty()) {
            log.warn("Postcode court mapping not found for postcode {}", postcode);
            return List.of();
        }

        String longestPostcodeMatch = results.stream()
            .map(e -> e.getId().getPostCode())
            .max(Comparator.comparingInt(String::length))
            .orElse("");
        List<PostCodeCourtEntity> filteredResults = results.stream()
            .filter(e -> e.getId().getPostCode().equals(longestPostcodeMatch))
            .toList();

        if (filteredResults.size() > 1) {
            log.error(
                "Multiple active EpimIds found for postcode:{} count:{}",
                longestPostcodeMatch,
                filteredResults.size()
            );
            return List.of();
        }
        log.info("Found court mapping of {} for postcode: {}", longestPostcodeMatch, postcode);
        return filteredResults;
    }
}
