package com.android.sapantanted.ambienttemperaturepredictor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import com.android.sapantanted.ambienttemperaturepredictor.helper.Constants;
import com.android.sapantanted.ambienttemperaturepredictor.model.TemperatureReading;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static android.content.ContentValues.TAG;
import static com.android.sapantanted.ambienttemperaturepredictor.helper.Functions.*;
import static com.android.sapantanted.ambienttemperaturepredictor.helper.Constants.*;

public class SensingService extends Service {
    private MqttAndroidClient mqttAndroidClient;
    private String macAdd;
    private String clientId = "AmbientTemperaturePredictor";
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
        macAdd = getMacAddr(getApplicationContext());

        clientId = clientId + System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(this, SERVER_URI, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    makeToast("Reconnected to : " + serverURI);
                    logInfo(SensingService.this,"[MQTT Reconnected]");
                } else {
                    makeToast("MQTT Connected!");
                    logInfo(SensingService.this,"[MQTT Connected]");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                makeToast("The Connection was lost." + cause.getMessage());
                cause.printStackTrace();
                logError(SensingService.this, "[MQTT Connection Lost]" + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                makeToast("Incoming message: " + new String(message.getPayload()));
                logInfo(SensingService.this,"[Message Arrived]"+new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
//                makeToast("Message delivered! ");
            }
        });
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        try {
            mqttAndroidClient.connect(mqttConnectOptions,
                    null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
//                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
//                            disconnectedBufferOptions.setBufferEnabled(true);
//                            disconnectedBufferOptions.setBufferSize(100);
//                            disconnectedBufferOptions.setPersistBuffer(false);
//                            disconnectedBufferOptions.setDeleteOldestMessages(false);
//                            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
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

    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, MQTT_QOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
//                    makeToast("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    makeToast("Failed to subscribe");
                }
            });
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, MQTT_QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String temperatureSensorId = getStringFromSP(SensingService.this, "temperature_sensor_id");
                    String sensorReading = new String(message.getPayload());
                    String id = sensorReading.split(",")[0];
                    if (!id.equalsIgnoreCase(temperatureSensorId)) return;
                    double actualTemperatureReading = Double.parseDouble(sensorReading.split(",")[1]);
                    TemperatureReading tr = new TemperatureReading(macAdd, -1,
                            getBatteryTemperature(SensingService.this), 0.0,
                            getRamUsagePercentage(SensingService.this), id, actualTemperatureReading,
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
                            getBatteryPercentage(SensingService.this),
                            getBatteryVoltage(SensingService.this),
                            isCharging(SensingService.this),
                            isTurboCharging(SensingService.this)
                    );
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
            mqttAndroidClient.publish(PUBLISH_TOPIC, message);
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

    @Override
    public void onDestroy() {
        makeToast("Destroy() called!");
        try {
            mqttAndroidClient.unsubscribe(SUBSCRIPTION_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mqttAndroidClient.unregisterResources();
        wl.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
