package com.kheera.internal;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.kheera.annotations.TestModule;
import com.kheera.annotations.RootTestModule;
import com.kheera.annotations.OnFinishTest;
import com.kheera.annotations.OnStartTest;
import com.kheera.annotations.TestStep;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import dalvik.system.DexFile;
import gherkin.pickles.Pickle;

/**
 * Created by andrewc on 4/07/2016.
 */
public class StepDefFunctionTable {

    private final HashSet<OnStartMethodMappingEntry> declaredBeforeMethods;
    private final HashMap<String, TestStepMappingEntry> declaredTestStepMethods;
    private final HashSet<OnFinishMethodMappingEntry> declaredFinishMethods;
    private final HashMap<String, TestStepMappingEntry> expressionCache;
    private final Context context;
    private final Class<? extends StepDefinition> stepDefClass;
    private final StepDefinition stepDefObject;

    //private Class gerkinRootClass;
    //private Object gerkinRootObject;

    private boolean enableExpressionCache = true;

    public StepDefFunctionTable(Context context, StepDefinition stepDef) throws AutomationRunnerException{
        this.context = context;
        this.stepDefClass = stepDef.getClass();
        this.stepDefObject = stepDef;
        this.declaredBeforeMethods = new HashSet<OnStartMethodMappingEntry>();
        this.declaredTestStepMethods = new HashMap<String, TestStepMappingEntry>();
        this.declaredFinishMethods = new HashSet<OnFinishMethodMappingEntry>();
        this.expressionCache = new HashMap<String, TestStepMappingEntry>();

        if(stepDefClass != null) {
            indexStepDefClass();
        }
    }

    private void indexStepDefClass() throws AutomationRunnerException {
        indexStepsOnModule(stepDefObject);
    }

    public Set<OnFinishMethodMappingEntry> findOnFinishMethod(Pickle pickle) {
        Benchmarker.start("findOnFinishMethod");
        HashSet<OnFinishMethodMappingEntry> method = new HashSet<OnFinishMethodMappingEntry>();
        String featureName = pickle.getName();
        for(OnFinishMethodMappingEntry m : declaredFinishMethods)
        {
            // evaluate conditions
            if(m.condition != null) {
            }

            method.add(m);
        }
        Benchmarker.stop();
        return method;
    }

    public Set<OnStartMethodMappingEntry> findSetUpMethods(Pickle pickle) {
        Benchmarker.start("findSetUpMethod");
        HashSet<OnStartMethodMappingEntry> method = new HashSet<OnStartMethodMappingEntry>();
        String featureName = pickle.getName();
        for(OnStartMethodMappingEntry m : declaredBeforeMethods)
        {
            // evaluate conditions
            if(m.condition != null) {
            }

            method.add(m);
        }
        Benchmarker.stop();
        return method;
    }

    public TestStepMappingEntry findMatchingMethod(String expression) {
        Benchmarker.start("findMatchingMethod: " + expression);
        // Check the expression cache
        if(enableExpressionCache && expressionCache.containsKey(expression)) {
            Benchmarker.stop();
            return expressionCache.get(expression);
        }
        // Find a matching method in the method map for the given expression
        for(TestStepMappingEntry entry : declaredTestStepMethods.values()) {
            // Run the regexp over the entry in the map.
            // If it matches, then we cache the result, and return it.
            if(expression.matches(entry.expression)) {
                if(enableExpressionCache && expressionCache.containsKey(expression)==false) {
                    expressionCache.put(expression, entry);
                }
                Benchmarker.stop();
                return entry;
            }
        }
        Benchmarker.stop();
        return null;
    }

    private void indexStepsOnModule(Object declaringObject) throws AutomationRunnerException {
        Benchmarker.start("indexStepsOnModule");
        Set<Method> annotatedTestSteps = getMethodsAnnotatedWith(declaringObject, TestStep.class);

        // Find all annotated methods with teststep
        for(Method m : annotatedTestSteps) {
            String exp = m.getAnnotation(TestStep.class).value();
            Method methodRef = m;
            TestStepMappingEntry testMethodMap = new TestStepMappingEntry(exp, declaringObject, methodRef);
            declaredTestStepMethods.put(exp, testMethodMap);
        }

        // Find and index any start test methods
        Set<Method> beforeClasses = getMethodsAnnotatedWith(declaringObject, OnStartTest.class);
        for(Method m : beforeClasses) {
            m.setAccessible(true);
            Method methodRef = m;
            String conditions = "";
            if(m.getAnnotation(OnStartTest.class).condition() != null)
                conditions = m.getAnnotation(OnStartTest.class).condition();

            OnStartMethodMappingEntry entry = new OnStartMethodMappingEntry(declaringObject, conditions, methodRef);
            declaredBeforeMethods.add(entry);
        }

        // Find and index any finish methods
        Set<Method> finishClasses = getMethodsAnnotatedWith(declaringObject, OnFinishTest.class);
        for(Method m : finishClasses) {
            m.setAccessible(true);
            Method methodRef = m;
            String conditions = "";
            if(m.getAnnotation(OnFinishTest.class).condition() != null)
                conditions = m.getAnnotation(OnFinishTest.class).condition();

            OnFinishMethodMappingEntry entry = new OnFinishMethodMappingEntry(declaringObject, conditions, methodRef);
            declaredFinishMethods.add(entry);
        }


        Field[] fields = declaringObject.getClass().getFields();
        for(Field field : fields) {
            try {
                if(field.get(declaringObject) != null) {
                    Class fieldClass = field.get(declaringObject).getClass();
                    if (fieldClass.isAnnotationPresent(TestModule.class)) {
                        indexStepsOnModule(field.get(declaringObject));
                    }
                }
            }
            catch(Exception e) {
                if(field.getName() != null)
                    throw new AutomationRunnerException("Failed indexing gerkin module: " + field.getName(), e);
                else
                    throw new AutomationRunnerException("Failed indexing gerkin module.", null);
            }
        }

        Benchmarker.stop();
    }

    private Set<Method> getMethodsAnnotatedWith(Object cl, Class<? extends Annotation> annotation) {
        Benchmarker.start("getMethodsAnnotatedWith");
        Set<Method> retSet = new LinkedHashSet<>();
        Method[] methods = cl.getClass().getMethods();
        for(Method m : methods) {
            if(m.isAnnotationPresent(annotation))
                retSet.add(m);
        }
        Benchmarker.stop();
        return retSet;
    }

    private Class findGerkinRoot() throws AutomationRunnerException {
        Benchmarker.start("findGerkinRoot");

        Class c = this.getClass();
        DexFile df = null;
        try {
            df = new DexFile(context.getPackageCodePath());
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
                try {
                    String s = iter.nextElement();
                    String appPackageName = InstrumentationRegistry.getTargetContext().getPackageName();

                    if(s.startsWith("android.")) continue;
                    if (s.startsWith(appPackageName)) {
                        Class cl = df.loadClass(s, c.getClassLoader());
                        if (cl.isAnnotationPresent(RootTestModule.class)) {
                            Benchmarker.stop();
                            return cl;
                        }
                    }
                }
                catch(Exception e) {}
            }
        } catch (IOException e) {
            throw new AutomationRunnerException("Error locating gerkin root.", e);
        }
        finally {
            if(df != null)
                try {
                    df.close();
                } catch (IOException e) {

                }
        }
        Benchmarker.stop();
        return null;
    }

}
