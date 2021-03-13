package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.schema.myDBHandler;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;

import static android.bluetooth.BluetoothProfile.GATT;
import static android.content.Context.BLUETOOTH_SERVICE;

public class MQTTconnection {
    private static SharedPreferences sharedPrefs;
    private int iteration = 0;
    private String lastTimestamp = "0";
    public Timer timer = new Timer();
    private final List<BluetoothDevice> connectedDevice;
    private final String userMail;
    private final String userSms;
    private MqttAndroidClient mqttAndroidClient;
    private final String serverUri = "ssl://mqtt.tele.pw.edu.pl:8883";//"ssl://10.10.80.55:8883";
    private final String clientId = MqttClient.generateClientId();
    private final String username = "bkogut";
    private final String password = "Zyw0tStud3nta@";//"Zyw0tStud3nt@";
    String exportPath = "";


    public MQTTconnection(final Context context, final String action, String mail, String sms){
        userMail = mail;
        userSms = sms;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        BluetoothManager manager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        connectedDevice = manager.getConnectedDevices(GATT);

        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable throwable) {

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
        /*InputStream input;
        try {
            input = context.getAssets().open("ca.bks");
            mqttConnectOptions.setSocketFactory(mqttAndroidClient.getSSLSocketFactory(input, "password"));
        } catch (IOException | MqttSecurityException e) {
            Log.d("ssl_connection:", "failure");
            e.printStackTrace();
        }
          catch(NullPointerException e){
            Log.e("ssl_connection:", e.toString());
        }
*/
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("MQTT: Successfully connected to server: " + serverUri);
                    if (action == "steps")
                        publishSteps(context);
                    else if (action == "mailSms")
                        publishMailSms();
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                    if (iteration < 10)
                        connect(context, action);
                    iteration ++;
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
        Cursor cursorSteps = db.getSteps(lastTimestamp);
        String topic = "gbBKogut/MiBand/" + connectedDevice.toString() + "/steps";
        String message = null;

        if (cursorSteps.getCount() == 0){
        }
        else{

            while (cursorSteps.moveToNext()){
                Log.d(null,cursorSteps.getString(0) + " " + cursorSteps.getString(1));

                try {
                    message = new JSONObject()
                            .put("deviceId", connectedDevice.toString())
                            .put("steps", cursorSteps.getString(1))
                            .put("timestamp", cursorSteps.getString(0)).toString();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                publish(topic, message);
                System.out.println("Published" );

                SharedPreferences.Editor mEditor = sharedPrefs.edit();
                mEditor.putString("lastTimestamp", cursorSteps.getString(0)).commit();
            }
            Toast.makeText(context, "Published", Toast.LENGTH_SHORT).show();

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

}