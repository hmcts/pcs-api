
package uk.gov.hmcts.reform.pcs.ccd.service;

import org.instancio.Instancio;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for generating fully populated test case instances using Instancio.
 * TVR: Profile this to be under dev and preview environments only !!!
 */
@Service
public class CaseCreationService {

    private static final String DEFAULT_EMAIL = "test@example.com";

    private final Map<Class<?>, Map<Field, String>> labelFieldsWithLabelsByClass;
    private final Map<Class<?>, Set<Field>> emailFieldsByClass;

    public CaseCreationService() {
        this.labelFieldsWithLabelsByClass = identifyLabelFieldsWithLabels(PCSCase.class,
                                                                          new HashMap<>(), new HashSet<>());
        this.emailFieldsByClass = identifyEmailFields(PCSCase.class, new HashMap<>(), new HashSet<>());
    }

    public PCSCase generateTestPCSCase(PCSCase fromEvent) {
        try {
            PCSCase pcsCase = Instancio.create(PCSCase.class);
            setLabelFieldsInObject(pcsCase, new HashSet<>());
            setEmailFieldsInObject(pcsCase, new HashSet<>());
            sanitizeInvalidFields(pcsCase);

            pcsCase.setPropertyAddress(createDefaultAddress());
            pcsCase.setFeeAmount("123.45");

            copyAllFieldsFromEvent(fromEvent, pcsCase);

            return fromEvent;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test PCSCase", e);
        }
    }

    private void copyAllFieldsFromEvent(PCSCase fromEvent, PCSCase pcsCase) {
        if (fromEvent == null) {
            return;
        }

        for (Field field : PCSCase.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(fromEvent);
                field.set(pcsCase, value);
            } catch (IllegalAccessException e) {
                // Ignore fields that can't be accessed
            }
        }
    }

    private void sanitizeInvalidFields(PCSCase pcsCase) {
        // Clear phone number - Instancio generates invalid phone formats
        pcsCase.setClaimantContactPhoneNumber(null);

        // Clear money fields - Instancio generates invalid currency formats
        pcsCase.setCurrentRent(null);
        pcsCase.setDailyRentChargeAmount(null);
        pcsCase.setAmendedDailyRentChargeAmount(null);
        pcsCase.setCalculatedDailyRentChargeAmount(null);
        pcsCase.setTotalRentArrears(null);

        // Clear document collections - CCD requires specific document structure
        pcsCase.setNoticeDocuments(null);
        pcsCase.setTenancyLicenceDocuments(null);
        pcsCase.setRentStatementDocuments(null);
        pcsCase.setAdditionalDocuments(null);

        // Clear third party payment sources - complex validation
        pcsCase.setThirdPartyPaymentSources(null);

        // Clear parties collection
        pcsCase.setParties(null);

        // Clear occupation licence documents (Wales)
        if (pcsCase.getOccupationLicenceDetailsWales() != null) {
            pcsCase.getOccupationLicenceDetailsWales().setLicenceDocuments(null);
        }
    }

    private Map<Class<?>, Map<Field, String>> identifyLabelFieldsWithLabels(
        Class<?> clazz,
        Map<Class<?>, Map<Field, String>> accumulator,
        Set<Class<?>> visited) {

        if (clazz == null || clazz == Object.class || visited.contains(clazz)) {
            return accumulator;
        }
        visited.add(clazz);

        Map<Field, String> labelFieldsForClass = new HashMap<>();

        for (Field field : clazz.getDeclaredFields()) {
            CCD ccdAnnotation = field.getAnnotation(CCD.class);

            if (ccdAnnotation != null && ccdAnnotation.typeOverride() == FieldType.Label) {
                field.setAccessible(true);
                String labelValue = ccdAnnotation.label();
                labelFieldsForClass.put(field, labelValue);
            }

            Class<?> fieldType = field.getType();
            if (!fieldType.isPrimitive()
                && !fieldType.getName().startsWith("java.lang")
                && !fieldType.getName().startsWith("java.util")
                && !fieldType.getName().startsWith("java.time")
                && !fieldType.isEnum()) {
                identifyLabelFieldsWithLabels(fieldType, accumulator, visited);
            }
        }
        if (!labelFieldsForClass.isEmpty()) {
            accumulator.put(clazz, labelFieldsForClass);
        }
        return accumulator;
    }

    private Map<Class<?>, Set<Field>> identifyEmailFields(
        Class<?> clazz,
        Map<Class<?>, Set<Field>> accumulator,
        Set<Class<?>> visited) {

        if (clazz == null || clazz == Object.class || visited.contains(clazz)) {
            return accumulator;
        }
        visited.add(clazz);

        Set<Field> emailFieldsForClass = new HashSet<>();

        for (Field field : clazz.getDeclaredFields()) {
            CCD ccdAnnotation = field.getAnnotation(CCD.class);

            if (ccdAnnotation != null && ccdAnnotation.typeOverride() == FieldType.Email) {
                field.setAccessible(true);
                emailFieldsForClass.add(field);
            }

            Class<?> fieldType = field.getType();
            if (!fieldType.isPrimitive()
                && !fieldType.getName().startsWith("java.lang")
                && !fieldType.getName().startsWith("java.util")
                && !fieldType.getName().startsWith("java.time")
                && !fieldType.isEnum()) {
                identifyEmailFields(fieldType, accumulator, visited);
            }
        }
        if (!emailFieldsForClass.isEmpty()) {
            accumulator.put(clazz, emailFieldsForClass);
        }
        return accumulator;
    }

    private void setLabelFieldsInObject(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) {
            return;
        }
        visited.add(obj);

        Class<?> clazz = obj.getClass();
        if (clazz.getName().startsWith("java.")) {
            return;
        }
        Map<Field, String> labelFields = labelFieldsWithLabelsByClass.get(clazz);
        if (labelFields != null) {
            for (Map.Entry<Field, String> entry : labelFields.entrySet()) {
                Field field = entry.getKey();
                String labelValue = entry.getValue();
                try {
                    //field.set(obj, labelValue);
                    field.set(obj, null);
                } catch (IllegalAccessException e) {
                    // Ignore illegal access exceptions for security reasons
                }
            }
        }
        processNestedObjects(obj, clazz, visited, this::setLabelFieldsInObject);
    }

    private void setEmailFieldsInObject(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) {
            return;
        }
        visited.add(obj);
        Class<?> clazz = obj.getClass();
        if (clazz.getName().startsWith("java.")) {
            return;
        }

        Set<Field> emailFields = emailFieldsByClass.get(clazz);
        if (emailFields != null) {
            for (Field field : emailFields) {
                try {
                    field.set(obj, DEFAULT_EMAIL);
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }
        }
        processNestedObjects(obj, clazz, visited, this::setEmailFieldsInObject);
    }

    private void processNestedObjects(Object obj, Class<?> clazz, Set<Object> visited,
                                      java.util.function.BiConsumer<Object, Set<Object>> processor) {
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);

                if (fieldValue == null) {
                    continue;
                }

                if (fieldValue instanceof Collection<?>) {
                    for (Object item : (Collection<?>) fieldValue) {
                        if (item instanceof ListValue<?>) {
                            processor.accept(((ListValue<?>) item).getValue(), visited);
                        } else {
                            processor.accept(item, visited);
                        }
                    }
                } else if (!fieldValue.getClass().getName().startsWith("java.")) {
                    processor.accept(fieldValue, visited);
                }
            } catch (IllegalAccessException e) {
                // Ignore
            }
        }
    }

    private AddressUK createDefaultAddress() {
        return AddressUK.builder()
            .addressLine1("1 Rse Way,")
            .postTown("London")
            .postCode("SW11 1PD")
            .build();
    }

}
