package edu.calbaptist.android.projectmeetings.utils.rest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.calbaptist.android.projectmeetings.App;
import edu.calbaptist.android.projectmeetings.R;
import edu.calbaptist.android.projectmeetings.exceptions.RestClientException;
import edu.calbaptist.android.projectmeetings.models.Meeting;
import edu.calbaptist.android.projectmeetings.models.User;

/**
 *  Rest Client
 *  Interfaces with the Rest API.
 *
 *  @author Caleb Solorio
 *  @version 0.4.0 11/27/17
 */
public class RestClient {
    public static final String TAG = "RestClient";
    public static final String PROTOCOL = App.CONTEXT.getString(R.string.rest_protocol);
    public static final String HOST = App.CONTEXT.getString(R.string.http_endpoint);
    public static final int PORT = App.CONTEXT.getResources().getInteger(R.integer.endpoint_port);

    /**
     * Creates a new User in Firebase.
     * @param user The user to create.
     * @param callback Executes after task is finished, ideally returns a User object.
     */
    public static void createUser(final User user, RestClientUserCallback callback) {
        String displayName = user.getDisplayName();
        String email = user.getEmail();
        String firebaseToken = user.getFirebaseToken();
        String googleToken = user.getGoogleToken();
        String instanceId = user.getInstanceId();

        if(isEmpty(displayName)|| isEmpty(email) || isEmpty(firebaseToken) ||
                isEmpty(googleToken) || isEmpty(instanceId)) {
            throw new IllegalArgumentException("Must provide displayName, " +
                    "firebaseToken, googleToken, and instanceId.");
        }

        try {
            JSONObject json = new JSONObject();
            json.put("display_name", displayName);
            json.put("email", email);
            json.put("firebase_token", firebaseToken);
            json.put("google_token", googleToken);
            json.put("instance_id", instanceId);

            Log.d(TAG, "createUser: JSON " + json.toString());

            callback.onTaskExecuted(putUser(json));
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Updates a User in Firebase.
     * @param user The user to update.
     * @param callback Executes after task is finished, ideally returns a User object.
     */
    public static void updateUser(User user, String token, RestClientUserCallback callback) {
        String displayName = user.getDisplayName();
        String email = user.getEmail();
        String firebaseToken = user.getFirebaseToken();
        String googleToken = user.getGoogleToken();
        String instanceId = user.getInstanceId();

        try {
            JSONObject json = new JSONObject();

            if(!isEmpty(displayName)) {
                json.put("display_name", displayName);
            }

            if(!isEmpty(email)) {
                json.put("email", email);
            }

            if(!isEmpty(firebaseToken)) {
                json.put("firebase_token", firebaseToken);
            }

            if(!isEmpty(googleToken)) {
                json.put("google_token", googleToken);

            }

            if(!isEmpty(instanceId)) {
                json.put("instance_id", instanceId);
            }

             callback.onTaskExecuted(putUser(json, token));
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Initiates a PUT request to be sent to the back end.
     */
    private static User putUser(JSONObject json)
            throws IOException, JSONException, RestClientException {
        URL url = new URL(PROTOCOL, HOST, PORT,  "/api/user/");
        HttpURLConnection conn = getBasicHttpURLConnection(url);

        return putUser(json, conn);
    }

    /**
     * Initiates a PUT request to be sent to the back end.
     */
    private static User putUser(JSONObject json, String token)
            throws IOException, JSONException, RestClientException {
        URL url = new URL(PROTOCOL, HOST, PORT,  "/api/user");
        HttpURLConnection conn = getBasicHttpURLConnection(url);
        conn.setRequestProperty("token", token);

        return putUser(json, conn);
    }

    /**
     * Initiates a PUT request to be sent to the back end.
     */
    private static User putUser(JSONObject json, HttpURLConnection conn)
            throws IOException, JSONException, RestClientException {
        conn.setRequestMethod("PUT");
        JSONObject data = jsonFromRequest(json, conn);

        return jsonToUser(data);
    }

    /**
     * Get a user by their uid.
     * @param uid The user's specific id.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a User object.
     */
    public static void getUserByUid(String uid, String token, RestClientUserCallback callback) {
        try {
            User user = getUser(new URL(PROTOCOL, HOST, PORT,  "/api/user/uid/" + uid), token);
            callback.onTaskExecuted(user);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Get a user by their email.
     * @param email The user's specific email.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a User object.
     */
    public static User getUserByEmail(String email, String token, RestClientUserCallback callback) {
        try {
            User user = getUser(new URL(PROTOCOL, HOST, PORT,  "/api/user/email/" + email), token);
            callback.onTaskExecuted(user);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }

        return null;
    }

    /**
     * Initiates a GET request for a user.
     */
    private static User getUser(URL url, String token)
            throws IOException, JSONException, RestClientException {
        HttpURLConnection conn = getBasicHttpURLConnection(url);
        conn.setRequestProperty("token", token);
        JSONObject data = jsonFromRequest(conn);

        return jsonToUser(data);
    }

    /**
     * Given a valid JSONObject, transform the object into a user.
     */
    private static User jsonToUser(JSONObject json) throws JSONException{
        User.UserBuilder builder = new User.UserBuilder()
                .setUid(json.getString("u_id"))
                .setEmail(json.getString("email"))
                .setDisplayName(json.getString("display_name"));

        if(json.has("firebase_token")) {
            builder = builder
                    .setFirebaseToken(json.getString("firebase_token"))
                    .setGoogleToken(json.getString("google_token"))
                    .setInstanceId(json.getString("instance_id"));
        }

        if(json.has("invites")) {
            Iterator inviteIterator = json.getJSONObject("invites").keys();
            ArrayList<String> inviteList = new ArrayList<>();

            while(inviteIterator.hasNext()) {
                inviteList.add((String) inviteIterator.next());
            }

            builder = builder.setInvites(inviteList);
        }

        if(json.has("meetings")) {
            Iterator meetingIterator = json.getJSONObject("meetings").keys();
            ArrayList<String> meetingList = new ArrayList<>();

            while(meetingIterator.hasNext()) {
                meetingList.add((String) meetingIterator.next());
            }

            builder = builder.setMeetings(meetingList);
        }

        return builder.build();
    }

    /**
     *
     * @param mid The id of the meeting.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a JSON object signaling a successful deletion.
     */
    public static void deleteUserInvite(String mid, String token, RestClientJsonCallback callback) {
        try {
            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/user/invite/" + mid);

            HttpURLConnection conn = ((HttpURLConnection) url.openConnection());
            conn.setRequestMethod("DELETE");
            conn.setDoInput(true);
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(conn);
            callback.onTaskExecuted(data);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Creates a new Meeting in Firebase.
     * @param meeting The meeting to create.
     * @param callback Executes after task is finished, ideally returns a Meeting object.
     */
    public static void createMeeting(Meeting meeting, String token, RestClientMeetingCallback callback) {
        String name = meeting.getName();
        String objective = meeting.getObjective();
        long time = meeting.getTime();
        long timeLimit = meeting.getTimeLimit();
        String driveFolderId = meeting.getDriveFolderId();
        ArrayList<String> invites = meeting.getInvites();

        if(isEmpty(name) || isEmpty(objective) ||
                isEmpty(driveFolderId) || time <= 0 || timeLimit <= 0) {
            callback.onExceptionRaised(new IllegalArgumentException("Must provide mid, uid, name, "
                    + "objective, time, timeLimit, driveFolderId, and add_invites."));
        }

        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("objective", objective);
            json.put("time", time);
            json.put("time_limit", timeLimit);
            json.put("drive_folder_id", driveFolderId);
            json.put("invites", new JSONArray(invites));

            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting");

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(json, conn);
            Meeting m = jsonToMeeting(data);

            callback.onTaskExecuted(m);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Updates a Meeting in Firebase.
     * @param meeting The meeting to update.
     * @param callback Executes after task is finished, ideally returns a Meeting object.
     */
    public static void updateMeeting(Meeting meeting, String token, RestClientMeetingCallback callback) {
        String name = meeting.getName();
        String objective = meeting.getObjective();
        long time = meeting.getTime();
        long timeLimit = meeting.getTimeLimit();
        String driveFolderId = meeting.getDriveFolderId();

        try {
            JSONObject json = new JSONObject();

            if(!isEmpty(name)) {
                json.put("name", name);
            }

            if(!isEmpty(objective)) {
                json.put("objective", objective);
            }

            if (time > 0) {
                json.put("time", time);
            }

            if (timeLimit > 0) {
                json.put("time_limit", timeLimit);
            }

            if(!isEmpty(driveFolderId)) {
                json.put("drive_folder_id", driveFolderId);
            }

            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting/" + meeting.getMId());

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(json, conn);
            Meeting m = jsonToMeeting(data);

            callback.onTaskExecuted(m);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Get a meeting by their mid.
     * @param mid The meeting's specific id.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a Meeting object.
     */
    public static void getMeeting(String mid, String token, RestClientMeetingCallback callback) {
        try {
            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting/" + mid);

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(conn);
            Meeting m = jsonToMeeting(data);
            callback.onTaskExecuted(m);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Deletes the specified meeting from Firebase.
     * @param mid The meeting's specific id.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a JSON object signaling a successful deletion.
     */
    public static void deleteMeeting(String mid, String token, RestClientJsonCallback callback) {
        try {
            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting/" + mid);

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(conn);
            callback.onTaskExecuted(data);
        } catch (ProtocolException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Invites the specified users to the specified meeting.
     * @param mid The meeting's specific id.
     * @param emails The list of emails whose respective users should be invited.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a JSON object signaling success.
     */
    public static void inviteToMeeting(String mid, ArrayList<String> emails,
                                       String token, RestClientJsonCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("emails", new JSONArray(emails));

            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting/" + mid + "/invites");

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(json, conn);
            callback.onTaskExecuted(data);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Uninvites the specified users to the specified meeting.
     * @param mid The meeting's specific id.
     * @param emails The list of emails whose respective users should be uninvited.
     * @param token The client's authorization token.
     * @param callback Executes after task is finished, ideally returns a JSON object signaling a success.
     */
    public static void uninviteFromMeeting(String mid, ArrayList<String> emails,
                                           String token, RestClientJsonCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("emails", new JSONArray(emails));

            URL url = new URL(PROTOCOL, HOST, PORT,  "/api/meeting/" + mid + "/invites");

            HttpURLConnection conn = getBasicHttpURLConnection(url);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("token", token);

            JSONObject data = jsonFromRequest(json, conn);
            callback.onTaskExecuted(data);
        } catch (IOException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onExceptionRaised(e);
        } catch (RestClientException e) {
            e.printStackTrace();
            callback.onTaskFailed(e);
        }
    }

    /**
     * Constructs a basic HttpURLConnection object.
     */
    private static HttpURLConnection getBasicHttpURLConnection(URL url) throws IOException {
        HttpURLConnection conn = ((HttpURLConnection) url.openConnection());
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);

        return conn;
    }

    /**
     * Maps return data from a JSON request to an JSONObject.
     */
    private static JSONObject jsonFromRequest(JSONObject json, HttpURLConnection conn)
            throws IOException, JSONException, RestClientException {
        OutputStream os = conn.getOutputStream();
        os.write(json.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        return jsonFromRequest(conn);
    }

    /**
     * Maps return data from a JSON request to an JSONObject.
     */
    private static JSONObject jsonFromRequest(HttpURLConnection conn)
            throws IOException, JSONException, RestClientException {
        int responseCode = conn.getResponseCode();

        if(responseCode != 200) {
            InputStream stream = conn.getErrorStream();
            StringBuilder builder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }
                in.close();
            }

            JSONObject json =  new JSONObject(builder.toString());
            throw new RestClientException(responseCode, json);
        }

        BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

        StringBuffer returnJson = new StringBuffer(1024);
        String tmp = "";
        while((tmp = bufferedReader.readLine())!= null)
            returnJson.append(tmp).append("\n");
        bufferedReader.close();

        try {
            return new JSONObject(returnJson.toString());
        } catch (JSONException e) {
            return new JSONObject("{ \"message\": \""
                    + returnJson.toString() + "\" }");
        }
    }

    /**
     * Given a valid JSONObject, map the JSON data to a Meeting object.
     */
    private static Meeting jsonToMeeting(JSONObject json) throws JSONException{
        Meeting.MeetingBuilder builder = new Meeting.MeetingBuilder()
                .setMId(json.getString("m_id"))
                .setUId(json.getString("u_id"))
                .setName(json.getString("name"))
                .setObjective(json.getString("objective"))
                .setTime(json.getLong("time"))
                .setTimeLimit(json.getLong("time_limit"))
                .setDriveFolderId(json.getString("drive_folder_id"));

        if(json.has("add_invites")) {
            builder = builder.setInvites((ArrayList) keysFromJsonString(json.getString("add_invites")));
        }

        return builder.build();
    }

    /**
     * Given a String that can be mapped to a JSONObject, return a list of the keys in the object.
     */
    private static List<String> keysFromJsonString(String json) throws JSONException {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = new JSONObject(json).keys();

        while(iterator.hasNext()) {
            keys.add(iterator.next());
        }

        return keys;
    }

    /**
     * Determines if a string is empty.
     */
    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
