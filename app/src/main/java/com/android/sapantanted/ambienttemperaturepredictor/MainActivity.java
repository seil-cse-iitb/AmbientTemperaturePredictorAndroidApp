package com.android.sapantanted.ambienttemperaturepredictor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.sapantanted.ambienttemperaturepredictor.helper.Constants;
import com.android.sapantanted.ambienttemperaturepredictor.helper.Functions;
import com.android.sapantanted.ambienttemperaturepredictor.model.TemperatureReading;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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

import java.text.DecimalFormat;

import static com.android.sapantanted.ambienttemperaturepredictor.helper.Functions.*;
import static com.android.sapantanted.ambienttemperaturepredictor.helper.Constants.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvAmbientTemperature, tvActualTemperature, tvMacId, tvRamUsage, tvMQTTMessage;
    private EditText etTemperatureSensorID;
    private MqttAndroidClient mqttAndroidClient;
    private String clientId = "AmbientTemperaturePredictor";
    private Handler mHandler;
    private CheckBox cbDeliveryStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvAmbientTemperature = findViewById(R.id.tvAmbientTemperature);
        tvActualTemperature = findViewById(R.id.tvActualTemperature);
        tvMacId = findViewById(R.id.tvMacId);
        tvRamUsage = findViewById(R.id.tvRamUsage);
        tvMQTTMessage = findViewById(R.id.tvMQTTMessage);
        cbDeliveryStatus = findViewById(R.id.cbDeliveryStatus);
        etTemperatureSensorID = findViewById(R.id.etTemperatureSensorID);
        etTemperatureSensorID.setText(getStringFromSP(this, "temperature_sensor_id"));

        tvMacId.setText("MAC ID: " + getMacAddr(getApplicationContext()));
        tvRamUsage.setText("Ram Usage: " + roundToDecimalPlaces(getRamUsagePercentage(MainActivity.this), 2) + " %");
        if (getStringFromSP(this, "delivery_status") == null) {
            cbDeliveryStatus.setChecked(false);
        } else {
            if (getStringFromSP(this, "delivery_status").equalsIgnoreCase("true"))
                cbDeliveryStatus.setChecked(true);
            else
                cbDeliveryStatus.setChecked(false);
        }
        cbDeliveryStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    putStringToSP(MainActivity.this, "delivery_status", "true");
                else
                    putStringToSP(MainActivity.this, "delivery_status", "false");
            }
        });
        //registering broadcastReceiver for battery temperature changes
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(batteryTemperatureBroadcastReceiver, intentfilter);

        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(this, SERVER_URI, clientId);
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
//            makeToast("Connecting to " + SERVER_URI);
            mqttAndroidClient.connect(mqttConnectOptions,
                    null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
//                            makeToast("Connected to: " + SERVER_URI);
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
                            makeToast("Failed to connect to: " + SERVER_URI);
                            exception.printStackTrace();
                        }
                    });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                double actualTemperatureReading = message.getData().getDouble("reading");
                tvActualTemperature.setText("Actual Temperature Sensor: " + actualTemperatureReading + " " + (char) 0x00B0 + "C");
                TemperatureReading tr = new TemperatureReading("mac_id", -1,
                        getBatteryTemperature(MainActivity.this), 0.0,
                        getRamUsagePercentage(MainActivity.this), "sensor_id", actualTemperatureReading,
                        getTemperature(Constants.BMS),
                        getTemperature(Constants.CHG_THERM),
                        getTemperature(Constants.XO_THERM),
                        getTemperature(Constants.XO_THERM_BUF),
                        getTemperature(Constants.MSM_THERM),
                        getTemperature(Constants.CHG_TEMP),
                        getTemperature(Constants.PM_8953_TZ),
                        getTemperature(Constants.PA_THERM0),
                        getTemperature(Constants.FRONT_TEMP),
                        getTemperature(Constants.BACK_TEMP),
                        getTemperature(Constants.BATTERY),
                        getBatteryPercentage(MainActivity.this),
                        getBatteryVoltage(MainActivity.this),
                        isCharging(MainActivity.this),
                        isTurboCharging(MainActivity.this)
                );
                tvMQTTMessage.setText(tr.toMQTTMessage());
            }
        };

    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, MQTT_QOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    makeToast("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("Failed to subscribe");
                }
            });
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, MQTT_QOS, new IMqttMessageListener() {
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
            tvAmbientTemperature.setText("Battery Temperature: " + batteryTemperature + " " + (char) 0x00B0 + "C");
            tvRamUsage.setText("Ram Usage: " + roundToDecimalPlaces(getRamUsagePercentage(MainActivity.this), 2) + " %");

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mqttAndroidClient.unsubscribe(SUBSCRIPTION_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient.unregisterResources();
        this.unregisterReceiver(batteryTemperatureBroadcastReceiver);
    }

    public void setTemperatureSensorId(View view) {
        putStringToSP(this, "temperature_sensor_id", etTemperatureSensorID.getText().toString());
        Toast.makeText(this, "Sensor id updated successfull!", Toast.LENGTH_SHORT).show();
    }

    public void startService(View view) {
        Intent intent = new Intent(MainActivity.this, SensingService.class);
        startService(intent);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent);
//        }
    }

    public void stopService(View view) {
        Intent intent = new Intent(MainActivity.this, SensingService.class);
        stopService(intent);
    }

    public void predictTemperature(View view) {
        final double batteryTemperature = getBatteryTemperature(MainActivity.this);
        double ramUsagePercentage = getRamUsagePercentage(MainActivity.this);
        int batteryPercentage = getBatteryPercentage(MainActivity.this);
        double batteryVoltage = getBatteryVoltage(MainActivity.this);
        boolean isCharging = isCharging(MainActivity.this);
        makeToast(isCharging+"");
        String macAddr = getMacAddr(MainActivity.this);
        String url="http://10.129.149.32:5000/atmos/prediction";
        url+="/"+batteryTemperature;
        url+="/"+ramUsagePercentage;
        url+="/"+batteryPercentage;
        url+="/"+batteryVoltage;
        url+="/"+(isCharging?1:0);
        url+="/"+macAddr;
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest sr = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String predictedTemperature =response;
                Toast.makeText(MainActivity.this,"Battery Temperature: "+batteryTemperature+"\nPredicted Temperature: "+predictedTemperature,Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast("Error!!"+error.getMessage());
            }
        });
        requestQueue.add(sr);
        requestQueue.start();
    }
}

