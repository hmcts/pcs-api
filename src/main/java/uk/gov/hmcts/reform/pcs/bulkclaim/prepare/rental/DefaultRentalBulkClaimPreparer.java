package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Rejected;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.RentalBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkAddressMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkClaimantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkCommonParticularsMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkDefendantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPbaCheck;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility.PropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkStatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkUnderlesseeMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimantDesignationRentalType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsRentedType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.RentalClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.TenancyTypeCode;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.RentSectionRoutingService;
import uk.gov.hmcts.reform.pcs.ccd.service.routing.wales.WalesRentSectionRoutingService;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.ClaimantEligibilityRules;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.TenancyDateRules;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Prepares rental claims for properties in England (tenancies) and Wales (occupation contracts). Wales claims are
 * rejected while the Wales make a claim feature is off.
 */
@Service
public class DefaultRentalBulkClaimPreparer implements RentalBulkClaimPreparer {

    private static final String SCHEMA_VERSION = "2";

    private final BulkClaimProperties properties;
    private final BulkPbaCheck pbaCheck;
    private final BulkPropertyEligibility propertyEligibility;
    private final BulkAddressMapper addressMapper;
    private final BulkClaimantMapper claimantMapper;
    private final BulkDefendantMapper defendantMapper;
    private final BulkUnderlesseeMapper underlesseeMapper;
    private final BulkStatementOfTruthMapper statementOfTruthMapper;
    private final BulkCommonParticularsMapper commonParticularsMapper;
    private final GroundRouter groundRouter;
    private final RentalNoticeMapper noticeMapper;
    private final RentalRentMapper rentMapper;
    private final RentalAlternativesMapper alternativesMapper;
    private final ClaimantEligibilityRules claimantEligibilityRules;
    private final TenancyDateRules tenancyDateRules;
    private final RentSectionRoutingService rentSectionRoutingService;
    private final FeatureToggleService featureToggleService;
    private final WalesGroundRouter walesGroundRouter;
    private final WalesNoticeMapper walesNoticeMapper;
    private final WalesOccupationContractMapper walesContractMapper;
    private final WalesDetailsMapper walesDetailsMapper;
    private final WalesRentSectionRoutingService walesRentSectionRoutingService;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultRentalBulkClaimPreparer(BulkClaimProperties properties,
                                          BulkPbaCheck pbaCheck,
                                          BulkPropertyEligibility propertyEligibility,
                                          BulkAddressMapper addressMapper,
                                          BulkClaimantMapper claimantMapper,
                                          BulkDefendantMapper defendantMapper,
                                          BulkUnderlesseeMapper underlesseeMapper,
                                          BulkStatementOfTruthMapper statementOfTruthMapper,
                                          BulkCommonParticularsMapper commonParticularsMapper,
                                          GroundRouter groundRouter,
                                          RentalNoticeMapper noticeMapper,
                                          RentalRentMapper rentMapper,
                                          RentalAlternativesMapper alternativesMapper,
                                          ClaimantEligibilityRules claimantEligibilityRules,
                                          TenancyDateRules tenancyDateRules,
                                          RentSectionRoutingService rentSectionRoutingService,
                                          FeatureToggleService featureToggleService,
                                          WalesGroundRouter walesGroundRouter,
                                          WalesNoticeMapper walesNoticeMapper,
                                          WalesOccupationContractMapper walesContractMapper,
                                          WalesDetailsMapper walesDetailsMapper,
                                          WalesRentSectionRoutingService walesRentSectionRoutingService) {
        this.properties = properties;
        this.pbaCheck = pbaCheck;
        this.propertyEligibility = propertyEligibility;
        this.addressMapper = addressMapper;
        this.claimantMapper = claimantMapper;
        this.defendantMapper = defendantMapper;
        this.underlesseeMapper = underlesseeMapper;
        this.statementOfTruthMapper = statementOfTruthMapper;
        this.commonParticularsMapper = commonParticularsMapper;
        this.groundRouter = groundRouter;
        this.noticeMapper = noticeMapper;
        this.rentMapper = rentMapper;
        this.alternativesMapper = alternativesMapper;
        this.claimantEligibilityRules = claimantEligibilityRules;
        this.tenancyDateRules = tenancyDateRules;
        this.rentSectionRoutingService = rentSectionRoutingService;
        this.featureToggleService = featureToggleService;
        this.walesGroundRouter = walesGroundRouter;
        this.walesNoticeMapper = walesNoticeMapper;
        this.walesContractMapper = walesContractMapper;
        this.walesDetailsMapper = walesDetailsMapper;
        this.walesRentSectionRoutingService = walesRentSectionRoutingService;
    }

    @Override
    public BulkClaimPreparationResult prepare(RentalClaimSet claimSet,
                                              RentalClaimSet.RentalClaim claim,
                                              OrganisationDetailsResponse organisation) {
        String cui = claim.getCustomerUniqueIdentifier();
        BulkErrorCollector errors = BulkErrorCollector.forRentalClaim(claimSet, claim);

        var pbaError = pbaCheck.validateOverride(
            organisation, claim.getPaymentAccountNumber(), errors.absolutePath("PaymentAccountNumber"));
        if (pbaError.isPresent()) {
            return new Rejected(cui, BulkFailReason.PBA_OVERRIDE_NOT_VALID, List.of(pbaError.get()));
        }

        PropertyEligibility eligibility = propertyEligibility.check(
            claim.getProperty().getAddress(), claim.getProperty().getLegislativeCountry());
        if (!eligibility.isEligible()) {
            return reject(cui, eligibility.failReason(), errors, "Property");
        }

        LegislativeCountry country = eligibility.legislativeCountry();
        if (country == LegislativeCountry.WALES
            && !featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)) {
            return reject(cui, BulkFailReason.CLAIM_KIND_NOT_ENABLED, errors, "Property");
        }

        ClaimantType claimantType = claimantType(claim.getClaimantDesignation(), country);
        if (!isClaimantTypeEligible(country, claimantType)) {
            return reject(cui, BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE, errors, "ClaimantDesignation");
        }

        PCSCase pcsCase = PCSCase.builder().build();
        eligibility.applyTo(pcsCase);
        pcsCase.setClaimantType(claimantTypeList(claimantType, country));
        BulkClaimExtras extras = populate(claimSet, claim, organisation, country, pcsCase, errors);

        if (errors.hasErrors()) {
            return new Rejected(cui, BulkFailReason.VALIDATION_FAILED, errors.errors());
        }

        return new Prepared(new PreparedBulkClaim(
            pcsCase,
            extras,
            BulkPbaCheck.effectiveAccount(claimSet.getPaymentAccountNumber(), claim.getPaymentAccountNumber()),
            claim.getPaymentCustomerReference()
        ));
    }

    private BulkClaimPreparationResult reject(String cui, BulkFailReason reason, BulkErrorCollector errors,
                                              String path) {
        return new Rejected(cui, reason, List.of(new BulkValidationError(
            errors.absolutePath(path), reason.name(), reason.getReason())));
    }

    private BulkClaimExtras populate(RentalClaimSet claimSet,
                                     RentalClaimSet.RentalClaim claim,
                                     OrganisationDetailsResponse organisation,
                                     LegislativeCountry country,
                                     PCSCase pcsCase,
                                     BulkErrorCollector errors) {
        final ParticularsRentedType particulars = claim.getClaimParticulars();
        final String particularsPath = "ClaimParticulars";

        pcsCase.setPropertyAddress(addressMapper.toValidAddress(
            claim.getProperty().getAddress(), "Property/Address", errors));

        BulkClaimantMapper.ClaimantMapping claimant = claimantMapper.map(
            claim.getClaimant(), claim.getSolicitor(), organisation, "Claimant", "Solicitor", errors);
        claimant.applyTo(pcsCase);

        BulkDefendantMapper.DefendantsMapping defendants = defendantMapper.map(
            claim.getDefendant(), "Defendant", errors);
        defendants.applyTo(pcsCase);

        pcsCase.setClaimAgainstTrespassers(VerticalYesNo.NO);
        pcsCase.setWantToUploadDocuments(VerticalYesNo.NO);
        pcsCase.setCompletionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW);

        final var common = commonParticularsMapper.apply(
            particulars, country, pcsCase, particularsPath, errors);
        underlesseeMapper.map(particulars.getUnderlesseeOrMortgagee(), particularsPath + "/UnderlesseeOrMortgagee",
                              errors).applyTo(pcsCase);
        pcsCase.setStatementOfTruth(statementOfTruthMapper.map(
            particulars.getClaimStatementOfTruth(), particularsPath + "/ClaimStatementOfTruth", errors));

        final String noticeType = country == LegislativeCountry.WALES
            ? populateWales(particulars, pcsCase, particularsPath, errors)
            : populateEngland(particulars, pcsCase, particularsPath, errors);
        applyClaimantAndSettlementDetails(particulars, pcsCase, particularsPath, errors);

        return new BulkClaimExtras(
            SCHEMA_VERSION,
            claim.getClaimantDesignation().value(),
            claimant.legacyPcolClaimantReference(),
            claimant.legacyPcolSolicitorReference(),
            claimant.solicitorFirmName(),
            common.claimingCosts(),
            noticeType,
            claimant.claimantExtras(),
            defendants.extras(),
            commonParticularsMapper.paymentHistory(particulars.getPaymentHistory()),
            null,
            List.of()
        );
    }

    private String populateEngland(ParticularsRentedType particulars,
                                   PCSCase pcsCase,
                                   String path,
                                   BulkErrorCollector errors) {
        rejectNotAllowedInEngland(particulars, path, errors);

        TenancyLicenceType tenancy = tenancy(particulars, pcsCase, path, errors);
        if (tenancy != null) {
            groundRouter.route(tenancy, particulars.getGrounds(), pcsCase, path + "/Grounds", errors);
        }

        String noticeType = noticeMapper.apply(particulars.getNotice(), pcsCase, path + "/Notice", errors);

        boolean rentApplies = tenancy != null
            && rentSectionRoutingService.shouldShowRentSection(pcsCase) == YesOrNo.YES;
        rentMapper.apply(particulars, rentApplies, pcsCase, path, errors);

        alternativesMapper.apply(particulars.getSuspensionOfRightToBuy(), particulars.getDemotionOfTenancy(),
                                 pcsCase, path, errors);
        return noticeType;
    }

    private static void rejectNotAllowedInEngland(ParticularsRentedType particulars,
                                                  String path,
                                                  BulkErrorCollector errors) {
        if (particulars.getWalesNotice() != null) {
            errors.add(path + "/WalesNotice", BulkErrorCollector.NOT_ALLOWED,
                       "WalesNotice only applies to a property in Wales; use Notice");
        }
        if (particulars.getWales() != null) {
            errors.add(path + "/Wales", BulkErrorCollector.NOT_ALLOWED, "Wales only applies to a property in Wales");
        }
    }

    private String populateWales(ParticularsRentedType particulars,
                                 PCSCase pcsCase,
                                 String path,
                                 BulkErrorCollector errors) {
        rejectNotAllowedInWales(particulars, path, errors);

        OccupationLicenceTypeWales contract = walesContractMapper.apply(particulars, pcsCase, path, errors);
        if (contract != null) {
            walesGroundRouter.route(contract, particulars.getGrounds(), pcsCase, path + "/Grounds", errors);
        }

        String noticeType = walesNoticeMapper.apply(
            particulars.getWalesNotice(), pcsCase, path + "/WalesNotice", errors);

        boolean asbApplies = pcsCase.getShowASBQuestionsPageWales() == YesOrNo.YES;
        walesDetailsMapper.apply(particulars.getWales(), asbApplies, pcsCase, path + "/Wales", errors);

        boolean rentApplies = contract != null
            && walesRentSectionRoutingService.shouldShowRentSection(pcsCase) == YesOrNo.YES;
        rentMapper.apply(particulars, rentApplies, pcsCase, path, errors);
        return noticeType;
    }

    private static void rejectNotAllowedInWales(ParticularsRentedType particulars,
                                                String path,
                                                BulkErrorCollector errors) {
        if (particulars.getNotice() != null) {
            errors.add(path + "/Notice", BulkErrorCollector.NOT_ALLOWED,
                       "Notice only applies to a property in England; use WalesNotice");
        }
        if (particulars.getSuspensionOfRightToBuy() != null) {
            errors.add(path + "/SuspensionOfRightToBuy", BulkErrorCollector.NOT_ALLOWED,
                       "Suspension of right to buy only applies to a property in England");
        }
        if (particulars.getDemotionOfTenancy() != null) {
            errors.add(path + "/DemotionOfTenancy", BulkErrorCollector.NOT_ALLOWED,
                       "Demotion of tenancy only applies to a property in England");
        }
    }

    private TenancyLicenceType tenancy(ParticularsRentedType particulars,
                                       PCSCase pcsCase,
                                       String path,
                                       BulkErrorCollector errors) {
        TenancyTypeCode code = particulars.getTenancyType();
        TenancyLicenceType type = switch (code) {
            case ASSURED_TENANCY -> TenancyLicenceType.ASSURED_TENANCY;
            case SECURE_TENANCY -> TenancyLicenceType.SECURE_TENANCY;
            case INTRODUCTORY_TENANCY -> TenancyLicenceType.INTRODUCTORY_TENANCY;
            case FLEXIBLE_TENANCY -> TenancyLicenceType.FLEXIBLE_TENANCY;
            case DEMOTED_TENANCY -> TenancyLicenceType.DEMOTED_TENANCY;
            case OTHER -> TenancyLicenceType.OTHER;
            case SECURE_CONTRACT, STANDARD_CONTRACT -> null;
        };
        if (type == null) {
            errors.add(path + "/TenancyType", BulkErrorCollector.INVALID_VALUE,
                       code + " is an occupation contract type and is not valid for a property in England");
            return null;
        }

        TenancyLicenceDetails.TenancyLicenceDetailsBuilder details = TenancyLicenceDetails.builder()
            .typeOfTenancyLicence(type)
            .tenancyLicenceDate(particulars.getTenancyCommenceDate())
            .hasCopyOfTenancyLicence(VerticalYesNo.NO)
            .reasonsForNoTenancyLicenceDocuments(properties.getSystemReasonNoDocuments());

        if (type == TenancyLicenceType.OTHER) {
            String otherDetails = particulars.getOtherTenancyTypeDetails();
            errors.requiredWithMaxLength(path + "/OtherTenancyTypeDetails", otherDetails, BulkLimits.MEDIUM_TEXT,
                                         TenancyLicenceDetails.DETAILS_OF_OTHER_TYPE_OF_TENANCY_LICENCE_LABEL);
            details.detailsOfOtherTypeOfTenancyLicence(otherDetails);
        }
        tenancyDateRules.validateEnglandTenancyLicenceDate(particulars.getTenancyCommenceDate())
            .ifPresent(message -> errors.add(path + "/TenancyCommenceDate", BulkErrorCollector.NOT_IN_PAST, message));

        pcsCase.setTenancyLicenceDetails(details.build());
        return type;
    }

    private void applyClaimantAndSettlementDetails(ParticularsRentedType particulars,
                                                   PCSCase pcsCase,
                                                   String path,
                                                   BulkErrorCollector errors) {
        String circumstances = StringUtils.trimToNull(particulars.getClaimantsCircumstances());
        errors.maxLength(path + "/ClaimantsCircumstances", circumstances, BulkLimits.LONG_TEXT,
                         "Claimant's circumstances");
        pcsCase.setClaimantCircumstances(ClaimantCircumstances.builder()
            .claimantCircumstancesSelect(VerticalYesNo.from(circumstances != null))
            .claimantCircumstancesDetails(circumstances)
            .build());

        if (particulars.isMediationAttempted() != null) {
            pcsCase.setMediationAttempted(VerticalYesNo.from(particulars.isMediationAttempted()));
        }
        if (particulars.isSettlementAttempted() != null) {
            pcsCase.setSettlementAttempted(VerticalYesNo.from(particulars.isSettlementAttempted()));
        }
    }

    private boolean isClaimantTypeEligible(LegislativeCountry country, ClaimantType type) {
        return claimantEligibilityRules.isClaimantTypeAllowedOnline(country, type)
            && properties.getEnabledClaimantTypes().contains(type);
    }

    private static ClaimantType claimantType(ClaimantDesignationRentalType designation, LegislativeCountry country) {
        return switch (designation) {
            case SOCIAL_LANDLORD -> country == LegislativeCountry.WALES
                ? ClaimantType.COMMUNITY_LANDLORD : ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;
            case PRIVATE_LANDLORD -> ClaimantType.PRIVATE_LANDLORD;
        };
    }

    private static DynamicStringList claimantTypeList(ClaimantType selected, LegislativeCountry country) {
        List<DynamicStringListElement> items = Arrays.stream(ClaimantType.values())
            .filter(type -> type.isApplicableFor(country))
            .map(type -> DynamicStringListElement.builder().code(type.name()).label(type.getLabel()).build())
            .toList();
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(selected.name()).label(selected.getLabel()).build())
            .listItems(items)
            .build();
    }
}
