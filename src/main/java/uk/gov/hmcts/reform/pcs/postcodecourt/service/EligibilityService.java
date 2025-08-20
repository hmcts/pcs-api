package uk.gov.hmcts.reform.pcs.postcodecourt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.postcodecourt.exception.InvalidPostCodeException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityResult;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.EligibilityStatus;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.PostcodeCourtMapping;
import uk.gov.hmcts.reform.pcs.postcodecourt.repository.PostCodeCourtRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EligibilityService {

    private final PostCodeCourtRepository postCodeCourtRepository;
    private final PartialPostcodesGenerator partialPostcodesGenerator;
    private final Clock ukClock;

    public EligibilityService(PostCodeCourtRepository postCodeCourtRepository,
                              PartialPostcodesGenerator partialPostcodesGenerator,
                              @Qualifier("ukClock") Clock ukClock) {

        this.postCodeCourtRepository = postCodeCourtRepository;
        this.partialPostcodesGenerator = partialPostcodesGenerator;
        this.ukClock = ukClock;
    }

    /**
     * Checks the eligibility to use the Possessions Service for a given postcode. Some postcodes
     * are cross-border in which case the returned {@link EligibilityResult} indicates that
     * the request should be repeated with legislative country specified.
     * @param postcode The postcode to check
     * @param providedLegislativeCountry An optional legislative country, for use with cross-border postcodes
     * @return An {@link EligibilityResult} with the eligibility status
     */
    public EligibilityResult checkEligibility(String postcode, LegislativeCountry providedLegislativeCountry) {
        if (postcode == null || postcode.isBlank()) {
            throw new InvalidPostCodeException("Postcode can't be empty or null");
        }

        List<PostcodeCourtMapping> postcodeMappings = findMatchingMappings(postcode, providedLegislativeCountry);

        if (providedLegislativeCountry == null) {
            List<PostcodeCourtMapping> mostSpecificMappings = getMostSpecificMappings(postcodeMappings);
            List<LegislativeCountry> legislativeCountries = getDistinctLegislativeCountries(mostSpecificMappings);

            if (legislativeCountries.size() > 1) {
                log.debug("Multiple legislative countries match {}: {}", postcode, legislativeCountries);

                return EligibilityResult.builder()
                    .status(EligibilityStatus.LEGISLATIVE_COUNTRY_REQUIRED)
                    .legislativeCountries(legislativeCountries)
                    .build();
            }
        }

        List<PostcodeCourtMapping> activeMappings = getActiveMappings(postcodeMappings);
        if (activeMappings.isEmpty()) {
            log.error("No active e-PIMS ID found for postcode: {}", postcode);
            return EligibilityResult.builder()
                .status(EligibilityStatus.NO_MATCH_FOUND)
                .build();
        }

        List<PostcodeCourtMapping> filteredResults = getMostSpecificMappings(activeMappings);
        if (filteredResults.size() > 1) {
            log.error("Multiple active e-PIMS IDs found for postcode: {} count: {}",
                postcode,
                filteredResults.size()
            );
            return EligibilityResult.builder()
                .status(EligibilityStatus.MULTIPLE_MATCHES_FOUND)
                .build();
        }

        PostcodeCourtMapping matchedMapping = filteredResults.getFirst();
        EligibilityStatus eligibilityStatus = getCourtEligibility(matchedMapping);

        log.debug("Postcode {} was matched against {} with eligibility status: {}",
                  postcode, matchedMapping.getPostcode(), eligibilityStatus);

        return EligibilityResult.builder()
            .status(eligibilityStatus)
            .epimsId(matchedMapping.getEpimsId())
            .legislativeCountry(matchedMapping.getLegislativeCountry())
            .build();
    }

    private List<PostcodeCourtMapping> findMatchingMappings(String postcode,
                                                            LegislativeCountry providedLegislativeCountry) {

        List<String> postcodes = partialPostcodesGenerator.generateForPostcode(postcode);

        if (providedLegislativeCountry != null) {
            return postCodeCourtRepository.findByPostCodeIn(postcodes, providedLegislativeCountry);
        } else {
            return postCodeCourtRepository.findByPostCodeIn(postcodes);
        }
    }

    private List<PostcodeCourtMapping> getActiveMappings(List<PostcodeCourtMapping> postcodeMappings) {
        LocalDate ukLocalDateNow = LocalDate.now(ukClock);

        return postcodeMappings.stream()
            .filter(mapping -> {
                LocalDate effectiveFrom = mapping.getMappingEffectiveFrom();
                LocalDate effectiveTo = mapping.getMappingEffectiveTo();

                return (!effectiveFrom.isAfter(ukLocalDateNow))
                    && (effectiveTo == null || !effectiveTo.isBefore(ukLocalDateNow));
            })
            .toList();
    }

    private EligibilityStatus getCourtEligibility(PostcodeCourtMapping matchedMapping) {
        return Optional.ofNullable(matchedMapping.getCourtEligibleFrom())
            .filter(eligibleFrom -> !eligibleFrom.isAfter(LocalDate.now(ukClock)))
            .map(isEligible -> EligibilityStatus.ELIGIBLE)
            .orElse(EligibilityStatus.NOT_ELIGIBLE);
    }


    /**
     * Returns all mappings that have the most specific, (i.e. longest), postcode.
     * @param postCodeMappings The list of mappings which may have different length partial postcodes
     * @return A list of mappings that have the most specific postcode
     */
    private static List<PostcodeCourtMapping> getMostSpecificMappings(List<PostcodeCourtMapping> postCodeMappings) {
        String mostSpecificPostcode = postCodeMappings.stream()
            .map(PostcodeCourtMapping::getPostcode)
            .max(Comparator.comparingInt(String::length))
            .orElse("");

        return postCodeMappings.stream()
            .filter(e -> e.getPostcode().equals(mostSpecificPostcode))
            .toList();
    }

    /**
     * Get distinct legislative countries from the provided mappings.
     * @param postCodeMappings The list of mappings to check
     * @return A list of distinct legistative countries, order by label
     */
    private static List<LegislativeCountry> getDistinctLegislativeCountries(
        List<PostcodeCourtMapping> postCodeMappings) {

        return postCodeMappings.stream()
            .map(PostcodeCourtMapping::getLegislativeCountry)
            .distinct()
            .sorted()
            .toList();
    }

}
