package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.CLAIMANT;
import static uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole.DEFENDANT;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Optional;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;

import java.util.Arrays;
import java.util.Map;
import uk.gov.hmcts.ccd.sdk.api.CCDAccessGroup;

@Getter
public enum GroupAccessType implements CCDAccessGroup {

    LOCAL_AUTHORITY_CLAIMANT_ACCESS("LOCALAUTH_PROFILE", 1),
    REAL_ESTATE_ORG_CLAIMANT_ACCESS("OTHER_REALT_PROFILE", 2),
    PROPERTY_CONSTRUCTION_ORG_CLAIMANT_ACCESS("OTHER_PROP_PROFILE", 3),
    NOT_FOR_PROFIT_ORG_CLAIMANT_ACCESS("OTHER_NFP_PROFILE", 4),
    CHARITY_ORG_CLAIMANT_ACCESS("OTHER_CHARITY_PROFILE", 5),
    ACCOMMODATION_FOOD_ORG_CLAIMANT_ACCESS("OTHER_ACCOM_PROFILE", 6),
    ACCOUNTANCY_BANKING_FINANCE_ORG_CLAIMANT_ACCESS("OTHER_ACCOUNT_PROFILE", 7),
    ADMIN_SUPPORT_ORG_CLAIMANT_ACCESS("OTHER_ADMIN_PROFILE", 8),
    BUSINESS_CONSULTING_MANAGEMENT_ORG_CLAIMANT_ACCESS("OTHER_BUSI_PROFILE", 9),
    CREATIVE_ARTS_DESIGN_ORG_CLAIMANT_ACCESS("OTHER_CREATI_PROFILE", 10),
    EDUCATION_ORG_CLAIMANT_ACCESS("OTHER_EDU_PROFILE", 11),
    ENERGY_UTILITIES_ORG_CLAIMANT_ACCESS("OTHER_ENERGY_PROFILE", 12),
    ENGINEERING_MANUFACTURING_ORG_CLAIMANT_ACCESS("OTHER_ENGG_PROFILE", 13),
    ENVIRONMENT_AGRICULTURE_ORG_CLAIMANT_ACCESS("OTHER_ENV_PROFILE", 14),
    FINANCIAL_SERVICES_ORG_CLAIMANT_ACCESS("OTHER_FIN_PROFILE", 15),
    HEALTHCARE_ORG_CLAIMANT_ACCESS("OTHER_HEALTH_PROFILE", 16),
    HOSPITALITY_EVENTS_ORG_CLAIMANT_ACCESS("OTHER_HOSP_PROFILE", 17),
    IT_COMMUNICATIONS_ORG_CLAIMANT_ACCESS("OTHER_ITCOMM_PROFILE", 18),
    LAW_ENFORCEMENT_SECURITY_ORG_CLAIMANT_ACCESS("OTHER_LAW_PROFILE", 19),
    LEISURE_SPORT_TOURISM_ORG_CLAIMANT_ACCESS("OTHER_LEISURE_PROFILE", 20),
    MARKETING_ADVERTISING_PR_ORG_CLAIMANT_ACCESS("OTHER_MARK_PROFILE", 21),
    MEDIA_INTERNET_ORG_CLAIMANT_ACCESS("OTHER_MEDIA_PROFILE", 22),
    MINING_QUARRYING_ORG_CLAIMANT_ACCESS("OTHER_MINING_PROFILE", 23),
    PUBLIC_SECTOR_DEFENCE_ORG_CLAIMANT_ACCESS("OTHER_PUBDEF_PROFILE", 24),
    PUBLIC_SERVICES_ADMINISTRATION_ORG_CLAIMANT_ACCESS("OTHER_PUBADM_PROFILE", 25),
    RECRUITMENT_HR_ORG_CLAIMANT_ACCESS("OTHER_REC_PROFILE", 26),
    RETAIL_WHOLESALE_ORG_CLAIMANT_ACCESS("OTHER_RETAIL_PROFILE", 27),
    SALES_ORG_CLAIMANT_ACCESS("OTHER_SALES_PROFILE", 28),
    SCIENCE_PHARMACEUTICALS_ORG_CLAIMANT_ACCESS("OTHER_SCIENCE_PROFILE", 29),
    SOCIAL_CARE_ORG_CLAIMANT_ACCESS("OTHER_SOCIAL_PROFILE", 30),
    TRANSPORT_LOGISTICS_ORG_CLAIMANT_ACCESS("OTHER_TRANSP_PROFILE", 31),

    SOLICITOR_ORG_CLAIMANT_ACCESS(
        "SOLICITOR_PROFILE", CLAIMANT, "solicitor-org-claimant-access", "claimant-solicitor",
        "Claimant access",
        "Grants users access to all possession cases in which this organisation is the claimant or acts "
            + "for the claimant",
        32, false, false, true, true
    ),
    SOLICITOR_ORG_DEFENDANT_ACCESS(
        "SOLICITOR_PROFILE", DEFENDANT, "solicitor-org-defendant-access", "defendant-solicitor",
        "Defendant access",
        "Grants users access to all possession cases in which this organisation is the defendant or acts "
            + "for the defendant",
        33, false, false, true, true
    ),

    DUTY_ADVISOR_ACCESS(
        "SOLICITOR_PROFILE", null, "duty-advisor-access", "duty-advisor-request",
        "In court duty advisor access",
        "Where the organisation is the provider of the Housing Loss Prevention Advice Service (HLPAS) "
            + "In Court Duty Scheme, this grants users temporary access to possession cases.",
        34, false, false, false, true
    );

    private static final String ORG_IDENTIFIER_TEMPLATE = "$ORGID$";
    private static final String CLAIMANT_ACCESS_HINT =
        "Grants users access to all possession cases in which this organisation is the claimant or acts "
            + "for the claimant";

    private static final Map<Key, GroupAccessType> CASE_ACCESS_GROUP_MAP = buildIndex();

    private final PartyRole partyRole;
    private final String organisationProfileId;
    private final String accessTypeId;
    private final String description;
    private final String hintText;
    private final int displayOrder;
    private final boolean accessMandatory;
    private final boolean accessDefault;
    private final boolean display;
    private final boolean groupAccessEnabled;

    private final String caseAssignedRoleField;

    GroupAccessType(String orgProfileId, PartyRole partyRole, String accessTypeId,
                    String caseAssignedRoleField, String description, String hintText, int displayOrder,
                    boolean accessMandatory, boolean accessDefault, boolean display, boolean groupAccessEnabled) {
        this.partyRole = partyRole;
        this.organisationProfileId = orgProfileId;
        this.accessTypeId = accessTypeId;
        this.accessMandatory = accessMandatory;
        this.accessDefault = accessDefault;
        this.display = display;
        this.description = description;
        this.hintText = hintText;
        this.displayOrder = displayOrder;
        this.groupAccessEnabled = groupAccessEnabled;
        this.caseAssignedRoleField = caseAssignedRoleField;
    }

    GroupAccessType(String orgProfileId, int displayOrder) {
        this(orgProfileId, CLAIMANT, "prof-org-claimant-access", "claimant",
            "Claimant access", CLAIMANT_ACCESS_HINT, displayOrder,
            false, false, true, true);
    }

    private record Key(String organisationProfileId, PartyRole partyRole) { }

    private static Map<Key, GroupAccessType> buildIndex() {
        return Arrays.stream(values())
            .filter(accessType -> accessType.partyRole != null)
            .collect(toUnmodifiableMap(
                accessType -> new Key(accessType.organisationProfileId, accessType.partyRole),
                identity()));
    }

    /**
     * The group ID template for an organisation profile acting in a party role, empty where the
     * combination has no access type. Keyed lookup, so selection does not depend on the order these
     * constants are declared in.
     */
    public static Optional<String> caseAccessGroupIdFor(String orgProfileId, PartyRole partyRole,
                                                        String organisationId) {
        return Optional.ofNullable(orgProfileId)
            .map(profileId -> new Key(profileId, partyRole))
            .map(CASE_ACCESS_GROUP_MAP::get)
            .map(groupAccessType ->
                     groupAccessType.getCaseAccessGroupIdTemplate().replace(ORG_IDENTIFIER_TEMPLATE, organisationId));
    }

    /**
     * Builds the case access group ID template from this constant's own {@code accessTypeId} and
     * group role, e.g. {@code "PCS:PCS:solicitor-org-claimant-access:claimant-solicitor:$ORGID$"}.
     */
    @Override
    public String getCaseAccessGroupIdTemplate() {
        return "PCS:PCS:" + accessTypeId + ":" + caseAssignedRoleField + ":$ORGID$";
    }


    /**
     * Cannot be left blank: the definition reader parses this column unconditionally and an empty
     * value fails the import with DateTimeParseException. Far future so it is not a live expiry -
     * every access type stops working on this date, so move it rather than let it arrive.
     */
    @Override
    public String getLiveTo() {
        return "01/01/2099";
    }
}
