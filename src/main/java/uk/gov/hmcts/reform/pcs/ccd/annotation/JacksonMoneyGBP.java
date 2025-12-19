package uk.gov.hmcts.reform.pcs.ccd.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.pcs.ccd.jackson.MoneyGBPDeserializer;
import uk.gov.hmcts.reform.pcs.ccd.jackson.MoneyGBPSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A convenience annotation that combines the CCD MoneyGBP JSON serialiser/deserialiser annotations.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@JsonSerialize(using = MoneyGBPSerializer.class)
@JsonDeserialize(using = MoneyGBPDeserializer.class)
@JacksonAnnotationsInside
public @interface JacksonMoneyGBP {
}
