package com.kheera.internal;

import com.google.gson.annotations.SerializedName;

/**
 * Created by andrewc on 22/11/16.
 */
public class TestRunnerConfig {
    @SerializedName("Name")
    public String Name;

    @SerializedName("Description")
    public String Description;

    @SerializedName("TestData")
    public String TestDataFile;

    @SerializedName("AppConfig")
    public String AppConfigFile;

    @SerializedName("Screenshots")
    public boolean Screenshots;

    @SerializedName("IncludeSkipped")
    public boolean IncludeSkipped;

    @SerializedName("WriteTestReport")
    public boolean WriteTestReport;

    @SerializedName("ReportLogPath")
    public String ReportLogPath;

    @SerializedName("FilterBy")
    public String FilterBy;

    @SerializedName("FilterType")
    public String FilterType;

    @SerializedName("DetailedErrors")
    public boolean DetailedErrors;

    @SerializedName("CoverageReport")
    public boolean CoverageReport;

    @SerializedName("CoverageFile")
    public String CoverageFile;

    @SerializedName("LogTag")
    public String LogTag;
}
