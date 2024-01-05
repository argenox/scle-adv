package com.argenox.scleadv;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothDevice = null;

    private static final int REQUEST_PERMS_CODE = 20;

//    private ScanCallback mScanCallback = null;

    public static String ARGENOX_SENSOR_SERVICE_BASE_STR = "00001200-874b-3189-8b10-0006da7fd002";

    private static String ARGENOX_SENSOR_SERVICE_TX_CHAR_UUID = "00001100-874b-3189-8b10-0006da7fd002";
    private static String ARGENOX_SENSOR_SERVICE_SENS_CTRL_CHAR_UUID = "00001000-874b-3189-8b10-0006da7fd002";

    private static String ARGENOX_MANAGEMENT_TX_CHAR_UUID = "6d501000-a21a-448b-174a-91910cbfacaa";
    private static String ARGENOX_MANAGEMENT_RX_CHAR_UUID = "6d501100-a21a-448b-174a-91910cbfacaa";

    public static String ARGENOX_SENSOR_SERVICE_ACCEL_CHAR_UUID = "00001020-874b-3189-8b10-0006da7fd002";

    private static String ARGENOX_SENSOR_SERVICE_BUFFER_CTRL_CHAR_UUID = "00002030-874b-3189-8b10-0006da7fd002";
    private static String ARGENOX_SENSOR_SERVICE_BUFFER_DATA_CHAR_UUID = "00002040-874b-3189-8b10-0006da7fd002";

    private final String MODULE_TAG = "Argenox SCLE ADV";

    private ArrayList<String> currentNeededPermissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        managePermissions();

        Button clickButton = (Button) findViewById(R.id.scanButton);
        clickButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Request the ACCESS_FINE_LOCATION permission at runtime
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
//            RequestPermissions(new string[] { android.Manifest.permission.ACCESS_FINE_LOCATION },
//                    REQUEST_FINE_LOCATION_PERMISSION);

                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            1);
                }

                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            1);
                }

                // Request the BLUETOOTH_SCAN permission at runtime
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{android.Manifest.permission.BLUETOOTH_SCAN},
                            1);
                }

                //Request the BLUETOOTH_CONNECT permission at runtime
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            1);
                }

                UUID sensorUUID;
                List<UUID> searchUUIDList = null;

                if (ARGENOX_SENSOR_SERVICE_BASE_STR != null) {
                    sensorUUID = UUID.fromString(ARGENOX_SENSOR_SERVICE_BASE_STR);
                    searchUUIDList = Arrays.asList(sensorUUID);
                }

                Log.v(MODULE_TAG, "Initializing Scan");
//                initScan();

                Log.v(MODULE_TAG, "Starting Scanning with" + searchUUIDList);
                scanWithServiceUUID(searchUUIDList, 0);
            }
        });
    }

    /*!
     * \brief Finds devices with matching Service UUID
     *
     * Begins a scan that will collect scan results and find all devices with matching service UUID
     *
     * @param None.
     *
     * @return true if BLE available, false otherwise
     */
    @SuppressWarnings("unused")
    public void scanWithServiceUUID(List<UUID> srvcUUIDs, int timeout) {

        ScanSettings settings = null;
        List<ScanFilter> filters = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 21) {

            if (srvcUUIDs != null) {
                for (UUID srvcUUID : srvcUUIDs) {
                    ParcelUuid scanUUID = new ParcelUuid(srvcUUID);
                    ScanFilter addrFilter = new ScanFilter.Builder().setServiceUuid(scanUUID).build();
                    filters.add(addrFilter);
                }
            }

            settings = new ScanSettings.Builder() //
                    //.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) //
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //
                    .build();
        }

        /* Scan for devices */
        startScanning(timeout, filters, settings);
    }

    public static String[] retrievePermissions(Context context) {
        try {
            return context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Application", "Error Exception not found");
        }

        return null;
    }

    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.e("Activity result","OK");
                    // There are no request codes
                    Intent data = result.getData();
                }
            });

    public boolean managePermissions() {


        /* Check for permissions which are needed */

        String[] allPerms = retrievePermissions(getApplicationContext());

        currentNeededPermissions.clear();

        if (allPerms != null && allPerms.length > 0) {
            for (String perm : allPerms) {
                int hasPermission = checkSelfPermission(perm);

                if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                    currentNeededPermissions.add(perm);
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,            android.Manifest.permission.BLUETOOTH_CONNECT)) {

                String[] connPerm = {Manifest.permission.BLUETOOTH_CONNECT};
                ActivityCompat.requestPermissions(this, connPerm,1);
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
        ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
            android.Manifest.permission.BLUETOOTH_SCAN
                )) {
                String[] connPerm = {Manifest.permission.BLUETOOTH_SCAN};
                ActivityCompat.requestPermissions(this, connPerm,1);
            }
        }

        if (currentNeededPermissions.size() > 0) {
            /* Request the first permission needed */

            Log.d(MODULE_TAG, "Requesting Permission for " + currentNeededPermissions.get(0));

            String[] curPermsMissing = new String[currentNeededPermissions.size()];
            curPermsMissing = currentNeededPermissions.toArray(curPermsMissing);
            //requestPermissions(curPermsMissing, REQUEST_PERMS_CODE);

//            ActivityCompat.requestPermissions(MainActivity.this, curPermsMissing,1);
//            for (String perm : curPermsMissing) {
//                //requestPermission(perm, REQUEST_PERMISSION_PHONE_STATE);
//                requestPermissionLauncher.launch(perm);
//                //requestPermission(MainActivity.this, perm,1);
//
//            }
            int permissionsCode = 42;


            ActivityCompat.requestPermissions(this, curPermsMissing, permissionsCode);


        }





        return false;
    }
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            Log.v(MODULE_TAG, "Scan Result " + result);

            ScanRecord scanRecord = result.getScanRecord();
            byte[] data = scanRecord.getManufacturerSpecificData(576);

            int tempDec = data[4] | (data[5] << 8);
            float tempFloat = tempDec / 100.0f;
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            Log.d(MODULE_TAG, "Batch Scan Results");

            /*  Process a   batch   scan    results */
            for (ScanResult sr : results) {
                Log.i("Scan Item:   ", sr.toString());
            }
        }
    };

    /* start scanning for BT LE devices around */
    @SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
    private boolean startScanning(long scanTimeoutMs, List<ScanFilter> filters, ScanSettings settings) {
        /* Guard against scanning if not enabled */
        if (!isBtEnabled()) {
            return false;
        }


        if (Build.VERSION.SDK_INT < 21) {
            Log.d(MODULE_TAG, "Not supported in this app to use Scanning API < 21");

        } else {

            Log.d(MODULE_TAG, "Scanning API >= 21");
            if (settings == null) {

                settings = new ScanSettings.Builder() //
                        //.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) //
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED) //
                        .build();
            }

            BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(MODULE_TAG, "Error No permission to scan");
                //return false;
            }
            bluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, mScanCallback);
        }
        return true;
    }


    private boolean isBtEnabled() {


        final BluetoothManager manager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null)
            return false;

        final BluetoothAdapter adapter = manager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            Log.d("onLeScan", device.toString());


        }
    };


}