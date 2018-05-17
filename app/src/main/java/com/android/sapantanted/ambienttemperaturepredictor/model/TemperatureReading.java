package com.android.sapantanted.ambienttemperaturepredictor.model;

/**
 * Created by seil on 11/5/18.
 */

/**
 * thermal_zone0 : bms
 * thermal_zone1 : chg_therm
 * thermal_zone2 : xo_therm
 * thermal_zone3 : xo_therm_buf
 * thermal_zone4 : msm_therm
 * thermal_zone5 : chg_temp
 * thermal_zone22 : pm8953_tz
 * thermal_zone23 : pa_therm0
 * thermal_zone24 : front_temp
 * thermal_zone25 : back_temp
 * thermal_zone26 : batteryTemp
 */


public class TemperatureReading {

    String macID;
    double smphTs, batteryTemperature, cpuTemperature, ramUsagePercent;
    String adtSensorId;
    double actualTemperature;
    double bms, chgTherm, xoTherm, xoThermBuf, msmTherm, chgTemp, pm8953Tz, paTherm0, frontTemp, backTemp, batteryTemp;
    double batteryPercentage, batteryVoltage;
    boolean isCharging, isTurboCharging;

    public TemperatureReading(String macID, double smphTs, double batteryTemperature, double cpuTemperature, double ramUsagePercent, String adtSensorId, double actualTemperature) {
        this.macID = macID;
        if (smphTs != -1)
            this.smphTs = smphTs;
        else
            this.smphTs = (double) System.currentTimeMillis() / 1000;
        this.batteryTemperature = batteryTemperature;
        this.cpuTemperature = cpuTemperature;
        this.ramUsagePercent = ramUsagePercent;
        this.adtSensorId = adtSensorId;
        this.actualTemperature = actualTemperature;
    }

    public TemperatureReading(String macID, double smphTs, double batteryTemperature, double cpuTemperature, double ramUsagePercent, String adtSensorId, double actualTemperature, double bms, double chgTherm, double xoTherm, double xoThermBuf, double msmTherm, double chgTemp, double pm8953Tz, double paTherm0, double frontTemp, double backTemp, double batteryTemp, double batteryPercentage, double batteryVoltage, boolean isCharging, boolean isTurboCharging) {
        this.macID = macID;
        if (smphTs != -1)
            this.smphTs = smphTs;
        else
            this.smphTs = (double) System.currentTimeMillis() / 1000;
        this.batteryTemperature = batteryTemperature;
        this.cpuTemperature = cpuTemperature;
        this.ramUsagePercent = ramUsagePercent;
        this.adtSensorId = adtSensorId;
        this.actualTemperature = actualTemperature;
        this.bms = bms;
        this.chgTherm = chgTherm;
        this.xoTherm = xoTherm;
        this.xoThermBuf = xoThermBuf;
        this.msmTherm = msmTherm;
        this.chgTemp = chgTemp;
        this.pm8953Tz = pm8953Tz;
        this.paTherm0 = paTherm0;
        this.frontTemp = frontTemp;
        this.backTemp = backTemp;
        this.batteryTemp = batteryTemp;
        this.batteryPercentage = batteryPercentage;
        this.batteryVoltage = batteryVoltage;
        this.isCharging = isCharging;
        this.isTurboCharging = isTurboCharging;
    }

    public TemperatureReading() {
    }

    public String getMacID() {
        return macID;
    }

    public void setMacID(String macID) {
        this.macID = macID;
    }

    public double getSmphTs() {
        return smphTs;
    }

    public void setSmphTs(double smphTs) {
        this.smphTs = smphTs;
    }

    public double getBatteryTemperature() {
        return batteryTemperature;
    }

    public void setBatteryTemperature(double batteryTemperature) {
        this.batteryTemperature = batteryTemperature;
    }

    public double getCpuTemperature() {
        return cpuTemperature;
    }

    public void setCpuTemperature(double cpuTemperature) {
        this.cpuTemperature = cpuTemperature;
    }

    public double getRamUsagePercent() {
        return ramUsagePercent;
    }

    public void setRamUsagePercent(double ramUsagePercent) {
        this.ramUsagePercent = ramUsagePercent;
    }

    public String getAdtSensorId() {
        return adtSensorId;
    }

    public void setAdtSensorId(String adtSensorId) {
        this.adtSensorId = adtSensorId;
    }

    public double getActualTemperature() {
        return actualTemperature;
    }

    public void setActualTemperature(double actualTemperature) {
        this.actualTemperature = actualTemperature;
    }

    @Override
    public String toString() {
        return "TemperatureReading{" +
                "macID='" + macID + '\'' +
                ", smphTs=" + smphTs +
                ", batteryTemperature=" + batteryTemperature +
                ", cpuTemperature=" + cpuTemperature +
                ", ramUsagePercent=" + ramUsagePercent +
                ", adtSensorId=" + adtSensorId +
                ", actualTemperature=" + actualTemperature +
                ", bms=" + bms +
                ", chgTherm=" + chgTherm +
                ", xoTherm=" + xoTherm +
                ", xoThermBuf=" + xoThermBuf +
                ", msmTherm=" + msmTherm +
                ", chgTemp=" + chgTemp +
                ", pm8953Tz=" + pm8953Tz +
                ", paTherm0=" + paTherm0 +
                ", frontTemp=" + frontTemp +
                ", backTemp=" + backTemp +
                ", batteryTemp=" + batteryTemp +
                ", batteryPercentage=" + batteryPercentage +
                ", batteryVoltage=" + batteryVoltage +
                ", isCharging=" + isCharging +
                ", isTurboCharging=" + isTurboCharging +
                '}';
    }

    public String toMQTTMessage() {
        return macID + "," + smphTs + "," + batteryTemperature + "," + cpuTemperature + "," +
                ramUsagePercent + "," + adtSensorId + "," + actualTemperature + "," +
                bms + "," + chgTherm + "," + xoTherm + "," + xoThermBuf + "," +
                msmTherm + "," + chgTemp + "," + pm8953Tz + "," + paTherm0 + "," +
                frontTemp + "," + backTemp + "," + batteryTemp + "," + batteryPercentage + "," +
                batteryVoltage + "," + (isCharging?1:0) + "," + (isTurboCharging?1:0);
    }
}
