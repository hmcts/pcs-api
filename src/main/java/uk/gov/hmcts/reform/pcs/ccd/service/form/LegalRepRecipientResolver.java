package uk.gov.hmcts.reform.pcs.ccd.service.form;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.ClaimPartyContactDetailsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.OrganisationEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.ClaimPartyContactDetailsRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.legalrepresentative.OrganisationRepository;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves where post for a represented defendant must go: the firm actively on record for them and that firm's
 * contact address for the case (seeded from PRD on notice of change, replaced by "Amend representative's details").
 * CPR 6.23 makes the solicitor's business address the party's address for service, so this only drives the
 * envelope; the forms themselves keep the defendant's own address.
 */
@Service
public class LegalRepRecipientResolver {

    private final OrganisationRepository organisationRepository;
    private final ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository;
    private final AddressMapper addressMapper;
    private final FeatureToggleService featureToggleService;

    public LegalRepRecipientResolver(OrganisationRepository organisationRepository,
                                     ClaimPartyContactDetailsRepository claimPartyContactDetailsRepository,
                                     AddressMapper addressMapper,
                                     FeatureToggleService featureToggleService) {
        this.organisationRepository = organisationRepository;
        this.claimPartyContactDetailsRepository = claimPartyContactDetailsRepository;
        this.addressMapper = addressMapper;
        this.featureToggleService = featureToggleService;
    }

    public Optional<LegalRepAddressee> resolve(PartyEntity defendant, long caseReference) {
        if (!featureToggleService.isEnabled(FeatureFlag.RELEASE_1_DOT_3)
            || !featureToggleService.isEnabled(FeatureFlag.CUI_RESPOND_TO_CLAIM_LR)) {
            return Optional.empty();
        }
        return organisationRepository
            .findByPartyLinkedToOrganisationAndCaseAndActive(defendant.getId(), caseReference)
            .flatMap(organisation -> addressee(organisation, caseReference));
    }

    private Optional<LegalRepAddressee> addressee(OrganisationEntity organisation, long caseReference) {
        return claimPartyContactDetailsRepository
            .findFirstByOrganisationOrganisationIdAndPcsCaseCaseReferenceOrderByIdDesc(
                organisation.getOrganisationId(), caseReference)
            .map(ClaimPartyContactDetailsEntity::getAddress)
            .filter(Objects::nonNull)
            .map(address -> new LegalRepAddressee(
                organisation.getOrganisationName(), addressMapper.toAddressUK(address)));
    }

    public record LegalRepAddressee(String name, AddressUK address) {
    }
}
