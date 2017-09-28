package com.kheera.internal;

import java.util.ArrayList;

/**
 * Created by andrewc on 15/08/2016.
 */
public class FeatureResult {
    public static final String PASS = "pass";
    public static final String FAIL = "fail";
    public static final String FAIL_FIRST = "failFirst";

    public String status;
    public String filename;

    public ArrayList<String> passingTests;
    public ArrayList<String> skippedTests;
    public ArrayList<String> failingTests;
}
