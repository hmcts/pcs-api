package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseDescriptionService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PcsCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final CaseDescriptionService caseDescriptionService;
    private final UserInfoService userInfoService;
    private final ModelMapper modelMapper;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseReference The CCD case reference to load
     */
    @Override
    public PcsCase getCase(long caseReference) {
        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);


        PcsCase pcsCase = PcsCase.builder()
            .hyphenatedCaseRef(formatCaseRef(caseReference))
            .claimAddress(convertAddress(pcsCaseEntity.getAddress()))
            .build();

        setDerivedProperties(pcsCase);

        return pcsCase;
    }

    private void setDerivedProperties(PcsCase pcsCase) {
        pcsCase.setCaseDescription(caseDescriptionService.createCaseDescription(pcsCase));
        pcsCase.setClaimPostcode(getPostcode(pcsCase.getClaimAddress()));

        String formattedAddress = formatAddress(pcsCase.getClaimAddress());

        pcsCase.setPageHeadingMarkdown("""
                                       <h3 class="govuk-heading-s">
                                            %s<br>
                                            Case number: ${[CASE_REFERENCE]} (${[STATE]})<br>
                                            User: ${userDetails}
                                        </h3>
                                       """.formatted(formattedAddress));

        UserInfo userInfo = userInfoService.getCurrentUserInfo();
        pcsCase.setUserDetails("%s (%s)".formatted(userInfo.getSub(), userInfo.getUid()));
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }

        return modelMapper.map(address, AddressUK.class);
    }

    private String formatAddress(AddressUK address) {
        if (address == null) {
            return null;
        }

        return Stream.of(address.getAddressLine1(), address.getAddressLine2(), address.getAddressLine3(),
                  address.getPostTown(), address.getCounty(), address.getPostCode())
            .filter(Objects::nonNull)
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.joining(", "));
    }

    private String getPostcode(AddressUK address) {
        if (address == null) {
            return null;
        }

        return address.getPostCode();
    }

    private PcsCaseEntity loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCaseReference(caseRef)
            .orElseThrow(() -> new CaseNotFoundException(caseRef));
    }

    private static String formatCaseRef(Long caseReference) {
        if (caseReference == null) {
            return null;
        }

        String temp = String.format("%016d", caseReference);
        return String.format(
            "%4s-%4s-%4s-%4s",
            temp.substring(0, 4),
            temp.substring(4, 8),
            temp.substring(8, 12),
            temp.substring(12, 16)
        );
    }

}
