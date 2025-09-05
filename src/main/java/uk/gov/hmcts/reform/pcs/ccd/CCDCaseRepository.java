package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentCategory;
import uk.gov.hmcts.reform.pcs.ccd.entity.DocumentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimPaymentTabRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.pcs.ccd.utils.ListValueUtils;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import uk.gov.hmcts.ccd.sdk.type.Document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
@Slf4j
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final ClaimPaymentTabRenderer claimPaymentTabRenderer;
    private final PcsCaseService pcsCaseService;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseReference The CCD case reference to load
     * @param state the current case state
     */
    @Override
    public PCSCase getCase(long caseReference, String state) {

        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);

        // TODO remove debug logging
        log.info("Loading case {} with {} documents", caseReference, pcsCaseEntity.getDocuments().size());
        pcsCaseEntity.getDocuments().forEach(doc ->
                log.info("Document: {} - Category: {}", doc.getFileName(), doc.getCategory()));


        PCSCase pcsCase = PCSCase.builder()

            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .caseManagementLocation(pcsCaseEntity.getCaseManagementLocation())
            .supportingDocumentsCategoryA(mapDocumentLinks(pcsCaseEntity.getDocuments(), DocumentCategory.CATEGORY_A))
            .supportingDocumentsCategoryB(mapDocumentLinks(pcsCaseEntity.getDocuments(), DocumentCategory.CATEGORY_B))
             .preActionProtocolCompleted(pcsCaseEntity.getPreActionProtocolCompleted() != null
                ? VerticalYesNo.from(pcsCaseEntity.getPreActionProtocolCompleted())
                : null)
            .currentRent(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getRentAmount() != null
                ? pcsCaseEntity.getTenancyLicence().getRentAmount().toPlainString() : null)
            .rentFrequency(pcsCaseEntity.getTenancyLicence() != null
                ? pcsCaseEntity.getTenancyLicence().getRentPaymentFrequency() : null)
            .otherRentFrequency(pcsCaseEntity.getTenancyLicence() != null
                ? pcsCaseEntity.getTenancyLicence().getOtherRentFrequency() : null)
            .dailyRentChargeAmount(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount() != null
                ? pcsCaseEntity.getTenancyLicence().getDailyRentChargeAmount().toPlainString() : null)
            .noticeServed(pcsCaseEntity.getTenancyLicence() != null
                && pcsCaseEntity.getTenancyLicence().getNoticeServed() != null
                ? YesOrNo.from(pcsCaseEntity.getTenancyLicence().getNoticeServed()) : null)
            .defendants(pcsCaseService.mapToDefendantDetails(pcsCaseEntity.getDefendants()))
            .build();

        setDerivedProperties(caseReference,pcsCase, pcsCaseEntity);

        return pcsCase;
    }

    private List<ListValue<DocumentLink>> mapDocumentLinks(Set<DocumentEntity> documentEntities, 
                                                           DocumentCategory categoryFilter) {
        if (documentEntities == null || documentEntities.isEmpty()) {
            log.warn("No documents found to map");
            return null;
        }

        List<ListValue<DocumentLink>> result = documentEntities.stream()
            .filter(documentEntity -> documentEntity.getCategory() == categoryFilter)
            .map(docEntity -> {
                // inner Document
                Document document = Document.builder()
                    .url(docEntity.getFilePath())
                    .filename(docEntity.getFileName())
                    .binaryUrl(docEntity.getFilePath())
                    .categoryId(categoryFilter.getLabel())
                    .build();

                // wrap in DocumentLink
                DocumentLink documentLink = new DocumentLink(document);

                return ListValue.<DocumentLink>builder()
                    .id(docEntity.getId().toString())
                    .value(documentLink)
                    .build();
            })
            .collect(Collectors.toList());

        log.info("Mapped {} documents for category {}", result.size(), categoryFilter);
        return result.isEmpty() ? null : result;
    }

    private void setDerivedProperties(long caseRef,PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean pcqIdSet = findPartyForCurrentUser(pcsCaseEntity)
            .map(party -> party.getPcqId() != null)
            .orElse(false);

        pcsCase.setUserPcqIdSet(YesOrNo.from(pcqIdSet));

        PaymentStatus paymentStatus = pcsCaseEntity.getPaymentStatus();
        if (paymentStatus != null) {
            pcsCase.setClaimPaymentTabMarkdown(claimPaymentTabRenderer.render(caseRef, paymentStatus));
        }
        pcsCase.setParties(mapAndWrapParties(pcsCaseEntity.getParties()));

        pcsCase.setPageHeadingMarkdown("""
                                       <h3 class="govuk-heading-s">
                                            %s<br>
                                            Case number: ${[CASE_REFERENCE]} <br>
                                        </h3>
                                       """.formatted(formatAddress(pcsCase.getPropertyAddress())));

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

    private List<ListValue<Party>> mapAndWrapParties(Set<PartyEntity> partyEntities) {
        return partyEntities.stream()
            .map(entity -> modelMapper.map(entity, Party.class))
            .collect(Collectors.collectingAndThen(Collectors.toList(), ListValueUtils::wrapListItems));
    }
}
