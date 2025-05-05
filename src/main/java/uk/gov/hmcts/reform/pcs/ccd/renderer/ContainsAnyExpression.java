package uk.gov.hmcts.reform.pcs.ccd.renderer;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.node.expression.BinaryExpression;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplateImpl;

import java.util.Collection;

public class ContainsAnyExpression extends BinaryExpression<Boolean> {

    @Override
    public Boolean evaluate(PebbleTemplateImpl self, EvaluationContextImpl context) {

        Object leftValue = getLeftExpression().evaluate(self, context);
        Object rightValue = getRightExpression().evaluate(self, context);

        // TODO: Tests
        if (leftValue == null || rightValue == null) {
            return false;
        }

        if (!(leftValue instanceof Collection<?> leftCollection)) {
            throw new PebbleException(null,
                                      "ContainsAny operator can currently only be used on Collections. Actual type was: "
                                          + leftValue.getClass().getName(),
                                      this.getLineNumber(),
                                      self.getName()
            );
        }

        if (rightValue instanceof Collection<?> collection) {
            return collection.stream().anyMatch(leftCollection::contains);
        } else {
            return leftCollection.contains(rightValue);
        }
    }

}
