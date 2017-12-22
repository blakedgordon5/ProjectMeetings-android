package edu.calbaptist.android.projectmeetings.async.meeting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import edu.calbaptist.android.projectmeetings.App;
import edu.calbaptist.android.projectmeetings.R;
import edu.calbaptist.android.projectmeetings.models.Meeting;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClient;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClientMeetingCallback;

/**
 *  Create Meeting Async
 *  Handles updating a meeting asynchronously.
 *
 *  @author Caleb Solorio
 *  @version 1.0.0 12/18/17
 */

public class UpdateMeetingAsync extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "UpdateMeetingAsync";
    private static final SharedPreferences PREFERENCES = App.context.getSharedPreferences(
            App.context.getString(R.string.app_package),
            Context.MODE_PRIVATE);

    private Meeting meeting;
    private RestClientMeetingCallback callback;

    /**
     * The UpdateMeetingAsync constructor.
     * @param meeting The meeting to create.
     * @param callback Executes after creating the meeting.
     */
    public UpdateMeetingAsync(Meeting meeting, RestClientMeetingCallback callback) {
        this.meeting = meeting;
        this.callback = callback;
    }

    /**
     * Updates a meeting in the background.
     * @param voids Will not be called.
     * @return null
     */
    @Override
    protected Void doInBackground(Void... voids) {
        RestClient.updateMeeting(meeting,
                PREFERENCES.getString("firebase_token", null), callback);
        return null;
    }
}