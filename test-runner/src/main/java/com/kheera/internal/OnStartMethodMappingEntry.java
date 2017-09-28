package com.kheera.internal;

import java.lang.reflect.Method;

/**
 * Created by andrewc on 3/07/2016.
 */
public class OnStartMethodMappingEntry {
    public final Method methodReference;
    public final Object declaringObject;
    public final String condition;

    public OnStartMethodMappingEntry(Object declaringObject, String condition, Method methodReference) {
        this.declaringObject = declaringObject;
        this.methodReference = methodReference;
        this.condition = condition;
    }
}
