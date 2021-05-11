package nodomain.freeyourgadget.gadgetbridge.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


import com.google.gson.Gson;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.schema.myDBHandler;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;

public class MQTTconnection {
    private static SharedPreferences sharedPrefs;
    private int iteration = 0;
    private String lastTimestamp = "0";
    public Timer timer = new Timer();
    //private final List<BluetoothDevice> connectedDevice;
    private String connectedDevice;
    private final String userMail;
    private final String userSms;
    private List<String> neighborDevices = new ArrayList<>();
    private MqttAndroidClient mqttAndroidClient;
    private final String serverUri = "ssl://mqtt.tele.pw.edu.pl:8883";//"ssl://10.10.80.55:8883";
    private final String clientId = MqttClient.generateClientId();
    private final String username = "bkogut";
    private final String password = "Zyw0tStud3nta@";//"Zyw0tStud3nt@";
    private List<String> subscribedTopics = new ArrayList<>();
    String exportPath = "";

    public MQTTconnection(final Context context, final String action, String mail, String sms, List<String> devices){
        userMail = mail;
        userSms = sms;
        neighborDevices = devices;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        //BluetoothManager manager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        //connectedDevice = manager.getConnectedDevices(GATT);
        exportDB(context);
        myDBHandler db = new myDBHandler(context, null,null,1);
        connectedDevice = db.getDeviceId();

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w("CONNECTION LOST", "MQTTconnection");

                if (action == "getDevice")
                    connect(context, action);
            }
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });

        connect(context, action);

    }
    public void setCallback(MqttCallback callback) {
        mqttAndroidClient.setCallback(callback);
    }

    private void connect(final Context context, final String action){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setKeepAliveInterval(3600);
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("MQTT: Successfully connected to server: " + serverUri);
                    if (action.equals("steps")) {
                        publishSteps(context);
                        //Toast.makeText(context, "Opublikowano kroki", Toast.LENGTH_LONG).show();
                    }
                    else if (action.equals("mailSms")) {
                        publishMailSms();
                        //Toast.makeText(context, "Opublikowano notyfikacje", Toast.LENGTH_LONG).show();
                    }
                    else if (action.matches("\\d+")) {
                        publishMood(action);
                        //Toast.makeText(context, "Opublikowano samopoczucie", Toast.LENGTH_LONG).show();
                    }
                    else if (action.equals("sendDevices")) {
                        publishNeighborDevices(neighborDevices);
                        //Toast.makeText(context, "Opublikowano urządzenia w pobliżu", Toast.LENGTH_LONG).show();
                    }
                    else if (action.equals("getConfiguration")) {
                        try {
                            subscribe("configuration");

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    askForConfiguration();
                                }
                            }, 5 * 1000);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }

                }
                @SuppressLint("SimpleDateFormat")
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                    if (iteration < 10)
                        connect(context, action);
                    iteration ++;
                    if (iteration == 10) {
                        Log.w("Mqtt", "Niepowodzenie. Sprawdź połączenie z Internetem");
                        Toast.makeText(context, "Niepowodzenie. Sprawdź połączenie z Internetem", Toast.LENGTH_LONG).show();
                    }
                    if (iteration == 10) {
                        if (action.equals("sendDevices")) {
                            try {
                                saveNeighbors(neighborDevices, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (action.matches("\\d+")) {
                            try {
                                saveMood(action, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (MqttException ex){
            ex.printStackTrace();
        } }

    private void publish(String topic, String message){
        try {
            mqttAndroidClient.publish(topic, message.getBytes(),0,false);
        }catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void publishSteps(Context context){
        File myPath = null;
        try {
            myPath = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteRecursive(myPath);
        exportDB(context);
        lastTimestamp = sharedPrefs.getString("lastTimestamp", "0");//"2019-12-30 13:00:00";
        myDBHandler db = new myDBHandler(context, null,null,1);
        //Cursor cursorSteps = db.getSteps(lastTimestamp);
        Cursor cursor = db.getStepsAndHeartRate(lastTimestamp);
        String topic = "gbBKogut/MiBand/" + connectedDevice.toString() + "/steps";
        String message = null;

        if (cursor.getCount() == 0){
        }
        else{

            while (cursor.moveToNext()){
                Log.d(null,cursor.getString(0) + " " + cursor.getString(1));

                try {
                    message = new JSONObject()
                            .put("deviceId", connectedDevice.toString())
                            .put("steps", cursor.getString(1))
                            .put("heart_rate", cursor.getString(2))
                            .put("timestamp", cursor.getString(0)).toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                publish(topic, message);

                SharedPreferences.Editor mEditor = sharedPrefs.edit();
                mEditor.putString("lastTimestamp", cursor.getString(0)).commit();
            }
        }
    }
    private void publishMailSms(){
        String topic = "gbBKogut/MiBand/" + connectedDevice.toString();
        String messageMail = null;
        String messageSms = null;

        try {
            messageMail = new JSONObject()
                    .put("deviceId", connectedDevice.toString())
                    .put("mail", userMail).toString();
            messageSms = new JSONObject()
                    .put("deviceId", connectedDevice.toString())
                    .put("sms", userSms).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (userMail != null )
            publish(topic + "/mail", messageMail);
        if (userSms != null)
            publish(topic + "/sms", messageSms);

    }

    @SuppressLint("SimpleDateFormat")
    private void publishMood(String mood){
        String topic = "gbBKogut/MiBand/" + connectedDevice.toString();
        String messageMood = null;

        try {
            HashMap<String, String> hashMap = loadMoodFromPrefs();
            hashMap.put(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()), mood);

            for (Map.Entry<String, String> map : hashMap.entrySet()) {
                messageMood = new JSONObject()
                        .put("deviceId", connectedDevice.toString())
                        .put("timestamp", map.getKey())
                        .put("mood", map.getValue()).toString();

                if (mood != null )
                    publish(topic + "/mood", messageMood);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(connectedDevice + "_mood");
        editor.apply();
    }

    @SuppressLint("SimpleDateFormat")
    public void publishNeighborDevices(List<String> devices){
        String topic = "gbBKogut/MiBand/" + connectedDevice.toString();
        String messageDevices = null;

        try {
            HashMap<String, List<String>> hashMap = loadNeighbors();
            hashMap.put(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()), devices);

            for (Map.Entry<String, List<String>> map : hashMap.entrySet()) {

                messageDevices = new JSONObject()
                            .put("deviceId", connectedDevice.toString())
                            .put("timestamp", map.getKey())
                            .put("devices", map.getValue()).toString();

                publish(topic + "/neighborDevices", messageDevices);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.remove(connectedDevice + "_neighbors");
        editor.apply();
    }

    private void exportDB(Context context) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(context);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            exportPath = destFile.getAbsolutePath();
           // GB.toast(this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
           // GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }
    private void exportShared() {
        try {
            File myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.exportToFile(sharedPrefs, myFile, null);

        } catch (IOException ex) {
            //GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_shared, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();

    }

    public void subscribe(String topic) throws MqttException {
        mqttAndroidClient.subscribe("gbBKogut/MiBand/" + topic, 0);
        subscribedTopics.add(topic);
    }

    public boolean isSubscribed(String topic) throws MqttException {
        return subscribedTopics.contains(topic);
    }

    private void saveNeighbors(List<String> neighborDevices, String time) throws JSONException {
        HashMap<String, List<String>> hashMap = loadNeighbors();
        hashMap.put(time, neighborDevices);

        JSONObject jsonObject = new JSONObject(hashMap);
        String jsonString = jsonObject.toString();


        SharedPreferences.Editor mEditor = sharedPrefs.edit();
        mEditor.putString(connectedDevice + "_neighbors", jsonString).apply();
    }

    private HashMap<String, List<String>> loadNeighbors() throws JSONException {
        HashMap<String, List<String>> hashMap = new HashMap<>();

        String jsonString = sharedPrefs.getString(connectedDevice + "_neighbors", (new JSONObject()).toString());
        JSONObject jsonObject = new JSONObject(jsonString);
        Iterator<String> keysItr = jsonObject.keys();
        while(keysItr.hasNext()) {
            ArrayList<String> neighborList = new ArrayList<String>();
            String time = keysItr.next();
            JSONArray neighbors = jsonObject.getJSONArray(time);
            if (neighbors != null) {
                int len = neighbors.length();
                for (int i=0;i<len;i++){
                    neighborList.add(neighbors.get(i).toString());
                }
            }

            //Log.w("SHARED_NEIGHBORS", time + "_" + neighbors);
            hashMap.put(time, neighborList);
        }
        return hashMap;
    }

    private void askForConfiguration(){
        String topic = "gbBKogut/MiBand/getConfiguration" ;
        String message = "";

        publish(topic, message);
    }

    private void saveMood(String mood, String time) throws JSONException {
        HashMap<String, String> hashMap = loadMoodFromPrefs();
        hashMap.put(time, mood);

        JSONObject jsonObject = new JSONObject(hashMap);
        String jsonString = jsonObject.toString();


        SharedPreferences.Editor mEditor = sharedPrefs.edit();
        mEditor.putString(connectedDevice + "_mood", jsonString).apply();
    }

    private HashMap<String, String> loadMoodFromPrefs() throws JSONException {
        HashMap<String, String> hashMap = new HashMap<>();

        String jsonString = sharedPrefs.getString(connectedDevice + "_mood", (new JSONObject()).toString());
        JSONObject jsonObject = new JSONObject(jsonString);
        Iterator<String> keysItr = jsonObject.keys();
        while(keysItr.hasNext()) {
            String time = keysItr.next();
            String mood = jsonObject.getString(time);

            Log.w("SHARED_MOOD", time + "_" + mood);
            hashMap.put(time, mood);
        }
        return hashMap;
    }
}