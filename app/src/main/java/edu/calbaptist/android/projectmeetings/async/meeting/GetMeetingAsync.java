package edu.calbaptist.android.projectmeetings.async.meeting;

import android.os.AsyncTask;

import edu.calbaptist.android.projectmeetings.App;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClientMeetingCallback;
import edu.calbaptist.android.projectmeetings.utils.rest.RestClient;

/**
 *  Get Meeting Async
 *  Handles the retrieval of a meeting asynchronously.
 *
 *  @author Caleb Solorio
 *  @version 1.0.0 12/18/17
 */

public class GetMeetingAsync extends AsyncTask<Void, Void, Void> {
    public static final String TAG = "DeleteUserInviteAsync";

    private String mId;
    private RestClientMeetingCallback mCallback;

    /**
     * The GetMeetingAsync constructor.
     * @param mId The id of the meeting to get.
     * @param callback Executes after retrieval.
     */
    public GetMeetingAsync(String mId, RestClientMeetingCallback callback) {
        this.mId = mId;
        this.mCallback = callback;
    }

    /**
     * Gets a meeting in the background.
     * @param voids Will not be called.
     * @return null
     */
    @Override
    protected Void doInBackground(Void... voids) {
        RestClient.getMeeting(mId, App.PREFERENCES.getString("firebase_token", null), mCallback);
        return null;
    }
}
