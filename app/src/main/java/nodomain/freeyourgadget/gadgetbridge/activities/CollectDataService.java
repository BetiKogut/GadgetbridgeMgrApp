package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ObjectWrapperForBinder;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;

public class CollectDataService extends Service {
    GBDevice gbDevice;
    private List<String> neighborDevices = new ArrayList<>();
    private final List<String> myDevices = new ArrayList<>(Arrays.asList("C1:BE:2C:35:A6:45", "F1:96:86:DC:94:EA", "F0:F9:B3:A4:14:8A", "EC:86:E9:AF:93:B9", "C1:70:F6:3D:D9:36",    "ED:A9:27:9E:CF:70", "C8:0F:10:25:2C:B7"));
    final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static MQTTconnection mqttConnection;
    private int scanTime = 30; //seconds
    private int interval = 15;  //minutes

    @Override
        public IBinder onBind(Intent intent) {
            // TODO: Return the communication channel to the service.
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public void onCreate() {
            // TODO Auto-generated method stub
            Log.w("TIMER", "Service Created m");
            Toast.makeText(getApplicationContext(), "Service Created m", Toast.LENGTH_LONG).show();
            super.onCreate();
        }

        @Override
        public void onDestroy() {
            // TODO Auto-generated method stub
            Log.w("TIMER", "Service Destroy");
            super.onDestroy();
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            int minute = calendar.get(Calendar.MINUTE);

            GBDevice device = ((ObjectWrapperForBinder)intent.getExtras().getBinder("KEY")).getData();

            if (device != null){
                gbDevice = device;
            }
            // TODO Auto-generated method stub
            Log.w("TIMER", "Service Running m");
            Toast.makeText(getApplicationContext(), "Service Running m", Toast.LENGTH_LONG).show();

            disconnect();
            startBluetoothScanning();
            stopBluetoothScanning();

            if (minute >= 0 && minute < 15) {
                calendar.set(Calendar.MINUTE, 15);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            if (minute >= 15 && minute < 30) {
                calendar.set(Calendar.MINUTE, 30);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            if (minute >= 30 && minute < 45) {
                calendar.set(Calendar.MINUTE, 45);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            if (minute >= 45){
                //calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR) + 1);
                //calendar.add(Calendar.HOUR, 1);
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            UUID uuid = UUID.randomUUID();
            Log.w("Debug", "*****" + calendar.getTime());
            PendingIntent pintent = PendingIntent.getService(getContext(), uuid.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarm = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pintent);

            //calendar.getTimeInMillis()
            //System.currentTimeMillis()+(3*60*1000)

            getConfiguration();

            return START_STICKY;
        }

    private void getConfiguration(){
            mqttConnection = new MQTTconnection(getContext(), "getConfiguration", null, null, null);
            mqttConnection.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                    Toast.makeText(getContext(), "Message arrived", Toast.LENGTH_LONG).show();
                    Log.w("Debug", topic + mqttMessage.toString());

                    JSONObject data = new JSONObject(mqttMessage.toString());
                    scanTime = data.getInt("scanTime");
                    interval = data.getInt("interval");
                    Log.w("NEW_CONFIG", "scanTime: " + scanTime + " interval: " + interval);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
    }

    private void disconnect(){
        Log.w("*Disconnected", "Disconnected");
        GBApplication.deviceService().disconnect();

    }

    private void connect (){
        GBApplication.deviceService().connect(gbDevice);
    }


    void startBluetoothScanning(){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("*CollectDataStartScan: " , "time " + Calendar.getInstance().getTime());
                neighborDevices.clear();
                mBluetoothAdapter.startDiscovery();

            }
        }, 5 * 1000);
    }
    void stopBluetoothScanning(){
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("*StopBTScan: " , "time " + Calendar.getInstance().getTime());
                mBluetoothAdapter.cancelDiscovery();
                mqttConnection = new MQTTconnection(getContext(), "sendDevices", null, null, neighborDevices);

                connect();

            }
        }, (5 + scanTime) * 1000);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                Log.i("Device Name: " , "device " + deviceName);
                Log.i("deviceHardwareAddress " , "hard"  + deviceHardwareAddress);

                if (device.getName()!= null ) {
                    if (myDevices.contains(device.getAddress()) && !neighborDevices.contains(device.getAddress())){
                        neighborDevices.add(device.getAddress());
                    }
                    Log.i("*BTDeviceFound", "BLE device found: " + device.getName() + "; MAC " + device.getAddress());
                }
            }
        }
    };
}
