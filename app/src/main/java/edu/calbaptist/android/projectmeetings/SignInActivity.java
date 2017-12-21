package edu.calbaptist.android.projectmeetings;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;
import java.util.Arrays;

import edu.calbaptist.android.projectmeetings.async.meeting.GetMeetingAsync;
import edu.calbaptist.android.projectmeetings.async.user.CreateUserAsync;
import edu.calbaptist.android.projectmeetings.async.user.UpdateUserAsync;
import edu.calbaptist.android.projectmeetings.exceptions.RestClientException;
import edu.calbaptist.android.projectmeetings.models.Meeting;
import edu.calbaptist.android.projectmeetings.models.User;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClientMeetingCallback;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClientUserCallback;

/**
 *  Sign In Activity
 *  Assists the user with logging into the app.
 *
 *  @author Caleb Solorio
 *  @version 0.7.0 12/20/17
 */

public class SignInActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String TAG = "SignInActivity";
    private final static SharedPreferences PREFERENCES = App.context.getSharedPreferences(
            "edu.calbaptist.android.projectmeetings.Account_Name",
            Context.MODE_PRIVATE);

    private GoogleAccountCredential mCredential;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { DriveScopes.DRIVE_METADATA_READONLY };
    private static final int GOOGLE_SIGN_IN = 9001;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private LinearLayout buttonContainer;
    private SignInButton signInButton;
    private Button signOutButton;
    private LinearLayout connectingContainer;
    private TextView welcomeText;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private Meeting meeting;

    /**
     * Initializes SignInActivity.
     * @param savedInstanceState Contains any data sent from the previous activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(PREFERENCES.getBoolean("signed_in",false)) {
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();

            if (intent != null && extras != null) {
                String type = extras.getString("type");

                if(type != null) {
                    switch (type) {
                        case "meeting_invite":
                            Intent transfer = new Intent(this, MeetingListActivity.class);
                            startActivity(transfer);
                            break;
                        case "meeting_warn":
                            toMeetingActivity(extras.getString("m_id"));
                            break;
                        case "meeting_start":
                            toMeetingActivity(extras.getString("m_id"));
                            break;
                    }
                } else {
                    Intent transfer = new Intent(this, MeetingListActivity.class);
                    startActivity(transfer);
                }
            } else {
                Intent transfer = new Intent(this, MeetingListActivity.class);
                startActivity(transfer);
            }
        }

        setContentView(R.layout.activity_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mAuth = FirebaseAuth.getInstance();

        buttonContainer = (LinearLayout) findViewById(R.id.layout_button_container);

        signInButton = (SignInButton) findViewById(R.id.button_sign_in);
        signInButton.setOnClickListener(this);

        signOutButton = (Button) findViewById(R.id.button_sign_out);
        signOutButton.setOnClickListener(this);

        if(PREFERENCES.getString("firebase_token", null) == null) {
            signOutButton.setEnabled(false);
        }

        connectingContainer = (LinearLayout) findViewById(R.id.layout_main_connecting_container);
        welcomeText = (TextView) findViewById(R.id.text_main_welcome);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        // Configure FCM notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = getString(R.string.notification_channel_id);
            String channelName = getString(R.string.notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

    }

    /**
     * Executes on activity start.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Handles OnClick events.
     * @param view the view in which the event occurred.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sign_in:
                signIn();
                break;
            case R.id.button_sign_out:
                signOut();
                break;
        }

    }

    /**
     * Initializes the Google sign-in activity.
     */
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case GOOGLE_SIGN_IN:
                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...)
                GoogleSignInResult acct = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                try {
                    handleSignInResult(acct);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    showToast("This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                    showGooglePlayServicesAvailabilityErrorDialog(REQUEST_GOOGLE_PLAY_SERVICES);
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                    data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (accountName != null) {
                        SharedPreferences.Editor editor = PREFERENCES.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.putBoolean("signed_in", true);
                        editor.apply();

                        mCredential.setSelectedAccountName(accountName);
                    }
                }
                break;
        }
    }

    /**
     * Handles the result of the Google sign-in request.
     */
    private void handleSignInResult(GoogleSignInResult result) throws IOException, GoogleAuthException {
        if(result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount user = result.getSignInAccount();
            firebaseAuthWithGoogle(user);

            assert user != null;
            PREFERENCES.edit().putString("google_token", user.getIdToken()).apply();
        } else {
            showToast("Sign in with Google failed :(");
        }
    }

    /**
     * Handles the result of signing in with Google.
     */
    private void firebaseAuthWithGoogle(final GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            final FirebaseUser user = mAuth.getCurrentUser();

                            FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
                            assert mUser != null;
                            mUser.getToken(true)
                                    .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                        public void onComplete(@NonNull Task<GetTokenResult> task) {
                                            if (task.isSuccessful()) {
                                                try {
                                                    createUser(acct.getDisplayName(), acct.getEmail(), acct.getIdToken());
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                            showToast("Authentication with Firebase failed :(");
                        }
                    }
                });
    }

    /**
     * Executes on failure to connect with Google.
     * @param connectionResult Contains data on the connection attempt.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        showToast("Failed to connect with Google :(");
    }

    /**
     * Signs the user out of the app.
     */
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                PREFERENCES.edit().clear().commit();
                signOutButton.setEnabled(false);
            }
        });
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                SignInActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Creates a new user.
     */
    private void createUser(final String displayName, final String email, final String gToken) throws IOException, GoogleAuthException {
        buttonContainer.setVisibility(View.GONE);
        connectingContainer.setVisibility(View.VISIBLE);
        welcomeText.setText("Welcome, " + displayName + "!");

        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        assert mUser != null;
        mUser.getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            final String idToken = task.getResult().getToken();
                            final User user = new User.UserBuilder()
                                    .setDisplayName(displayName)
                                    .setEmail(email)
                                    .setFirebaseToken(idToken)
                                    .setGoogleToken(gToken)
                                    .setInstanceId(FirebaseInstanceId.getInstance().getToken())
                                    .build();

                            new CreateUserAsync(user, new AsyncCreateUserCallback()).execute();
                        }
                    }
                });
    }

    /**
     * Get's the meeting asynchronously.
     * @param mId Specifies the meeting to get.
     */
    private void toMeetingActivity(final String mId) {
        new GetMeetingAsync(mId, new AsyncMeetingCallback()).execute();
    }

    /**
     * Shows the given string in a short Toast.
     */
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows the sign-in buttons to the user.
     */
    private void showSignInButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonContainer.setVisibility(View.VISIBLE);
                connectingContainer.setVisibility(View.GONE);

                if(PREFERENCES.getString("firebase_token", null) != null) {
                    signOutButton.setEnabled(true);
                }
            }
        });
    }

    /**
     * Specifies the RestClientUserCallback implementation after updating a user's data.
     */
    private class AsyncCreateUserCallback implements RestClientUserCallback {
        @Override
        public void onTaskExecuted(User user) {
            SharedPreferences.Editor editor = PREFERENCES.edit();
            editor.putString(PREF_ACCOUNT_NAME, user.getEmail());
            editor.putBoolean("signed_in", true);
            editor.putString("u_id",user.getUid());
            editor.putString("display_name",user.getDisplayName());
            editor.putString("email",user.getEmail());
            editor.putString("firebase_token", user.getFirebaseToken());
            editor.putString("google_token", user.getGoogleToken());
            editor.putString("instance_id", user.getInstanceId());
            editor.apply();

            showSignInButtons();

            startActivity(new Intent(getApplicationContext(), MeetingListActivity.class));
        }

        @Override
        public void onTaskFailed(RestClientException e) {
            showToast("Uh oh, an unknown error occured :(");
            showSignInButtons();
        }

        @Override
        public void onExceptionRaised(Exception e) {
            showToast("An unknown error occured :(");
            showSignInButtons();
        }
    }

    /**
     * Specifies the RestClientUserCallback implementation after getting a user's data.
     */
    private class AsyncUpdateUserCallback implements RestClientUserCallback {
        @Override
        public void onTaskExecuted(User user) {
            Intent transfer = new Intent(getApplicationContext(),
                    MeetingActivity.class);
            transfer.putExtra("meeting", meeting);
            transfer.putExtra("user", user);

            startActivity(transfer);
        }

        @Override
        public void onTaskFailed(RestClientException e) {
            showToast("Unable to update user.");
        }

        @Override
        public void onExceptionRaised(Exception e) {
            showToast("Unable to update user.");
        }
    }

    /**
     * Specifies the RestClientMeetingCallback implementation after updating a meeting's data.
     */
    private class AsyncMeetingCallback implements RestClientMeetingCallback {
        @Override
        public void onTaskExecuted(final Meeting m) {
            meeting = m;
            new UpdateUserAsync(new User.UserBuilder().build(),
                    new AsyncUpdateUserCallback()).execute();
        }

        @Override
        public void onTaskFailed(RestClientException e) {
            showToast("Unable to get meeting info.");
        }

        @Override
        public void onExceptionRaised(Exception e) {
            showToast("Unable to get meeting info.");
        }
    }
}