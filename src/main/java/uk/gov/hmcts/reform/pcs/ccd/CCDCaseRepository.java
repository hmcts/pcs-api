package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenApp;
import uk.gov.hmcts.reform.pcs.ccd.domain.GenAppEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PaymentStatus;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.AddressEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimHistoryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimPaymentTabRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GenAppHistoryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GenAppListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.CounterClaimEventService;
import uk.gov.hmcts.reform.pcs.ccd.service.GenAppService;
import uk.gov.hmcts.reform.pcs.ccd.utils.ListValueUtils;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.roles.api.security.SecurityContextService;
import uk.gov.hmcts.reform.pcs.roles.service.UserInfoService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final SecurityContextService securityContextService;
    private final ModelMapper modelMapper;
    private final ClaimPaymentTabRenderer claimPaymentTabRenderer;
    private final ClaimListRenderer claimListRenderer;
    private final ClaimHistoryRenderer claimHistoryRenderer;
    private final GenAppListRenderer genAppListRenderer;
    private final ClaimService claimService;
    private final GenAppService genAppService;
    private final UserInfoService userInfoService;
    private final GenAppHistoryRenderer genAppHistoryRenderer;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseReference The CCD case reference to load
     */
    @Override
    public PCSCase getCase(long caseReference) {

        PcsCaseEntity pcsCaseEntity = loadCaseData(caseReference);

        PCSCase pcsCase = PCSCase.builder()
            .propertyAddress(convertAddress(pcsCaseEntity.getPropertyAddress()))
            .caseManagementLocation(pcsCaseEntity.getCaseManagementLocation())
            .preActionProtocolCompleted(pcsCaseEntity.getPreActionProtocolCompleted() != null
                ? VerticalYesNo.from(pcsCaseEntity.getPreActionProtocolCompleted())
                : null)
            .build();

        setDerivedProperties(caseReference, pcsCase, pcsCaseEntity);

        return pcsCase;
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

        List<Claim> claims = pcsCaseEntity.getClaims().stream()
            .map(claimEntity -> modelMapper.map(claimEntity, Claim.class))
            .toList();


        UserInfo userInfo = userInfoService.getCurrentUserInfo();
        Map<UUID, List<CounterClaimEvent>> claimActionMap = claims.stream()
                .collect(Collectors.toMap(Claim::getId, claim -> getApplicableAndPermittedActions(claim, userInfo)));

        pcsCase.setClaimListMarkdown(claimListRenderer.render(caseRef, claims, claimActionMap));
        pcsCase.setClaimHistoryMarkdown(claimHistoryRenderer.render(caseRef, claims));

        List<GenApp> genApps = pcsCaseEntity.getGenApps().stream()
            .map(genApp -> modelMapper.map(genApp, GenApp.class))
            .toList();

        Map<UUID, List<GenAppEvent>> genAppActionMap = genApps.stream()
            .collect(Collectors.toMap(GenApp::getId, genApp -> getApplicableAndPermittedActions(genApp, userInfo)));

        pcsCase.setGenAppListMarkdown(genAppListRenderer.render(caseRef, genApps, genAppActionMap));
        pcsCase.setGenAppHistoryMarkdown(genAppHistoryRenderer.render(caseRef, genApps));

        pcsCase.setFormattedPropertyAddress(formatAddress(pcsCase.getPropertyAddress()));

        pcsCase.setPageHeadingMarkdown("""
                                       <h3 class="govuk-heading-s">
                                            %s<br>
                                            Case number: ${[CASE_REFERENCE]} <br>
                                            Current user: %s
                                        </h3>
                                       """.formatted(formatAddress(pcsCase.getPropertyAddress()), userInfo.getSub()));

    }

    private List<CounterClaimEvent> getApplicableAndPermittedActions(Claim claim, UserInfo userInfo) {
        List<CounterClaimEvent> applicableEvents = claimService.getApplicableCounterClaimEvents(claim.getId());

        String userEmail = userInfo.getSub();
        List<String> userRoles = new ArrayList<>(userInfo.getRoles());

        if (userEmail.equals(claim.getApplicantEmail())) {
            userRoles.add(CounterClaimEventService.CLAIM_APPLICANT);
        }

        if (userEmail.equals(claim.getRespondentEmail())) {
            userRoles.add(CounterClaimEventService.CLAIM_RESPONDENT);
        }

        return filterByUserRolesCC(applicableEvents, userRoles);
    }

    private List<CounterClaimEvent> filterByUserRolesCC(List<CounterClaimEvent> actions, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        return actions.stream()
            .filter(action -> action.getApplicableRoles().stream().anyMatch(userRoles::contains))
            .toList();
    }

    private List<GenAppEvent> getApplicableAndPermittedActions(GenApp genApp, UserInfo userInfo) {
        List<GenAppEvent> applicableEvents = genAppService.getApplicableEvents(genApp.getId());

        List<String> userRoles = new ArrayList<>(userInfo.getRoles());

        return filterByUserRolesGA(applicableEvents, userRoles);
    }

    private List<GenAppEvent> filterByUserRolesGA(List<GenAppEvent> actions, List<String> userRoles) {
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        return actions.stream()
            .filter(action -> action.getApplicableRoles().stream().anyMatch(userRoles::contains))
            .toList();
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
