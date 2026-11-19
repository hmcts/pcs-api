package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.mortgage;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.bulkclaim.config.BulkClaimProperties;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PaymentHistoryLine;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Prepared;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimPreparationResult.Rejected;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkFailReason;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkValidationError;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.PreparedBulkClaim;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.MortgageBulkClaimPreparer;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkAddressMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkClaimantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkCommonParticularsMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkDefendantMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPbaCheck;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkPropertyEligibility.PropertyEligibility;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkStatementOfTruthMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkUnderlesseeMapper;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.MortgageClaimSet;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ParticularsMortgagedType;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Arrays;
import java.util.List;

/**
 * Prepares mortgage possession claims for England and, when the Wales make a claim flag is on, Wales.
 */
@Service
public class DefaultMortgageBulkClaimPreparer implements MortgageBulkClaimPreparer {

    private static final String SCHEMA_VERSION = "2";
    private static final String PARTICULARS_PATH = "ParticularsOfClaim";

    private final BulkClaimProperties properties;
    private final FeatureToggleService featureToggleService;
    private final BulkPbaCheck pbaCheck;
    private final BulkPropertyEligibility propertyEligibility;
    private final BulkAddressMapper addressMapper;
    private final BulkClaimantMapper claimantMapper;
    private final BulkDefendantMapper defendantMapper;
    private final BulkUnderlesseeMapper underlesseeMapper;
    private final BulkStatementOfTruthMapper statementOfTruthMapper;
    private final BulkCommonParticularsMapper commonParticularsMapper;
    private final MortgageParticularsMapper mortgageParticularsMapper;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DefaultMortgageBulkClaimPreparer(BulkClaimProperties properties,
                                            FeatureToggleService featureToggleService,
                                            BulkPbaCheck pbaCheck,
                                            BulkPropertyEligibility propertyEligibility,
                                            BulkAddressMapper addressMapper,
                                            BulkClaimantMapper claimantMapper,
                                            BulkDefendantMapper defendantMapper,
                                            BulkUnderlesseeMapper underlesseeMapper,
                                            BulkStatementOfTruthMapper statementOfTruthMapper,
                                            BulkCommonParticularsMapper commonParticularsMapper,
                                            MortgageParticularsMapper mortgageParticularsMapper) {
        this.properties = properties;
        this.featureToggleService = featureToggleService;
        this.pbaCheck = pbaCheck;
        this.propertyEligibility = propertyEligibility;
        this.addressMapper = addressMapper;
        this.claimantMapper = claimantMapper;
        this.defendantMapper = defendantMapper;
        this.underlesseeMapper = underlesseeMapper;
        this.statementOfTruthMapper = statementOfTruthMapper;
        this.commonParticularsMapper = commonParticularsMapper;
        this.mortgageParticularsMapper = mortgageParticularsMapper;
    }

    @Override
    public BulkClaimPreparationResult prepare(MortgageClaimSet claimSet,
                                              MortgageClaimSet.MortgageClaim claim,
                                              OrganisationDetailsResponse organisation) {
        String cui = claim.getCustomerUniqueIdentifier();
        BulkErrorCollector errors = BulkErrorCollector.forMortgageClaim(claimSet, claim);

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
        if (country == LegislativeCountry.WALES && !featureToggleService.isEnabled(FeatureFlag.WALES_MAKE_A_CLAIM)) {
            return reject(cui, BulkFailReason.CLAIM_KIND_NOT_ENABLED, errors, "Property");
        }

        if (!properties.getEnabledClaimantTypes().contains(ClaimantType.MORTGAGE_LENDER)) {
            return reject(cui, BulkFailReason.CLAIMANT_TYPE_NOT_ELIGIBLE, errors, "ClaimantDesignation");
        }

        PCSCase pcsCase = PCSCase.builder().build();
        eligibility.applyTo(pcsCase);
        pcsCase.setClaimantType(mortgageLenderType(country));
        BulkClaimExtras extras = populate(claim, organisation, pcsCase, country, errors);

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

    private BulkClaimExtras populate(MortgageClaimSet.MortgageClaim claim,
                                     OrganisationDetailsResponse organisation,
                                     PCSCase pcsCase,
                                     LegislativeCountry country,
                                     BulkErrorCollector errors) {
        final ParticularsMortgagedType particulars = claim.getParticularsOfClaim();

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

        final var common = commonParticularsMapper.apply(particulars, country, pcsCase, PARTICULARS_PATH, errors);
        underlesseeMapper.map(particulars.getUnderlesseeOrMortgagee(), PARTICULARS_PATH + "/UnderlesseeOrMortgagee",
                              errors).applyTo(pcsCase);
        pcsCase.setStatementOfTruth(statementOfTruthMapper.map(
            particulars.getClaimStatementOfTruth(), PARTICULARS_PATH + "/ClaimStatementOfTruth", errors));

        List<PaymentHistoryLine> paymentHistory = commonParticularsMapper.paymentHistory(
            particulars.getPaymentHistory());
        mortgageParticularsMapper.requirePaymentHistory(paymentHistory, PARTICULARS_PATH + "/PaymentHistory", errors);

        return new BulkClaimExtras(
            SCHEMA_VERSION,
            claim.getClaimantDesignation().value(),
            claimant.legacyPcolClaimantReference(),
            claimant.legacyPcolSolicitorReference(),
            claimant.solicitorFirmName(),
            common.claimingCosts(),
            null,
            claimant.claimantExtras(),
            defendants.extras(),
            paymentHistory,
            mortgageParticularsMapper.map(particulars, paymentHistory, PARTICULARS_PATH, errors),
            mortgageParticularsMapper.occupants(claim.getOccupant(), "Occupant", errors)
        );
    }

    private static DynamicStringList mortgageLenderType(LegislativeCountry country) {
        List<DynamicStringListElement> items = Arrays.stream(ClaimantType.values())
            .filter(type -> type.isApplicableFor(country))
            .map(type -> DynamicStringListElement.builder().code(type.name()).label(type.getLabel()).build())
            .toList();
        ClaimantType selected = ClaimantType.MORTGAGE_LENDER;
        return DynamicStringList.builder()
            .value(DynamicStringListElement.builder().code(selected.name()).label(selected.getLabel()).build())
            .listItems(items)
            .build();
    }
}
