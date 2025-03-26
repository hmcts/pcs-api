package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.wrapListItems;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase> {

    private final PCSCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;

    public CCDCaseRepository(PCSCaseRepository pcsCaseRepository,
                             PartyRepository partyRepository) {
        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
    }

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase getCase(long caseRef, String roleAssignments) {
        // Load the case from our database.
        PCSCase pcsCase = pcsCaseRepository.findDtoByCaseReference(caseRef).orElseThrow(
            () -> new CaseNotFoundException("No case data found for reference " + caseRef)
        );

        Map<Boolean, List<Party>> partiesByActiveFlag = partyRepository.findAllDtoByCaseReference(caseRef)
            .stream()
            .collect(Collectors.groupingBy(Party::isActive));

        pcsCase.setActiveParties(wrapListItems(partiesByActiveFlag.get(true)));
        pcsCase.setInactiveParties(wrapListItems(partiesByActiveFlag.get(false)));

        // TODO: Extract to a dynamic fields "decorator" class?
        pcsCase.setHyphenatedCaseRef(formatCaseRef(caseRef));
        pcsCase.setCaseDescription(buildCaseDescription(pcsCase));
        pcsCase.setActivePartiesEmpty(YesOrNo.from(pcsCase.getActiveParties().isEmpty()));
        pcsCase.setInactivePartiesEmpty(YesOrNo.from(pcsCase.getInactiveParties().isEmpty()));

        return pcsCase;
    }

    private String buildCaseDescription(PCSCase pcsCase) {
        AddressUK propertyAddress = pcsCase.getPropertyAddress();
        return "Property address: " + getPropertyAddressAsString(propertyAddress);
    }

    private static String getPropertyAddressAsString(AddressUK propertyAddress) {
        if (propertyAddress != null) {
            return Stream.of(propertyAddress.getAddressLine1(), propertyAddress.getAddressLine2(),
                      propertyAddress.getAddressLine3(), propertyAddress.getPostTown())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
        } else {
            return "Not specified";
        }
    }

    private static String formatCaseRef(Long caseId) {
        if (caseId == null) {
            return null;
        }

        String temp = String.format("%016d", caseId);
        return String.format(
            "%4s-%4s-%4s-%4s",
            temp.substring(0, 4),
            temp.substring(4, 8),
            temp.substring(8, 12),
            temp.substring(12, 16)
        );
    }

}
