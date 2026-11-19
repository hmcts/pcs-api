package uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.bulkclaim.model.BulkClaimExtras.PartyExtras;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ClaimantType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.ContactDetailsType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.NameType;
import uk.gov.hmcts.reform.pcs.bulkclaim.xml.generated.SolicitorType;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.OrganisationProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.ClaimantNameRules;
import uk.gov.hmcts.reform.pcs.reference.dto.OrganisationDetailsResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.bulkclaim.prepare.common.BulkErrorCollector.INVALID_VALUE;

/**
 * Maps the claimant (and optional solicitor) to the claimant sections of a {@link PCSCase}. The claimant name
 * and contact details come from the PRD organisation unless the claim supplies its own. Only a solicitor
 * organisation may supply a Solicitor block; for any other organisation it is ignored apart from the firm name and
 * PCOL reference, which are kept as legacy data.
 */
@Component
public class BulkClaimantMapper {

    private final BulkAddressMapper addressMapper;

    public BulkClaimantMapper(BulkAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    /**
     * The claimant sections of the case, plus the PCOL-only data that has no home on the case.
     * The claimant name is the supplied company or individual name, recorded as an override when it differs
     * from the PRD organisation name and as the fallback name when PRD has none.
     */
    public record ClaimantMapping(ClaimantInformation claimantInformation,
                                  ClaimantContactPreferences contactPreferences,
                                  PartyExtras claimantExtras,
                                  String solicitorFirmName,
                                  String legacyPcolClaimantReference,
                                  String legacyPcolSolicitorReference,
                                  boolean noticeOfActingRequired) {

        public void applyTo(PCSCase pcsCase) {
            pcsCase.setClaimantInformation(claimantInformation);
            pcsCase.setClaimantContactPreferences(contactPreferences);
        }
    }

    /**
     * Maps the claimant and solicitor to the claimant sections of the case.
     *
     * @param path path of the claimant element relative to the claim, for example {@code Claimant}
     * @param solicitorPath path of the solicitor element relative to the claim
     */
    public ClaimantMapping map(ClaimantType claimant,
                               SolicitorType solicitor,
                               OrganisationDetailsResponse organisation,
                               String path,
                               String solicitorPath,
                               BulkErrorCollector errors) {

        ClaimantInformation information = mapInformation(claimant, organisation, path, errors);

        boolean solicitorIgnored = solicitor != null && !isSolicitorOrganisation(organisation);
        ContactDetailsType solicitorContact = solicitor != null && !solicitorIgnored
            ? solicitor.getContactDetails() : null;
        ContactDetailsType claimantContact = claimant.getContactDetails();
        ClaimantContactPreferences preferences = mapContactPreferences(
            claimant, solicitorContact, claimantContact, organisation, path, solicitorPath, errors);

        if (solicitor != null) {
            errors.requiredWithMaxLength(solicitorPath + "/Firm", solicitor.getFirm(), BulkLimits.SIGNATORY,
                                         "Solicitor firm");
        }

        return new ClaimantMapping(
            information,
            preferences,
            claimantExtras(claimant),
            solicitor != null ? solicitor.getFirm() : null,
            claimant.getPCOLReferenceNumber(),
            solicitor != null ? solicitor.getPCOLReferenceNumber() : null,
            solicitorIgnored
        );
    }

    private static boolean isSolicitorOrganisation(OrganisationDetailsResponse organisation) {
        return organisation != null && organisation.getOrganisationProfileIds() != null
            && organisation.getOrganisationProfileIds().contains(OrganisationProfile.SOLICITOR_PROFILE.getId());
    }

    private ClaimantInformation mapInformation(ClaimantType claimant,
                                               OrganisationDetailsResponse organisation,
                                               String path,
                                               BulkErrorCollector errors) {
        String suppliedName = suppliedName(claimant, path, errors);
        String organisationName = organisation != null ? StringUtils.trimToNull(organisation.getName()) : null;

        ClaimantInformation.ClaimantInformationBuilder builder = ClaimantInformation.builder();
        if (organisationName == null) {
            builder.orgNameFound(YesOrNo.NO).fallbackClaimantName(suppliedName);
        } else {
            builder.orgNameFound(YesOrNo.YES).claimantName(organisationName);
            if (suppliedName == null || organisationName.equalsIgnoreCase(suppliedName)) {
                builder.isClaimantNameCorrect(VerticalYesNo.YES);
            } else {
                builder.isClaimantNameCorrect(VerticalYesNo.NO).overriddenClaimantName(suppliedName);
            }
        }

        ClaimantInformation information = builder.build();
        errors.maxLength(path, ClaimantNameRules.resolveClaimantName(information).name(), BulkLimits.NAME,
                         "Claimant name");
        return information;
    }

    private String suppliedName(ClaimantType claimant, String path, BulkErrorCollector errors) {
        if (StringUtils.isNotBlank(claimant.getCompanyName())) {
            return claimant.getCompanyName().trim();
        }
        NameType individual = claimant.getIndividualName();
        if (individual == null) {
            errors.add(path, BulkErrorCollector.REQUIRED, "Claimant name is required");
            return null;
        }
        return (StringUtils.defaultString(individual.getFirstName()) + " "
            + StringUtils.defaultString(individual.getLastName())).trim();
    }

    private ClaimantContactPreferences mapContactPreferences(ClaimantType claimant,
                                                             ContactDetailsType solicitorContact,
                                                             ContactDetailsType claimantContact,
                                                             OrganisationDetailsResponse organisation,
                                                             String path,
                                                             String solicitorPath,
                                                             BulkErrorCollector errors) {
        AddressUK organisationAddress = organisationAddress(organisation);
        String email = claimant.getContactEmailAddress();
        errors.requiredWithMaxLength(path + "/ContactEmailAddress", email, BulkLimits.EMAIL,
                                     "Claimant contact email address");

        ClaimantContactPreferences.ClaimantContactPreferencesBuilder builder = ClaimantContactPreferences.builder()
            .organisationAddress(organisationAddress)
            .orgAddressFound(YesOrNo.from(organisationAddress != null))
            .claimantContactEmail(organisation != null && organisation.getSuperUser() != null
                                      ? organisation.getSuperUser().getEmail() : null)
            .isCorrectClaimantContactEmail(VerticalYesNo.NO)
            .overriddenClaimantContactEmail(email);

        ContactDetailsType addressSource = firstWithAddress(solicitorContact, claimantContact);
        String addressPath = addressSource == null ? null
            : (addressSource == solicitorContact ? solicitorPath : path) + "/ContactDetails/Address";
        if (addressSource != null) {
            builder.isCorrectClaimantContactAddress(VerticalYesNo.NO)
                .overriddenClaimantContactAddress(
                    addressMapper.toValidAddress(addressSource.getAddress(), addressPath, errors));
        } else if (organisationAddress != null) {
            builder.isCorrectClaimantContactAddress(VerticalYesNo.YES);
        } else {
            errors.add(path + "/ContactDetails", BulkErrorCollector.REQUIRED,
                       "A contact address is required because the organisation has no address on record");
        }

        String phone = firstPhone(addressSource, solicitorContact, claimantContact);
        if (phone == null) {
            builder.claimantProvidePhoneNumber(VerticalYesNo.NO);
        } else {
            builder.claimantProvidePhoneNumber(VerticalYesNo.YES).claimantContactPhoneNumber(phone);
            if (!BulkLimits.PHONE_PATTERN.matcher(phone).matches()) {
                errors.add(path + "/ContactDetails/TelephoneNumber", INVALID_VALUE,
                           "Telephone number must be an 11 digit UK number starting with 0");
            }
        }
        return builder.build();
    }

    private static ContactDetailsType firstWithAddress(ContactDetailsType... candidates) {
        return Stream.of(candidates)
            .filter(contact -> contact != null && contact.getAddress() != null)
            .findFirst()
            .orElse(null);
    }

    private static String firstPhone(ContactDetailsType preferred, ContactDetailsType... others) {
        return Stream.concat(Stream.of(preferred), Stream.of(others))
            .filter(contact -> contact != null && StringUtils.isNotBlank(contact.getTelephoneNumber()))
            .map(contact -> contact.getTelephoneNumber().replaceAll("\\s", ""))
            .findFirst()
            .orElse(null);
    }

    private static AddressUK organisationAddress(OrganisationDetailsResponse organisation) {
        return Optional.ofNullable(organisation)
            .map(OrganisationDetailsResponse::getContactInformation)
            .filter(contacts -> !contacts.isEmpty())
            .map(List::getFirst)
            .map(contact -> AddressUK.builder()
                .addressLine1(contact.getAddressLine1())
                .addressLine2(contact.getAddressLine2())
                .addressLine3(contact.getAddressLine3())
                .postTown(contact.getTownCity())
                .county(contact.getCounty())
                .country(contact.getCountry())
                .postCode(contact.getPostCode())
                .build())
            .orElse(null);
    }

    private static PartyExtras claimantExtras(ClaimantType claimant) {
        NameType name = claimant.getIndividualName();
        ContactDetailsType contact = claimant.getContactDetails();
        return new PartyExtras(
            name != null ? name.getTitle() : null,
            name != null ? name.getInitials() : null,
            name != null ? name.getOtherNames() : null,
            contact != null ? contact.getDX() : null,
            contact != null ? contact.getFaxNumber() : null,
            null,
            null,
            null
        );
    }
}
