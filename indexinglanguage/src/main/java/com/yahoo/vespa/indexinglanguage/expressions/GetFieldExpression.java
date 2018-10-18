// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.indexinglanguage.expressions;

import com.yahoo.document.DataType;
import com.yahoo.document.Field;
import com.yahoo.document.StructuredDataType;
import com.yahoo.document.datatypes.FieldValue;
import com.yahoo.document.datatypes.StructuredFieldValue;

/**
 * @author Simon Thoresen Hult
 */
public final class GetFieldExpression extends Expression {

    private final String fieldName;

    public GetFieldExpression(String fieldName) {
        super(UnresolvedDataType.INSTANCE);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    protected void doExecute(ExecutionContext ctx) {
        FieldValue input = ctx.getValue();
        if (!(input instanceof StructuredFieldValue)) {
            throw new IllegalArgumentException("Expected structured input, got " + input.getDataType().getName() + ".");
        }
        StructuredFieldValue struct = (StructuredFieldValue)input;
        Field field = struct.getField(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found in struct type '" +
                                               struct.getDataType().getName() + "'");
        }
        ctx.setValue(struct.getFieldValue(field));
    }

    @Override
    protected void doVerify(VerificationContext context) {
        DataType input = context.getValue();
        if (!(input instanceof StructuredDataType)) {
            throw new VerificationException(this, "Expected structured input, got " + input.getName() + ".");
        }
        Field field = ((StructuredDataType)input).getField(fieldName);
        if (field == null) {
            throw new VerificationException(this, "Field '" + fieldName + "' not found in struct type '" +
                                                  input.getName() + "'");
        }
        context.setValue(field.getDataType());
    }

    @Override
    public DataType createdOutputType() {
        return UnresolvedDataType.INSTANCE;
    }

    @Override
    public String toString() {
        return "get_field " + fieldName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GetFieldExpression)) {
            return false;
        }
        GetFieldExpression rhs = (GetFieldExpression)obj;
        if (!fieldName.equals(rhs.fieldName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() + fieldName.hashCode();
    }
}
