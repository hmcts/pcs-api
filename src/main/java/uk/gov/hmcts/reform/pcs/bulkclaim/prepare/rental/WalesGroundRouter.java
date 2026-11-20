package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.rental;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector;
import uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkLimits;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.EstateManagementGroundType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.GroundsType;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.EstateManagementGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsReasonsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.MandatoryGroundWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractMandatoryGroundsWales;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.GroundsRules.WalesGroundsRouting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Routes the flat list of grounds on a bulk claim for a property in Wales to the standard or secure occupation
 * contract grounds and reasons of the make a claim journey, then runs the same grounds validation as that journey.
 */
@Component
public class WalesGroundRouter {

    private final GroundsRules groundsRules;

    public WalesGroundRouter(GroundsRules groundsRules) {
        this.groundsRules = groundsRules;
    }

    /**
     * Routes the claimed grounds for the occupation contract type onto the case and sets the ASB questions and
     * reasons page flags.
     *
     * @param path path of the Grounds element relative to the claim
     */
    public void route(OccupationLicenceTypeWales contract,
                      GroundsType grounds,
                      PCSCase target,
                      String path,
                      BulkErrorCollector errors) {

        if (!grounds.getRentArrearsOrBreach().isEmpty()) {
            errors.add(path + "/RentArrearsOrBreach", BulkErrorCollector.NOT_ALLOWED,
                       "RentArrearsOrBreach only applies to secure and flexible tenancies in England");
        }

        List<Entry> entries = entries(grounds, path, errors);
        Optional<WalesGroundsRouting> routing;
        boolean antisocialBehaviour;
        if (contract == OccupationLicenceTypeWales.SECURE_CONTRACT) {
            SecureContractGroundsForPossessionWales secure = secure(entries, target, path, errors);
            routing = groundsRules.deriveSecureContractGroundsRouting(secure);
            antisocialBehaviour = groundsRules.hasWalesAntisocialBehaviourGround(null, secure);
        } else {
            GroundsForPossessionWales standard = standard(entries, target, path, errors);
            routing = groundsRules.deriveWalesGroundsRouting(standard);
            antisocialBehaviour = groundsRules.hasWalesAntisocialBehaviourGround(standard, null);
        }

        target.setShowASBQuestionsPageWales(YesOrNo.from(antisocialBehaviour));
        target.setShowReasonsForGroundsPageWales(routing.map(WalesGroundsRouting::showReasonsPage)
                                                     .orElse(YesOrNo.NO));
    }

    private record Entry(GroundType ground, String path) {
        String code() {
            return ground.getCode();
        }
    }

    private List<Entry> entries(GroundsType grounds, String path, BulkErrorCollector errors) {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < grounds.getGround().size(); i++) {
            GroundType ground = grounds.getGround().get(i);
            String groundPath = path + "/" + BulkErrorCollector.indexed("Ground", i);
            if (!seen.add(ground.getCode())) {
                errors.add(groundPath + "/Code", BulkErrorCollector.INVALID_VALUE,
                           "Ground " + ground.getCode() + " is listed more than once");
                continue;
            }
            entries.add(new Entry(ground, groundPath));
        }
        return entries;
    }

    private void unknown(Entry entry, OccupationLicenceTypeWales contract, BulkErrorCollector errors) {
        errors.add(entry.path() + "/Code", BulkErrorCollector.INVALID_VALUE,
                   "Ground " + entry.code() + " is not valid for a " + contract + " in Wales");
    }

    /** The reason text for a ground that needs one. */
    private String reason(Entry entry, BulkErrorCollector errors) {
        errors.requiredWithMaxLength(entry.path() + "/Reason", entry.ground().getReason(), BulkLimits.MEDIUM_TEXT,
                                     "Reason for ground " + entry.code());
        return entry.ground().getReason();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String code) {
        try {
            return Enum.valueOf(type, code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void rejectEstateManagementGrounds(Entry entry, BulkErrorCollector errors) {
        if (!entry.ground().getEstateManagementGround().isEmpty()) {
            errors.add(entry.path() + "/EstateManagementGround", BulkErrorCollector.NOT_ALLOWED,
                       "Estate management grounds only apply to ground ESTATE_MANAGEMENT_GROUNDS_S160");
        }
    }

    /** The estate management grounds relied on, each with its own reason. */
    private Map<EstateManagementGroundsWales, String> estateManagementGrounds(Entry entry,
                                                                             BulkErrorCollector errors) {
        if (entry.ground().getReason() != null) {
            errors.add(entry.path() + "/Reason", BulkErrorCollector.NOT_ALLOWED,
                       "Ground " + entry.code() + " takes a reason on each EstateManagementGround, not on the ground");
        }
        Map<EstateManagementGroundsWales, String> selected = new EnumMap<>(EstateManagementGroundsWales.class);
        List<EstateManagementGroundType> grounds = entry.ground().getEstateManagementGround();
        for (int i = 0; i < grounds.size(); i++) {
            EstateManagementGroundType estateGround = grounds.get(i);
            String groundPath = entry.path() + "/" + BulkErrorCollector.indexed("EstateManagementGround", i);
            EstateManagementGroundsWales code = parse(EstateManagementGroundsWales.class,
                                                      String.valueOf(estateGround.getCode().value()));
            if (code == null) {
                errors.add(groundPath + "/Code", BulkErrorCollector.INVALID_VALUE,
                           estateGround.getCode().value() + " is not an estate management ground");
                continue;
            }
            errors.requiredWithMaxLength(groundPath + "/Reason", estateGround.getReason(), BulkLimits.MEDIUM_TEXT,
                                         "Reason for estate management ground " + code);
            if (selected.containsKey(code)) {
                errors.add(groundPath + "/Code", BulkErrorCollector.INVALID_VALUE,
                           "Estate management ground " + code + " is listed more than once");
            } else {
                selected.put(code, estateGround.getReason());
            }
        }
        return selected;
    }

    // ---- standard and other contracts ----

    private GroundsForPossessionWales standard(List<Entry> entries,
                                               PCSCase target,
                                               String path,
                                               BulkErrorCollector errors) {
        Set<DiscretionaryGroundWales> discretionary = EnumSet.noneOf(DiscretionaryGroundWales.class);
        Set<MandatoryGroundWales> mandatory = EnumSet.noneOf(MandatoryGroundWales.class);
        Set<EstateManagementGroundsWales> estate = EnumSet.noneOf(EstateManagementGroundsWales.class);
        GroundsReasonsWales.GroundsReasonsWalesBuilder reasons = GroundsReasonsWales.builder();

        for (Entry entry : entries) {
            DiscretionaryGroundWales discretionaryGround = parse(DiscretionaryGroundWales.class, entry.code());
            MandatoryGroundWales mandatoryGround = parse(MandatoryGroundWales.class, entry.code());
            if (discretionaryGround != null) {
                discretionary.add(discretionaryGround);
                if (discretionaryGround == DiscretionaryGroundWales.ESTATE_MANAGEMENT_GROUNDS_S160) {
                    estate.addAll(standardEstateReasons(entry, reasons, errors));
                } else {
                    rejectEstateManagementGrounds(entry, errors);
                    if (discretionaryGround == DiscretionaryGroundWales.OTHER_BREACH_OF_CONTRACT_S157) {
                        reasons.otherBreachSection157Reason(reason(entry, errors));
                    }
                }
            } else if (mandatoryGround != null) {
                mandatory.add(mandatoryGround);
                rejectEstateManagementGrounds(entry, errors);
                standardMandatoryReason(mandatoryGround, entry, reasons, errors);
            } else {
                unknown(entry, OccupationLicenceTypeWales.STANDARD_CONTRACT, errors);
            }
        }

        GroundsForPossessionWales possessionGrounds = GroundsForPossessionWales.builder()
            .discretionaryGrounds(discretionary)
            .mandatoryGrounds(mandatory)
            .estateManagementGrounds(estate)
            .build();
        target.setGroundsForPossessionWales(possessionGrounds);
        target.setGroundsReasonsWales(reasons.build());
        errors.addAll(path, BulkErrorCollector.REQUIRED, groundsRules.validateWalesGrounds(possessionGrounds));
        return possessionGrounds;
    }

    private void standardMandatoryReason(MandatoryGroundWales ground,
                                         Entry entry,
                                         GroundsReasonsWales.GroundsReasonsWalesBuilder reasons,
                                         BulkErrorCollector errors) {
        switch (ground) {
            case FAILURE_TO_GIVE_UP_POSSESSION_S170 -> reasons.failToGiveUpS170Reason(reason(entry, errors));
            case LANDLORD_NOTICE_PERIODIC_S178 -> reasons.landlordNoticePeriodicS178Reason(reason(entry, errors));
            case LANDLORD_NOTICE_FT_END_S186 -> reasons.landlordNoticeFtEndS186Reason(reason(entry, errors));
            case FAIL_TO_GIVE_UP_BREAK_NOTICE_S191 ->
                reasons.failToGiveUpBreakNoticeS191Reason(reason(entry, errors));
            case LANDLORD_BREAK_CLAUSE_S199 -> reasons.landlordBreakClauseS199Reason(reason(entry, errors));
            case CONVERTED_FIXED_TERM_SCH12_25B2 ->
                reasons.convertedFixedTermSch1225B2Reason(reason(entry, errors));
            case SERIOUS_ARREARS_PERIODIC_S181, SERIOUS_ARREARS_FIXED_TERM_S187 -> { }
        }
    }

    private Set<EstateManagementGroundsWales> standardEstateReasons(
        Entry entry, GroundsReasonsWales.GroundsReasonsWalesBuilder reasons, BulkErrorCollector errors) {

        Map<EstateManagementGroundsWales, String> selected = estateManagementGrounds(entry, errors);
        for (Map.Entry<EstateManagementGroundsWales, String> estateGround : selected.entrySet()) {
            String text = estateGround.getValue();
            switch (estateGround.getKey()) {
                case BUILDING_WORKS -> reasons.buildingWorksReason(text);
                case REDEVELOPMENT_SCHEMES -> reasons.redevelopmentSchemesReason(text);
                case CHARITIES -> reasons.charitiesReason(text);
                case DISABLED_SUITABLE_DWELLING -> reasons.disabledSuitableDwellingReason(text);
                case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.housingAssociationsAndTrustsReason(text);
                case SPECIAL_NEEDS_DWELLINGS -> reasons.specialNeedsDwellingsReason(text);
                case RESERVE_SUCCESSORS -> reasons.reserveSuccessorsReason(text);
                case JOINT_CONTRACT_HOLDERS -> reasons.jointContractHoldersReason(text);
                case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.otherEstateManagementReasonsReason(text);
            }
        }
        return selected.keySet();
    }

    // ---- secure contracts ----

    private SecureContractGroundsForPossessionWales secure(List<Entry> entries,
                                                           PCSCase target,
                                                           String path,
                                                           BulkErrorCollector errors) {
        Set<SecureContractDiscretionaryGroundsWales> discretionary =
            EnumSet.noneOf(SecureContractDiscretionaryGroundsWales.class);
        Set<SecureContractMandatoryGroundsWales> mandatory =
            EnumSet.noneOf(SecureContractMandatoryGroundsWales.class);
        Set<EstateManagementGroundsWales> estate = EnumSet.noneOf(EstateManagementGroundsWales.class);
        GroundsReasonsWales.GroundsReasonsWalesBuilder reasons = GroundsReasonsWales.builder();

        for (Entry entry : entries) {
            SecureContractDiscretionaryGroundsWales discretionaryGround =
                parse(SecureContractDiscretionaryGroundsWales.class, entry.code());
            SecureContractMandatoryGroundsWales mandatoryGround =
                parse(SecureContractMandatoryGroundsWales.class, entry.code());
            if (discretionaryGround != null) {
                discretionary.add(discretionaryGround);
                if (discretionaryGround == SecureContractDiscretionaryGroundsWales.ESTATE_MANAGEMENT_GROUNDS_S160) {
                    estate.addAll(secureEstateReasons(entry, reasons, errors));
                } else {
                    rejectEstateManagementGrounds(entry, errors);
                    if (discretionaryGround == SecureContractDiscretionaryGroundsWales.OTHER_BREACH_OF_CONTRACT_S157) {
                        reasons.secureOtherBreachOfContractReason(reason(entry, errors));
                    }
                }
            } else if (mandatoryGround != null) {
                mandatory.add(mandatoryGround);
                rejectEstateManagementGrounds(entry, errors);
                String text = reason(entry, errors);
                switch (mandatoryGround) {
                    case FAILURE_TO_GIVE_UP_POSSESSION_S170 ->
                        reasons.secureFailureToGiveUpPossessionSection170Reason(text);
                    case LANDLORD_NOTICE_S186 -> reasons.secureLandlordNoticeSection186Reason(text);
                    case FAILURE_TO_GIVE_UP_POSSESSION_S191 ->
                        reasons.secureFailureToGiveUpPossessionSection191Reason(text);
                    case LANDLORD_NOTICE_S199 -> reasons.secureLandlordNoticeSection199Reason(text);
                }
            } else {
                unknown(entry, OccupationLicenceTypeWales.SECURE_CONTRACT, errors);
            }
        }

        SecureContractGroundsForPossessionWales possessionGrounds = SecureContractGroundsForPossessionWales.builder()
            .discretionaryGrounds(discretionary)
            .mandatoryGrounds(mandatory)
            .estateManagementGrounds(estate)
            .build();
        target.setSecureContractGroundsForPossessionWales(possessionGrounds);
        target.setGroundsReasonsWales(reasons.build());
        errors.addAll(path, BulkErrorCollector.REQUIRED, groundsRules.validateSecureContractGrounds(possessionGrounds));
        return possessionGrounds;
    }

    private Set<EstateManagementGroundsWales> secureEstateReasons(
        Entry entry, GroundsReasonsWales.GroundsReasonsWalesBuilder reasons, BulkErrorCollector errors) {

        Map<EstateManagementGroundsWales, String> selected = estateManagementGrounds(entry, errors);
        for (Map.Entry<EstateManagementGroundsWales, String> estateGround : selected.entrySet()) {
            String text = estateGround.getValue();
            switch (estateGround.getKey()) {
                case BUILDING_WORKS -> reasons.secureBuildingWorksReason(text);
                case REDEVELOPMENT_SCHEMES -> reasons.secureRedevelopmentSchemesReason(text);
                case CHARITIES -> reasons.secureCharitiesReason(text);
                case DISABLED_SUITABLE_DWELLING -> reasons.secureDisabledSuitableDwellingReason(text);
                case HOUSING_ASSOCIATIONS_AND_TRUSTS -> reasons.secureHousingAssociationsAndTrustsReason(text);
                case SPECIAL_NEEDS_DWELLINGS -> reasons.secureSpecialNeedsDwellingsReason(text);
                case RESERVE_SUCCESSORS -> reasons.secureReserveSuccessorsReason(text);
                case JOINT_CONTRACT_HOLDERS -> reasons.secureJointContractHoldersReason(text);
                case OTHER_ESTATE_MANAGEMENT_REASONS -> reasons.secureOtherEstateManagementReasonsReason(text);
            }
        }
        return selected.keySet();
    }
}
