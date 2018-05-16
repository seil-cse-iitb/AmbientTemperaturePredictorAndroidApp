package com.android.sapantanted.ambienttemperaturepredictor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sapantanted.ambienttemperaturepredictor.model.TemperatureReading;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SensingService extends Service {
    static final String subscriptionTopic = "nodemcu/kresit/dht/SEIL";
    static final String publishTopic = "data/seil/sm_ph_temp/1122";
    static final String serverUri = "tcp://mqtt.seil.cse.iitb.ac.in:1883";
    static final int mqttQOS = 1;

    private MqttAndroidClient mqttAndroidClient;
    private String macAdd;
    private String clientId = "AmbientTemperaturePredictor";
    private double batteryTemperature = -1;
    private PowerManager.WakeLock wl;

    public SensingService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        makeToast("onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        makeToast("onCreate()");
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(
                getApplicationContext().POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            macAdd = getMacAddr();
        } else {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            macAdd = info.getMacAddress();
        }

        //registering broadcastReceiver for battery temperature changes
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(batteryTemperatureBroadcastReceiver, intentfilter);

        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(this, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    makeToast("Reconnected to : " + serverURI);
//                    subscribeToTopic();
                } else {
//                    makeToast("Connected to: " + serverURI);
                    makeToast("MQTT Connected!");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                makeToast("The Connection was lost." + cause.getMessage());
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                makeToast("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                makeToast("Message delivered! ");
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        try {
//            makeToast("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions,
                    null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
//                            makeToast("Connected to: " + serverUri);
                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                            disconnectedBufferOptions.setBufferEnabled(true);
                            disconnectedBufferOptions.setBufferSize(100);
                            disconnectedBufferOptions.setPersistBuffer(false);
                            disconnectedBufferOptions.setDeleteOldestMessages(false);
                            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                            subscribeToTopic();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            makeToast("Failed to connect to: " + serverUri);
                            exception.printStackTrace();
                        }
                    });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }

    public static String getMacAddr() {
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
            //handle exception
        }
        return "";
    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, mqttQOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
//                    makeToast("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("Failed to subscribe");
                }
            });
            mqttAndroidClient.subscribe(subscriptionTopic, mqttQOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String temperatureSensorId = SensingService.this.getSharedPreferences("sp", MODE_PRIVATE).getString("temperature_sensor_id", "");
                    String sensorReading = new String(message.getPayload());
                    String id = sensorReading.split(",")[0];
                    if (!id.equalsIgnoreCase(temperatureSensorId)) return;
                    final double actualTemperatureReading = Double.parseDouble(sensorReading.split(",")[1]);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("reading", actualTemperatureReading);
                    msg.setData(bundle);
                    TemperatureReading tr = new TemperatureReading(macAdd, -1, batteryTemperature, 0.0, 0.0, Integer.parseInt(id), actualTemperatureReading);
                    if (mqttAndroidClient.isConnected())
                        publishMessage(tr.toMQTTMessage());
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(String publishMessage) {

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            if (!mqttAndroidClient.isConnected()) {
                System.out.println("MQTT not connected!!");
//                makeToast(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void makeToast(String s) {
        Toast.makeText(SensingService.this, "SensingService: " + s, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver batteryTemperatureBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double batteryTemperature = (double) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            SensingService.this.batteryTemperature = batteryTemperature;

        }
    };

    @Override
    public void onDestroy() {
        makeToast("Destroy() called!");
        try {
            mqttAndroidClient.unsubscribe(subscriptionTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient.unregisterResources();
        this.unregisterReceiver(batteryTemperatureBroadcastReceiver);
        wl.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
