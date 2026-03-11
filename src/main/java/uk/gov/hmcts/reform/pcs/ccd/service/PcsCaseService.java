package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PcsCaseService {

    private final PcsCaseRepository pcsCaseRepository;
    private final ClaimService claimService;
    private final PartyService partyService;
    private final DocumentService documentService;
    private final TenancyLicenceService tenancyLicenceService;
    private final AddressMapper addressMapper;

    public PcsCaseEntity createCase(long caseReference,
                                    AddressUK propertyAddress,
                                    LegislativeCountry legislativeCountry) {

        Objects.requireNonNull(propertyAddress, "Property address must be provided to create a case");
        Objects.requireNonNull(legislativeCountry, "Legislative country must be provided to create a case");

        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setCaseReference(caseReference);
        pcsCaseEntity.setPropertyAddress(addressMapper.toAddressEntityAndNormalise(propertyAddress));
        pcsCaseEntity.setLegislativeCountry(legislativeCountry);

        return pcsCaseRepository.save(pcsCaseEntity);
    }

    public void createMainClaimOnCase(long caseReference, PCSCase pcsCase) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);

        ClaimEntity claimEntity = claimService.createMainClaimEntity(pcsCase);
        List<DocumentEntity> documentEntities = documentService.createAllDocuments(pcsCase);
        pcsCaseEntity.addDocuments(documentEntities);
        claimEntity.addClaimDocuments(documentEntities);
        pcsCaseEntity.addClaim(claimEntity);

        partyService.createAllParties(pcsCase, pcsCaseEntity, claimEntity);

        pcsCaseEntity.setTenancyLicence(tenancyLicenceService.createTenancyLicenceEntity(pcsCase));
    }

    public PcsCaseEntity loadCase(long caseReference) {
        return pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
    }

    @Transactional
    public void updateCaseFlags(long caseReference, Flags caseFlags, List<ListValue<Party>> parties) {
        PcsCaseEntity pcsCaseEntity = loadCase(caseReference);
        pcsCaseEntity.setCaseFlags(caseFlags);

        if (parties != null && !parties.isEmpty()) {
            Map<UUID, PartyEntity> partiesById = pcsCaseEntity.getParties().stream()
                .collect(Collectors.toMap(PartyEntity::getId, Function.identity()));

            for (ListValue<Party> partyListValue : parties) {
                if (partyListValue.getId() == null || partyListValue.getValue() == null) {
                    continue;
                }

                UUID partyId;
                try {
                    partyId = UUID.fromString(partyListValue.getId());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                PartyEntity partyEntity = partiesById.get(partyId);
                if (partyEntity != null) {
                    partyEntity.setFlags(partyListValue.getValue().getFlags());
                }
            }
        }

        pcsCaseRepository.save(pcsCaseEntity);
    }

}
