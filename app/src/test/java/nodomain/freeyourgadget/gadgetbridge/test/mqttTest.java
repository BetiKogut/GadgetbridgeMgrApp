package nodomain.freeyourgadget.gadgetbridge.test;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import nodomain.freeyourgadget.gadgetbridge.activities.MQTTconnection;

public class mqttTest {
    private void startMqtt(String action){
        MQTTconnection mqttConnection = new MQTTconnection(null, action, "testmail", "testsms");
        mqttConnection.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug",mqttMessage.toString());
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

}
