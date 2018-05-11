package com.android.sapantanted.ambienttemperaturepredictor.model;

/**
 * Created by seil on 11/5/18.
 */

public class TemperatureReading {

    String macID;
    double smphTs, batteryTemperature, cpuTemperature, ramUsagePercent;
    int adtSensorId;
    double actualTemperature;

    public TemperatureReading(String macID, double smphTs, double batteryTemperature, double cpuTemperature, double ramUsagePercent, int adtSensorId, double actualTemperature) {
        this.macID = macID;
        if (smphTs != -1)
            this.smphTs = smphTs;
        else
            this.smphTs = (double)System.currentTimeMillis()/1000;
        this.batteryTemperature = batteryTemperature;
        this.cpuTemperature = cpuTemperature;
        this.ramUsagePercent = ramUsagePercent;
        this.adtSensorId = adtSensorId;
        this.actualTemperature = actualTemperature;
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

    public int getAdtSensorId() {
        return adtSensorId;
    }

    public void setAdtSensorId(int adtSensorId) {
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
                '}';
    }

    public String toMQTTMessage() {
        return macID + "," + smphTs + "," + batteryTemperature + "," + cpuTemperature + "," + ramUsagePercent + "," + adtSensorId + "," + actualTemperature;
    }
}
