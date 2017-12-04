package edu.calbaptist.android.projectmeetings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.iid.FirebaseInstanceId;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import edu.calbaptist.android.projectmeetings.Exceptions.RestClientException;

/**
 * Created by Austin on 12/1/2017.
 */

public class MeetingCreationActivity extends AppCompatActivity{

    EditText MeetingName, MeetingObjective, length;
    DatePicker date;
    TimePicker time;
    Button submit;
    private static final String TAG = "MeetingCreationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_creation);

        MeetingName = findViewById(R.id.MeetingName);
        MeetingObjective = findViewById(R.id.MeetingObjective);
        date = findViewById(R.id.Date);
        time = findViewById(R.id.Time);
        submit = findViewById(R.id.Submit);
        length = findViewById(R.id.Length);

        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Calendar calendar = Calendar.getInstance();
                calendar.set(date.getYear(), date.getMonth(), date.getDayOfMonth(), time.getHour(), time.getMinute());
                final long millis = calendar.getTimeInMillis();
                final long mLength = Long.parseLong(length.getText().toString())*60*1000;
                createMeeting(millis, mLength);
            }
        });
    }

    private void createMeeting(final long millis, final long mLength){
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        mUser.getToken(true)
                .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                    public void onComplete(@NonNull Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            final String idToken = task.getResult().getToken();

                            SharedPreferences prefs = App.context.getSharedPreferences(
                                    "edu.calbaptist.android.projectmeetings.Account_Name",
                                    Context.MODE_PRIVATE);

                            //TODO: Add actual invitation functionality
                            ArrayList<String> invites = new ArrayList<String>(Arrays.asList(new String[]{"austin@gmail.com", "blake@gmail.com"}));

                            final Meeting meeting = new Meeting.MeetingBuilder()
                                    .setName(MeetingName.getText().toString())
                                    .setObjective(MeetingObjective.getText().toString())
                                    .setTime(millis)
                                    .setTimeLimit(mLength)
                                    .setDriveFolderId(prefs.getString("DefaultFolder", ""))
                                    .setInvites(invites)
                                    .build();

                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    RestClient.createMeeting(meeting, idToken, new Callback.RestClientMeeting() {
                                        @Override
                                        void onTaskExecuted(Meeting m) {
                                            Log.d(TAG, "onTaskExecuted: " + m.getName());
                                            switchActivity(MeetingListActivity.class);
                                        }

                                        @Override
                                        void onTaskFailed(RestClientException e) {
                                            Log.d(TAG, "onTaskFailed with " + e.getResponseCode()
                                                    + ": " + e.getJson().toString());
                                        }

                                        @Override
                                        void onExceptionRaised(Exception e) {
                                            Log.d(TAG, "onExceptionRaised: " + e.getMessage());
                                        }
                                    });
                                }
                            });

                            Log.d(TAG, "Firebase Token: " + idToken);
                            // Send token to your backend via HTTPS
                            // ...
                        } else {
                            // Handle error -> task.getException();
                        }
                    }
                });
    }
    private void switchActivity(Class activity){
        startActivity(new Intent(this, activity));
    }
}

