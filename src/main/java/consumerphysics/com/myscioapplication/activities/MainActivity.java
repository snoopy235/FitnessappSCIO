package consumerphysics.com.myscioapplication.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.sdk.callback.cloud.ScioCloudAnalyzeManyCallback;
import com.consumerphysics.android.sdk.callback.cloud.ScioCloudUserCallback;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceCalibrateHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceCallback;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceCallbackHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceConnectHandler;
import com.consumerphysics.android.sdk.callback.device.ScioDeviceScanHandler;
import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.ScioReading;
import com.consumerphysics.android.sdk.model.ScioUser;
import com.consumerphysics.android.sdk.sciosdk.ScioCloud;
import com.consumerphysics.android.sdk.sciosdk.ScioDevice;
import com.consumerphysics.android.sdk.sciosdk.ScioLoginActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import consumerphysics.com.myscioapplication.R;
import consumerphysics.com.myscioapplication.adapter.ScioModelAdapter;

public final class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int LOGIN_ACTIVITY_RESULT = 1000;

    // TODO: Put your redirect url here!
    private static final String REDIRECT_URL = "https://www.consumerphysics.com";

    // TODO: Put your app key here!
    private static final String APPLICATION_KEY = "4b5ac28b-28f9-4695-b784-b7665dfe3763";

    // Scio
    private ScioDevice scioDevice;
    private ScioCloud scioCloud;

    // UI
    private TextView nameTextView;
    private TextView addressTextView;
    private TextView statusTextView;
    private TextView usernameTextView;
    private TextView modelTextView;
    private TextView version;
    private ProgressDialog progressDialog;

    // Members
    private String deviceName;
    private String deviceAddress;
    private String username;
    private String modelId;
    private String modelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initScioCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (scioCloud.hasAccessToken() && username == null) {
            getScioUser();
        }

        updateDisplay();
    }

    @Override
    public void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        // Make sure scio device is disconnected or leaks may occur
        if (scioDevice != null) {
            scioDevice.disconnect();
            scioDevice = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LOGIN_ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "We are logged in.");
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final String description = data.getStringExtra(ScioLoginActivity.ERROR_DESCRIPTION);
                            final int errorCode = data.getIntExtra(ScioLoginActivity.ERROR_CODE, -1);

                            Toast.makeText(MainActivity.this, "An error has occurred.\nError code: " + errorCode + "\nDescription: " + description, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                break;
        }
    }

    public void doLogout(final View view) {
        Log.d(TAG, "doLogout");

        if (scioCloud != null) {
            scioCloud.deleteAccessToken();

            storeUsername(null);
            updateDisplay();
        }
    }

    public void doLogin(final View view) {
        Log.d(TAG, "doLogin");

        if (!scioCloud.hasAccessToken()) {
            final Intent intent = new Intent(this, ScioLoginActivity.class);
            intent.putExtra(ScioLoginActivity.INTENT_REDIRECT_URI, REDIRECT_URL);
            intent.putExtra(ScioLoginActivity.INTENT_APPLICATION_ID, APPLICATION_KEY);

            startActivityForResult(intent, LOGIN_ACTIVITY_RESULT);
        }
        else {
            Log.d(TAG, "Already have token");

            getScioUser();
        }
    }

    public void doDiscover(View view) {
        Intent intent = new Intent(this, DiscoverActivity.class);
        startActivity(intent);
    }

    public void doModels(final View view) {
        if (scioCloud == null || !scioCloud.hasAccessToken()) {
            Log.d(TAG, "Can not select collection. User is not logged in");
            Toast.makeText(getApplicationContext(), "Can not select collection. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final Intent intent = new Intent(this, ModelActivity.class);
        startActivity(intent);
    }

    public void doConnect(final View view) {
        if (deviceAddress == null) {
            Toast.makeText(getApplicationContext(), "No SCiO is selected", Toast.LENGTH_SHORT).show();
            return;
        }

        scioDevice = new ScioDevice(this, deviceAddress);

        scioDevice.setButtonPressedCallback(new ScioDeviceCallback() {
            @Override
            public void execute() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay();
                        Toast.makeText(getApplicationContext(), "SCiO button was pressed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        progressDialog = ProgressDialog.show(this, "Please Wait", "Connecting...", false);

        scioDevice.connect(new ScioDeviceConnectHandler() {
            @Override
            public void onConnected() {
                Log.d(TAG, "SCiO was connected successfully");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay();
                        Toast.makeText(getApplicationContext(), "SCiO was connected successfully", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onConnectFailed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay();
                        Toast.makeText(getApplicationContext(), "Connection to SCiO failed", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onTimeout() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplay();
                        Toast.makeText(getApplicationContext(), "Connection to SCiO timed out.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    public void doDisconnect(final View view) {
        if (scioDevice == null || !scioDevice.isConnected()) {
            Toast.makeText(getApplicationContext(), "SCiO not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Disconnecting...", false);

        scioDevice.setScioDisconnectCallback(new ScioDeviceCallback() {
            @Override
            public void execute() {
                Toast.makeText(getApplicationContext(), "SCiO disconnected.", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();

                updateDisplay();
            }
        });

        scioDevice.disconnect();
    }

    public void doCalibrate(final View view) {
        if (scioDevice == null || !scioDevice.isConnected()) {
            Log.d(TAG, "Can not calibrate. SCiO is not connected");
            Toast.makeText(getApplicationContext(), "Can not calibrate. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Calibrating...", false);

        scioDevice.calibrate(new ScioDeviceCalibrateHandler() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "SCiO was calibrated successfully");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "SCiO was calibrated successfully", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onError() {
                Log.e(TAG, "Error while calibrating");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while calibrating", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onTimeout() {
                Log.e(TAG, "Timeout while calibrating");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while calibrating", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    public void doRename(final View view) {
        if (scioDevice == null || !scioDevice.isConnected()) {
            Log.d(TAG, "Can not calibrate. SCiO is not connected");
            Toast.makeText(getApplicationContext(), "Can not rename device. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Rename SCiO");
        alertDialog.setMessage("Enter new name");

        final EditText input = new EditText(MainActivity.this);
        input.setHint(nameTextView.getText().toString());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                scioDevice.renameDevice(input.getText().toString(), new ScioDeviceCallbackHandler() {
                    @Override
                    public void onSuccess() {
                        storeDeviceName(input);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Device renamed.", Toast.LENGTH_SHORT).show();
                                nameTextView.setText(input.getText().toString());
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Rename device failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onTimeout() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Rename device failed due to SCiO timeout", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void storeDeviceName(final EditText input) {
        getSharedPreferences().edit().putString("scio_name", input.getText().toString()).commit();
    }

    public void doScan(final View view) {

        if (scioDevice == null || !scioDevice.isConnected()) {
            Log.d(TAG, "Can not scan. SCiO is not connected");
            Toast.makeText(getApplicationContext(), "Can not scan. SCiO is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modelId == null) {
            Log.d(TAG, "Can not scan. Model was not selected.");
            Toast.makeText(getApplicationContext(), "Can not scan. Model was not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scioCloud == null || !scioCloud.hasAccessToken()) {
            Log.d(TAG, "Can not scan. User is not logged in");
            Toast.makeText(getApplicationContext(), "Can not scan. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Please Wait", "Analyzing...", false);

        scioDevice.scan(new ScioDeviceScanHandler() {
            @Override
            public void onSuccess(final ScioReading reading) {
                Log.d(TAG, "onSuccess");

                // ScioReading object is Serializable and can be saved to be used later for analyzing.

                List<String> modelsToAnalyze = new ArrayList<>();
                modelsToAnalyze.addAll(Arrays.asList(modelId.split(",")));

                scioCloud.analyze(reading, modelsToAnalyze, new ScioCloudAnalyzeManyCallback() {
                    @Override
                    public void onSuccess(final List<ScioModel> models) {
                        Log.d(TAG, "analyze onSuccess");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showAnalyzeResults(models);
                                progressDialog.dismiss();
                            }
                        });
                    }

                    @Override
                    public void onError(final int code, final String msg) {
                        Log.d(TAG, "analyze onError");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Error while analyzing: " + msg, Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });

                    }
                });
            }

            @Override
            public void onNeedCalibrate() {
                Log.d(TAG, "onNeedCalibrate");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Can not scan. Calibration is needed", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onError() {
                Log.d(TAG, "onError");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while scanning", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });

            }

            @Override
            public void onTimeout() {
                Log.d(TAG, "onTimeout");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Timeout while scanning", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void getScioUser() {
        progressDialog = ProgressDialog.show(this, "Please Wait", "Getting User Info...", false);

        scioCloud.getScioUser(new ScioCloudUserCallback() {
            @Override
            public void onSuccess(final ScioUser user) {
                Log.d(TAG, "First name=" + user.getFirstName() + " Last name=" + user.getLastName());

                storeUsername(user.getUsername());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Welcome " + user.getFirstName() + " " + user.getLastName(), Toast.LENGTH_SHORT).show();

                        updateDisplay();

                        progressDialog.dismiss();
                    }
                });
            }

            @Override
            public void onError(final int code, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Error while getting the user info.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void showAnalyzeResults(final List<ScioModel> models) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Results");

        final LayoutInflater inflater = getLayoutInflater();
        final View convertView = inflater.inflate(R.layout.results_view, null);
        builder.setView(convertView);

        final ArrayList<ScioModel> arrayOfModels = new ArrayList<>();
        final ScioModelAdapter scioModelAdapter = new ScioModelAdapter(this, arrayOfModels);

        final ListView listView = (ListView) convertView.findViewById(R.id.results);
        listView.setAdapter(scioModelAdapter);

        scioModelAdapter.addAll(models);

        builder.setPositiveButton("OK", null);
        builder.setCancelable(true);

        builder.create().show();
    }

    private void initScioCloud() {
        scioCloud = new ScioCloud(this);
    }

    private void initUI() {
        setContentView(R.layout.activity_main);

        nameTextView = (TextView) findViewById(R.id.tv_scio_name);
        addressTextView = (TextView) findViewById(R.id.tv_scio_address);
        statusTextView = (TextView) findViewById(R.id.tv_scio_status);
        usernameTextView = (TextView) findViewById(R.id.tv_username);
        modelTextView = (TextView) findViewById(R.id.tv_model);
        version = (TextView) findViewById(R.id.version);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setText("v" + pInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void storeUsername(final String username) {
        this.username = username;

        getSharedPreferences().edit().putString("username", username).commit();
    }

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences("MyPref", Context.MODE_PRIVATE);
    }

    private void updateDisplay() {
        Log.d(TAG, "Updating the display");

        final SharedPreferences pref = getSharedPreferences();

        deviceName = pref.getString("scio_name", null);
        deviceAddress = pref.getString("scio_address", null);
        username = pref.getString("username", null);
        modelName = pref.getString("model_name", null);
        modelId = pref.getString("model_id", null);

        nameTextView.setText(deviceName);
        addressTextView.setText(deviceAddress);
        usernameTextView.setText(username);
        modelTextView.setText(modelName);

        if (scioDevice == null || !scioDevice.isConnected()) {
            statusTextView.setText("Disconnected");
        }
        else {
            statusTextView.setText("Connected");
        }
    }
}