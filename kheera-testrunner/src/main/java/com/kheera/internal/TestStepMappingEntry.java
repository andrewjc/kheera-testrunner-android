package com.kheera.internal;

import java.lang.reflect.Method;

/**
 * Created by andrewc on 3/07/2016.
 */
public class TestStepMappingEntry {
    public final String expression;
    public final Method methodReference;
    public final Object declaringObject;

    public TestStepMappingEntry(String expression, Object declaringObject, Method methodReference) {
        this.expression = expression;
        this.declaringObject = declaringObject;
        this.methodReference = methodReference;
    }
}
