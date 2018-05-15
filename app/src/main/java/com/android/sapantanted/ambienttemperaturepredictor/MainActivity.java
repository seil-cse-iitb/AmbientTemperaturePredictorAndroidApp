package com.android.sapantanted.ambienttemperaturepredictor;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {
    static final String subscriptionTopic = "nodemcu/kresit/dht/SEIL";
    static final String publishTopic = "data/seil/sm_ph_temp/1122";
    static final String serverUri = "tcp://mqtt.seil.cse.iitb.ac.in:1883";
    static final int mqttQOS = 1;

    private TextView tvAmbientTemperature, tvActualTemperature;
    private EditText etTemperatureSensorID;
    private MqttAndroidClient mqttAndroidClient;
    private String macAdd;
    private String clientId = "AmbientTemperaturePredictor";
    private double batteryTemperature = -1;
    private Handler mHandler;
    private Button btnStartStopService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            macAdd = getMacAddr();
        } else {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            macAdd = info.getMacAddress();
        }
        tvAmbientTemperature = findViewById(R.id.tvAmbientTemperature);
        tvActualTemperature = findViewById(R.id.tvActualTemperature);
        etTemperatureSensorID = findViewById(R.id.etTemperatureSensorID);
        etTemperatureSensorID.setText(getSharedPreferences("sp", MODE_PRIVATE).getString("temperature_sensor_id", ""));
        btnStartStopService = findViewById(R.id.btnStartStopService);

        if (isMyServiceRunning(SensingService.class)) {
            btnStartStopService.setBackgroundColor(Color.parseColor("#ff5252"));//Red
            btnStartStopService.setText("Stop Data Collection");
        } else {
            btnStartStopService.setBackgroundColor(Color.parseColor("#66bb6a"));//Green
            btnStartStopService.setText("Start Data Collection");
        }
        //registering broadcastReceiver for battery temperature changes
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(batteryTemperatureBroadcastReceiver, intentfilter);

        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    makeToast("Reconnected to : " + serverURI);
                    subscribeToTopic();
                } else {
                    makeToast("Connected to: " + serverURI);
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
            makeToast("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions,
                    null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            makeToast("Connected to: " + serverUri);
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

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                tvActualTemperature.setText("Actual Temperature Sensor: " + message.getData().getDouble("reading") + " " + (char) 0x00B0 + "C");
            }
        };

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
                    makeToast("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("Failed to subscribe");
                }
            });
            mqttAndroidClient.subscribe(subscriptionTopic, mqttQOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String sensorReading = new String(message.getPayload());
                    String id = sensorReading.split(",")[0];
                    if (!id.equalsIgnoreCase(etTemperatureSensorID.getText().toString())) return;
                    final double actualTemperatureReading = Double.parseDouble(sensorReading.split(",")[1]);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("reading", actualTemperatureReading);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private void makeToast(String s) {
        Toast.makeText(MainActivity.this, "MainActivity: " + s, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver batteryTemperatureBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double batteryTemperature = (double) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            MainActivity.this.batteryTemperature = batteryTemperature;
            tvAmbientTemperature.setText("Battery Temperature: " + MainActivity.this.batteryTemperature + " " + (char) 0x00B0 + "C");
        }
    };

    public void setTemperatureSensorId(View view) {
        SharedPreferences sp = this.getSharedPreferences("sp", MODE_PRIVATE);
        sp.edit().putString("temperature_sensor_id", etTemperatureSensorID.getText().toString()).commit();
        Toast.makeText(this, "Sensor id updated successfull!", Toast.LENGTH_SHORT).show();
    }

    public void startStopService(View view) {
        Intent intent = new Intent(getApplicationContext(), SensingService.class);
        if (isMyServiceRunning(SensingService.class)) {
            stopService(intent);
            btnStartStopService.setBackgroundColor(Color.parseColor("#66bb6a"));//Green
            btnStartStopService.setText("Start Data Collection");
        } else {
            startService(intent);
            btnStartStopService.setBackgroundColor(Color.parseColor("#ff5252"));//Red
            btnStartStopService.setText("Stop Data Collection");
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

