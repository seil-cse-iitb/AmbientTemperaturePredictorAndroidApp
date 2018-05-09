package com.android.sapantanted.ambienttemperaturepredictor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {
    private TextView tvAmbientTemperature;
    private final static String MQTT_TOPIC = "/data/seil/sm_ph_temp/1122";
    MqttAndroidClient client;
    MqttAndroidClient mqttAndroidClient;
    String clientId = "AmbientTemperaturePredictor";
    final String serverUri = "tcp://mqtt.seil.cse.iitb.ac.in:1883";
    final String subscriptionTopic = "/data/seil/sm_ph_temp/1122";
    final String publishTopic = "/data/seil/sm_ph_temp/1122";

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    makeToast("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
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
            makeToast("Message Published");
            if (!mqttAndroidClient.isConnected()) {
                makeToast(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void makeToast(String s) {
        Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() {
        this.unregisterReceiver(broadcastreceiver);
        super.onStop();
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvAmbientTemperature = findViewById(R.id.tvAmbientTemperature);
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        MainActivity.this.registerReceiver(broadcastreceiver, intentfilter);
        clientId = clientId + System.currentTimeMillis();

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    makeToast("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    makeToast("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                makeToast("The Connection was lost.");
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
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
//        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://mqtt.seil.cse.iitb.ac.in:1883", clientId);
//        try {
//            IMqttToken token = client.connect();
//            token.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    // We are connected
//                    Toast.makeText(MainActivity.this, "MQTT Connected!", Toast.LENGTH_SHORT).show();
//                }
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    // Something went wrong e.g. connection timeout or firewall problems
//                    Toast.makeText(MainActivity.this, "MQTT Connection Failed!", Toast.LENGTH_SHORT).show();
//                }
//            });
//            System.out.println(token.getTopics());
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }


    private BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double temperature = (double) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            tvAmbientTemperature.setText("Battery Temperature: " + temperature + " " + (char) 0x00B0 + "C");
//            publishMessage(temperature+" C");


//            String topic = "data/seil/14/1122";
//            String payload = temperature + "";
//            byte[] encodedPayload = new byte[0];
//            try {
//                encodedPayload = payload.getBytes("UTF-8");
//                MqttMessage message = new MqttMessage(encodedPayload);
//                if (client != null)
//                    client.publish(topic, message);
//            } catch (UnsupportedEncodingException | MqttException e) {
//                e.printStackTrace();
//            }
        }
    };

    private MqttConnectOptions getMqttConnectionOption() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setWill(MQTT_TOPIC, "I am going offline".getBytes(), 1, true);
        //mqttConnectOptions.setUserName("username");
        //mqttConnectOptions.setPassword("password".toCharArray());
        return mqttConnectOptions;
    }
}

