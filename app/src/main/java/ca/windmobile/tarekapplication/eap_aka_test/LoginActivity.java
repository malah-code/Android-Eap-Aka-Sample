package ca.windmobile.tarekapplication.eap_aka_test;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;
import android.content.Context;
import android.util.Log;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };

    // UI references.
    private EditText mSSIDView;
    private TextView vTextViewStatus;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private WifiManager wifiManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Set up the login form.
        mSSIDView = (EditText) findViewById(R.id.txtSsid);
        vTextViewStatus = (TextView) findViewById(R.id.textViewStatus);


        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                Runnable runnable = new Runnable() {

                    public void run() {
                        attemptLogin();
                    }
                };
                new Thread(runnable).start();

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }



    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        }
    }

    /**
     *    This method takes a given String, searches the current list of configured WiFi
     *     networks, and returns the networkId for the network if the SSID matches. If not,
     *     it returns -1.
     */
    private int ssidToNetworkId(String ssid) {

        List<WifiConfiguration> currentNetworks = wifiManager.getConfiguredNetworks();
        int networkId = -1;

        // For each network in the list, compare the SSID with the given one
        for (WifiConfiguration test : currentNetworks) {
            if ( test.SSID.equals(ssid) ) {
                networkId = test.networkId;
                break;
            }
        }

        return networkId;
    }

    private void logThis(final String log) {
        runOnUiThread(new Runnable() {
            public void run() {
                vTextViewStatus.append(log);
            }
        });
    }
    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // perform the user login attempt.
       // showProgress(true);
        runOnUiThread(new Runnable() {
            public void run() {
                vTextViewStatus.setText("");
            }
        });


        // Initialize the WifiConfiguration object
        logThis("attemp to connect\n");
        mProgressView.refreshDrawableState();
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        WifiConfiguration wifi = new WifiConfiguration();
        wifi.SSID = mSSIDView.getText().toString();// "\"eap-aka-ansi\"";
        wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        enterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.AKA);
        wifi.enterpriseConfig = enterpriseConfig;

        logThis("finding saved WiFi\n");
        wifi.networkId = ssidToNetworkId(wifi.SSID);

        if (wifi.networkId == -1) {
            logThis("WiFi not found - adding it.\n");
            wifiManager.addNetwork(wifi);
        } else {
            logThis("WiFi found - updating it.\n");
            wifiManager.updateNetwork(wifi);
        }
        logThis("saving config.\n");
        wifiManager.saveConfiguration();

        wifi.networkId = ssidToNetworkId(wifi.SSID);
        logThis("wifi ID in device = " + wifi.networkId + "\n");

        SupplicantState supState;
        int networkIdToConnect = wifi.networkId;
        if (networkIdToConnect >= 0) {
            logThis("Start connecting...\n");

            // We disable the network before connecting, because if this was the last connection before
            // a disconnect(), this will not reconnect.
            wifiManager.disableNetwork(networkIdToConnect);
            wifiManager.enableNetwork(networkIdToConnect, true);


            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
            logThis("WifiWizard: Done connect to network : status =  " + supState.toString());
        } else {
            logThis("WifiWizard: cannot connect to network");
        }

        //showProgress(false);
    }




    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {


    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }




    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


}

