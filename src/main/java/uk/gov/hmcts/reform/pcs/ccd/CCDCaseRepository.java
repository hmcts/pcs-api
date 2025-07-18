package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimPaymentTabRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final ClaimPaymentTabRenderer claimPaymentTabRenderer;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseReference The CCD case reference to load
     */
    @Override
    public PCSCase getCase(long caseReference) {

        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);

        PCSCase pcsCase = PCSCase.builder()
            .applicantForename(pcsCaseEntity.getApplicantForename())
            .applicantSurname(pcsCaseEntity.getApplicantSurname())
            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .caseManagementLocation(pcsCaseEntity.getCaseManagementLocation())
            .build();

        setDerivedProperties(caseReference,pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private void setDerivedProperties(long caseRef,PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));
        pcsCase.setClaimPaymentTabMarkdown(claimPaymentTabRenderer.render(
            caseRef,pcsCaseEntity.getPaymentStatus().getLabel()));
        String formattedAddress = formatAddress(pcsCase.getPropertyAddress());
        pcsCase.setPageHeadingMarkdown("""
                                       <h3 class="govuk-heading-s">
                                            %s<br>
                                            Case number: ${[CASE_REFERENCE]} <br>
                                        </h3>
                                       """.formatted(formattedAddress));

    }

    private Optional<PartyEntity> findPartyForCurrentUser(PcsCaseEntity pcsCaseEntity) {
        UUID userId = securityContextService.getCurrentUserId();

        if (userId != null) {
            return pcsCaseEntity.getParties().stream()
                .filter(party -> userId.equals(party.getIdamId()))
                .findFirst();
        } else {
            return Optional.empty();
        }
    }

    private AddressUK convertAddress(AddressEntity address) {
        if (address == null) {
            return null;
        }

        return modelMapper.map(address, AddressUK.class);
    }

    private PcsCaseEntity loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCaseReference(caseRef)
            .orElseThrow(() -> new CaseNotFoundException(caseRef));
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

}
