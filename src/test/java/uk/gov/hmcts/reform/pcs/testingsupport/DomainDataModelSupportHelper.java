package uk.gov.hmcts.reform.pcs.testingsupport;

import uk.gov.hmcts.ccd.sdk.api.CCD;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DomainDataModelSupportHelper {

    private final Set<Class<?>> processedClasses = new HashSet<>();
    private final Map<String, List<CCDFieldInfo>> ccdFieldsByClass = new HashMap<>();
    private final Set<Class<?>> ignoredClassesFromMissing = new HashSet<>();
    private final Set<String> ignoredFieldsFromMissing = new HashSet<>();

    @SuppressWarnings("rawtypes")
    public DomainDataModelSupportHelper(Class clazz) {
        identifyCCDFields(clazz);
    }

    public void addClassesToIgnore(Class<?>... classes) {
        ignoredClassesFromMissing.addAll(Arrays.asList(classes));
    }

    public void addFieldsToIgnore(String... fieldNames) {
        ignoredFieldsFromMissing.addAll(Arrays.asList(fieldNames));
    }

    public List<MissingCCDFieldInfo> findMissingCCDFields(Class<?> entityClass) {
        Set<String> entityFieldNames = new HashSet<>();
        collectEntityFieldNames(entityClass, entityFieldNames, new HashSet<>());
        List<MissingCCDFieldInfo> missingFields = new ArrayList<>();
        ccdFieldsByClass.forEach((className, ccdFields) -> {
            if (!ignoredClassesFromMissing.stream()
                .filter(c -> c.getSimpleName().equalsIgnoreCase(className)).findAny().isPresent()) {
                for (CCDFieldInfo ccdField : ccdFields) {
                    if (!entityFieldNames.contains(ccdField.fieldName)
                        && !ignoredFieldsFromMissing.contains(ccdField.fieldName)) {
                        missingFields.add(new MissingCCDFieldInfo(className, ccdField));
                    }
                }
            }
        });
        return missingFields;
    }

    private void collectEntityFieldNames(Class<?> clazz, Set<String> fieldNames, Set<Class<?>> visited) {
        if (clazz == null || visited.contains(clazz) || ignoredClassesFromMissing.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            fieldNames.add(field.getName());
            Class<?> fieldType = field.getType();
            if (isCompositeType(fieldType)) {
                collectEntityFieldNames(fieldType, fieldNames, visited);
            }
        }
    }

    public void printCCDFields(PrintWriter writer) {
        writer.println("\n========================================");
        writer.println("CCD Annotated Fields Report From Domain :");
        writer.println("========================================\n");
        for (Map.Entry<String, List<CCDFieldInfo>> entry : ccdFieldsByClass.entrySet()) {
            String className = entry.getKey();
            List<CCDFieldInfo> fields = entry.getValue();
            writer.println("Class: " + className);
            writer.println("----------------------------------------");
            for (CCDFieldInfo field : fields) {
                outputDomainFieldDetails(field, writer);
            }
        }
        summaryOfFields(writer);
    }

    private void summaryOfFields(PrintWriter writer) {
        writer.println("========================================");
        writer.println("Total Classes: " + ccdFieldsByClass.size());
        writer.println("Total CCD Fields: " + ccdFieldsByClass.values().stream().mapToInt(List::size).sum());
        writer.println("========================================\n");
    }

    private static void outputDomainFieldDetails(CCDFieldInfo field, PrintWriter writer) {
        writer.println("  Field Name: " + field.fieldName);
        writer.println("  Field Type: " + field.fieldType.getSimpleName());
        writer.println("  Label: " + field.annotation.label());
        writer.println("  Searchable: " + field.annotation.searchable());
        writer.println();
    }

    public void printMissingCCDFields(PrintWriter writer, Class<?> entityClass) {
        writer.println("\n========================================");
        writer.println("Missing CCD Fields Report");
        writer.println("Entity: " + entityClass.getSimpleName());
        writer.println("========================================");
        writer.println("These CCD fields are NOT found in the entity graph\n");
        List<MissingCCDFieldInfo> missingFields = findMissingCCDFields(entityClass);
        if (missingFields.isEmpty()) {
            writer.println("All CCD fields are present in the entity graph.");
        } else {
            Map<String, List<MissingCCDFieldInfo>> groupedByClass = new HashMap<>();
            for (MissingCCDFieldInfo missing : missingFields) {
                groupedByClass.computeIfAbsent(missing.className, k -> new ArrayList<>()).add(missing);
            }

            groupedByClass.forEach((className, fields) -> {
                writer.println("Class: " + className);
                writer.println("----------------------------------------");
                fields.forEach(missing -> {
                    CCDFieldInfo field = missing.ccdFieldInfo;
                    writer.println("  Field Name: " + field.fieldName + " (MISSING)");
                    writer.println("  Field Type: " + field.fieldType.getSimpleName());
                    writer.println();
                });
                writer.println();
            });
        }
        summaryOfMissing(writer, missingFields);

    }

    private static void summaryOfMissing(PrintWriter writer, List<MissingCCDFieldInfo> missingFields) {
        writer.println("========================================");
        writer.println("Total Missing CCD Fields: " + missingFields.size());
        writer.println("Classes with Missing Fields: "
            + missingFields.stream().map(m -> m.className).distinct().count());
        writer.println("========================================\n");
    }

    public Map<String, List<CCDFieldInfo>> identifyCCDFields(Class<?> clazz) {
        if (clazz == null || processedClasses.contains(clazz) || ignoredClassesFromMissing.contains(clazz)) {
            return ccdFieldsByClass;
        }
        processedClasses.add(clazz);
        Field[] fields = clazz.getDeclaredFields();
        List<CCDFieldInfo> ccdFields = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(CCD.class)) {
                if (isCompositeType(field.getType())) {
                    identifyCCDFields(field.getType());
                } else {
                    CCD ccdAnnotation = field.getAnnotation(CCD.class);
                    CCDFieldInfo fieldInfo = new CCDFieldInfo(field.getName(), field.getType(), ccdAnnotation);
                    ccdFields.add(fieldInfo);
                }
            }
        }

        if (!ccdFields.isEmpty() && !ignoredClassesFromMissing.contains(clazz)) {
            ccdFieldsByClass.put(clazz.getSimpleName(), ccdFields);
        }
        return ccdFieldsByClass;
    }

    private boolean isCompositeType(Class<?> type) {
        return !type.isPrimitive()
            && !type.isEnum()
            && !type.getName().startsWith("java.")
            && !type.getName().startsWith("javax.")
            && !type.getName().startsWith("uk.gov.hmcts.ccd.sdk.type.");
    }

    record CCDFieldInfo(String fieldName, Class<?> fieldType, CCD annotation) {

    }

    record MissingCCDFieldInfo(String className, CCDFieldInfo ccdFieldInfo) {

    }

}

