/*  Copyright (C) 2016-2019 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.browse.MediaBrowser;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Timer;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.schema.myDBHandler;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.ImportExportSharedPreferences;

import static android.bluetooth.BluetoothProfile.GATT;
import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;


public class DbManagementActivity extends AbstractGBActivity implements LocationListener {
    public static final Logger LOG = LoggerFactory.getLogger(DbManagementActivity.class);
    public static SharedPreferences sharedPrefs;
    public static String exportPath = "";
    public MqttAndroidClient client;
    public String lastTimestamp = "0";
    public String mail = null;
    public String sms = null;
    public int iteration = 0;
    public Timer timer = new Timer();
    MQTTconnection mqttConnection;
    private static final int REQUEST_PERMISSION_LOCATION = 255;


    ///
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Context context;
    String lat;
    String provider;
    protected String latitude, longitude;
    protected boolean gps_enabled, network_enabled;
    ///

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_management);

        ///
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        ///

        TextView dbPath = findViewById(R.id.activity_db_management_path);
        dbPath.setText(getExternalPath());

        Button exportDBButton = findViewById(R.id.exportDBButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                exportDB();
                //getGpsLocation();
            }
        });
        Button importDBButton = findViewById(R.id.importDBButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });
        Button MQTTDBButton = findViewById(R.id.MQTTDBButton);
        MQTTDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //connectMQTT();
                startMqtt("steps");



            }
        });

        final EditText editMail = findViewById(R.id.enterMail);
        final EditText editSms = findViewById(R.id.enterSms);
        final Switch switchMail = findViewById(R.id.switchMail);
        final Switch switchSms = findViewById(R.id.switchSMS);

        switchMail.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mail = editMail.getText().toString();
                } else {
                    mail = null;
                }
            }
        });

        switchSms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sms = editSms.getText().toString();
                } else {
                    sms = null;
                }
            }
        });

        Button applyMail = findViewById(R.id.APPLY);
        applyMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editSms.setText("");
                editMail.setText("");

                startMqtt("mailSms");

            }
        });

        int oldDBVisibility = hasOldActivityDatabase() ? View.VISIBLE : View.GONE;

        Button deleteOldActivityDBButton = findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setVisibility(oldDBVisibility);
        deleteOldActivityDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOldActivityDbFile();
            }
        });

        Button deleteDBButton = findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

    }

    ///
    @Override
    public void onLocationChanged(Location location) {
        //txtLat = (TextView) findViewById(R.id.textview1);
        final EditText editMail = findViewById(R.id.enterMail);
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());
        editMail.setText(latitude + "_" + longitude);

        //Log.d("!!!!!!!!!!","Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }
    ///

    public boolean hasOldActivityDatabase() {
        return new DBHelper(this).existsDB("ActivityDatabase");
    }

    public String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
            LOG.warn("Unable to get external files dir", ex);
        }
        return getString(R.string.dbmanagementactivvity_cannot_access_export_path);
    }

    public void exportShared() {
        try {
            File myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.exportToFile(sharedPrefs, myFile, null);
            System.out.println(sharedPrefs);

        } catch (IOException ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_shared, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    public void importShared() {
        try {
            File myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.importFromFile(sharedPrefs, myFile);
        } catch (Exception ex) {
            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }


    public void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            exportPath = destFile.getAbsolutePath();
            GB.toast(this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    public void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_import_data_title)
                .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                .setPositiveButton(R.string.dbmanagementactivity_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            importShared();
                            DBHelper helper = new DBHelper(DbManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_import_successful), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
                .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_database_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    public void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this).setCancelable(true);
        new AlertDialog.Builder(this).setTitle(R.string.dbmanagementactivity_delete_old_activity_db);
        new AlertDialog.Builder(this).setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation);
        new AlertDialog.Builder(this).setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (GBApplication.deleteOldActivityDatabase(DbManagementActivity.this)) {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
        new AlertDialog.Builder(this).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        new AlertDialog.Builder(this).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startMqtt(String action) {
        mqttConnection = new MQTTconnection(getApplicationContext(), action, mail, sms);
        mqttConnection.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

}
