package com.android.sapantanted.ambienttemperaturepredictor.helper;

/**
 * Created by seil on 18/5/18.
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
 * thermal_zone26 : battery
 */

public class Constants {
    public static final String BMS = "thermal_zone0";
    public static final String CHG_THERM = "thermal_zone1";
    public static final String XO_THERM = "thermal_zone2";
    public static final String XO_THERM_BUF = "thermal_zone3";
    public static final String MSM_THERM = "thermal_zone4";
    public static final String CHG_TEMP = "thermal_zone5";
    public static final String PM_8953_TZ = "thermal_zone22";
    public static final String PA_THERM0 = "thermal_zone23";
    public static final String FRONT_TEMP = "thermal_zone24";
    public static final String BACK_TEMP = "thermal_zone25";
    public static final String BATTERY = "thermal_zone26";
    public static final String SUBSCRIPTION_TOPIC = "nodemcu/kresit/dht/ambi";
    public static final String PUBLISH_TOPIC = "data/seil/sm_ph_temp/1122";
    public static final String SERVER_URI = "tcp://mqtt.seil.cse.iitb.ac.in:1883";
    public static final int MQTT_QOS = 0;

}
