package com.android.sapantanted.ambienttemperaturepredictor.helper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.android.sapantanted.ambienttemperaturepredictor.SensingService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by seil on 16/5/18.
 */

public class Functions {

    public static BatteryManager getBatteryManager(Context context) {
        BatteryManager batteryManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            batteryManager = context.getApplicationContext().getSystemService(BatteryManager.class);
        } else {
            batteryManager = (BatteryManager) context.getApplicationContext().getSystemService(BATTERY_SERVICE);
        }
        return batteryManager;
    }

    public static boolean isCharging(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getBatteryManager(context.getApplicationContext()).isCharging();
        } else {
            IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryIntent = context.getApplicationContext().registerReceiver(null, batteryIntentFilter);
            int intExtra = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if (intExtra == 0) return false;
            else return true;
        }
    }

    public static boolean isTurboCharging(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            double averageCurrent = getBatteryAverageCurrent(context);
            makeToast(context,"AverageCurrent: "+averageCurrent);
            if (averageCurrent > 1500000) return true;
            else return false;
        } else {
            return false;
        }
        //TODO Current might not be correct parameter to check for turbo charging. Charging Voltage can be a significant parameter
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static double getBatteryAverageCurrent(Context context) {
        return getBatteryManager(context.getApplicationContext()).getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
    }

    public static double getBatteryVoltage(Context context) {
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, batteryIntentFilter);
        double batteryVoltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        if (batteryVoltage > 1000)
            batteryVoltage /= 1000f;
        return batteryVoltage;
    }

    public static float getTemperature(String zone) {
        Process p;
        try {
            p = Runtime.getRuntime().exec("cat sys/class/thermal/" + zone + "/temp");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            float temp = Float.parseFloat(line);
            if (temp > 100)
                temp /= 1000.0f;
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    public static String getMacAddr(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }
                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(Integer.toHexString(b & 0xFF) + ":");
                    }
                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            return info.getMacAddress();
        }
        return "";
    }

    public static String getStringFromSP(Context context, String key) {
        return context.getSharedPreferences("sp", MODE_PRIVATE).getString(key, "");
    }

    public static boolean putStringToSP(Context context, String key, String value) {
        return context.getSharedPreferences("sp", MODE_PRIVATE).edit().putString(key, value).commit();
    }


    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static int getBatteryPercentage(Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.getApplicationContext().registerReceiver(null, iFilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        float batteryPct = level / (float) scale;
        return (int) (batteryPct * 100);
    }

    public static double getBatteryTemperature(Context context) {
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null, batteryIntentFilter);
        double batteryTemperature = (double) batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
        return batteryTemperature;
    }

    public static double getRamUsagePercentage(Context context) {
        long freeSize, totalSize, usedSize;
        double percent = -1;
        try {
            freeSize = freeRamMemorySize(context);
            totalSize = totalRamMemorySize(context);
            usedSize = totalSize - freeSize;
            percent = (usedSize * 100 / (double) totalSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return percent;
    }

    private static long freeRamMemorySize(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;

        return availableMegs;
    }

    private static long totalRamMemorySize(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.totalMem / 1048576L;
        return availableMegs;
    }

    public static String roundToDecimalPlaces(double value, int decimalPlaces) {
        String formatString = "#";
        if (decimalPlaces > 0) formatString += ".";
        for (int i = 0; i < decimalPlaces; i++) {
            formatString += "#";
        }
        DecimalFormat decimalFormat = new DecimalFormat(formatString);
        return decimalFormat.format(value);
    }

    public static void log(Context context, String text) {
        File folder = context.getExternalFilesDir("AmbientTemperaturePredictionApp");
        System.out.println(text);
        File log = new File(folder, "log");
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(log);

            try {
                stream.write(text.getBytes());
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logError(Context context, String error) {
        log(context, "[" + new Date() + "][Exception]" + error);
    }

    public static void logInfo(Context context, String info) {
        log(context, "[" + new Date() + "][Info]" + info);
    }

    private static void makeToast(Context context, String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }

}
