package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.NextStepsRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftEventService;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.continueCaseCreation;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final ModelMapper modelMapper;
    private final DraftEventService draftEventService;
    private final NextStepsRenderer nextStepsRenderer;

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
            .build();

        setDerivedProperties(caseReference, pcsCase);

        return pcsCase;
    }

    private void setDerivedProperties(long caseReference, PCSCase pcsCase) {
        boolean draftExists = draftEventService.draftExists(caseReference, continueCaseCreation);

        pcsCase.getControlFlags()
            .setDraftExists(YesOrNo.from(draftExists));

        pcsCase.getMarkdownFields()
            .setNextSteps(nextStepsRenderer.render(caseReference, draftExists));

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

}
