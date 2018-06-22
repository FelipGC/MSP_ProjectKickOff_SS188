package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities.AppLogicActivity;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DataBase.LocalDataBase;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DistanceControl.CheckDistanceService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.ChatFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Users.User;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.FixedSizeList;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.NotificationUtility;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager.getAppLogicActivity;
import static com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.Constants.MAX_GPS_DISTANCE;

public final class PayloadReceiver extends PayloadCallback {

    private final String TAG = "PayloadReceiver";
    private static final FixedSizeList fixedSizeList = new FixedSizeList();
    //SimpleArrayMap is a more efficient data structure when lots of changes occur (in comparision to hash map)
    private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();
    private AppLogicActivity appLogicActivity;

    PayloadReceiver() {
        Log.i(TAG, "new PayloadReceiver()");
        appLogicActivity = AppLogicActivity.getInstance();
    }

    //Note: onPayloadReceived() is called when the first byte of a Payload is received;
    //it does not indicate that the entire Payload has been received.
    //The completion of the transfer is indicated when onPayloadTransferUpdate() is called with a status of PayloadTransferUpdate.Status.SUCCESS
    @Override
    public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
        //We will be receiving data
        Log.i(TAG, String.format("onPayloadReceived(endpointId=%s, payload=%s)", endpointId, payload));
        if (payload.getType() == Payload.Type.BYTES) {
            String payloadFilenameMessage = null;

            try {
                payloadFilenameMessage = new String(payload.asBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //Extracts the payloadId and filename from the message and stores it in the
            //filePayloadFilenames map. The format is payloadId:filename.
            Log.i(TAG, "Received string: " + payloadFilenameMessage);
            try {
                int substringDividerIndex = payloadFilenameMessage.indexOf(':');
                String payloadId = payloadFilenameMessage.substring(0, substringDividerIndex);
                String fileContent = payloadFilenameMessage.substring(substringDividerIndex + 1);
                //We must check whether we are receiving a file name (in order to rename a file)
                //or a chat message
                switch (payloadId) {
                    case "CHAT":
                        //If we already received it quit
                        if (fixedSizeList.contains(payload.getId()))
                            return;
                        Log.i(TAG, "Received CHAT" + fileContent);
                        fixedSizeList.add(payload.getId());
                        //We have a new chat message
                        onChatMessageReceived(endpointId, fileContent);
                        //Broadcast chat message to all if presenter
                        if (AppLogicActivity.getUserRole().getRoleType() == User.UserRole.PRESENTER)
                            appLogicActivity.getmService().broadcastMessage(String.valueOf(payload.asBytes()));
                        break;
                    case "POKE":
                        Log.i(TAG, "Received POKE" + fileContent);
                        if (fileContent.equals("S"))
                            NotificationUtility.startVibration();
                        else
                            NotificationUtility.endVibration();
                        break;
                    case "DISTANCE":
                        Log.i(TAG, "Received DISTANCE " + fileContent);
                        float distance = Float.parseFloat(fileContent);
                        if(distance > MAX_GPS_DISTANCE) {
                            onDistanceWarningReceived(endpointId, fileContent);
                        }
                        //Update location
                        for (ConnectionEndpoint connectionEndpoint : appLogicActivity.getmService().getConnectedEndpoints()) {
                            connectionEndpoint.setLastKnownDistance(fileContent + "m");
                        }
                        break;
                    case "LOCATION":
                        String[] coords = fileContent.split("/");
                        float latitude = Float.parseFloat(coords[0]);
                        float longitude = Float.parseFloat(coords[1]);
                        Location location = new Location("unknown");
                        location.setLongitude(longitude);
                        location.setLatitude(latitude);
                        onLocationReceived(location);
                        break;
                    default:
                        Log.i(TAG, "Received FILE-NAME: " + fileContent + " PayloadID=" + payloadId);
                        filePayloadFilenames.put(Long.valueOf(payloadId), fileContent);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (payload.getType() == Payload.Type.FILE) {
            Log.i(TAG, "Received FILE: ID=" + payload.getId());
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingPayloads.put(payload.getId(), payload);
            //TODO: Sending files may take some time. Display progressbar or something
        } else if (payload.getType() == Payload.Type.STREAM) {
            Log.i(TAG, "Received STREAM: ID=" + payload.getId());
            //We received a stream. i.e Voice stream
            receivedVoiceStream(payload.asStream().asInputStream());
        }
    }

    @Override
    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
            //Data fully received.
            Log.i(TAG, "Payload data fully received! ID=" + endpointId);
            //Display a notification.
            //Checks to see if the message is a chat message or a document
            Payload payload = incomingPayloads.remove(update.getPayloadId());
            Log.i(TAG, "onPayloadTransferUpdate() Incoming payload: " + payload);
            if (payload != null) {
                //Load data
                if (payload.getType() == Payload.Type.FILE) {
                    receivedFileParser(payload, update, endpointId);
                } else Log.i(TAG, "Payload received is not Type.FILE, but: " + payload.getType());
            }
        } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
            Log.i(TAG, "Payload status: PayloadTransferUpdate.Status.FAILURE");
        }
    }
    /*
     * Sends the received message from the endpoint to the device
     */

    private void onChatMessageReceived(String id, String message) {
        Log.i(TAG, "RECEIVED CHAT MESSAGES" + message);
        //Display notification
        NotificationUtility.displayNotificationChat("Chat message received",
                String.format("%s has sent you a message...", LocalDataBase.otherUsers.get(id).getName()),
                NotificationCompat.PRIORITY_DEFAULT);
        ChatFragment chat = appLogicActivity.getChatFragment();
        chat.getDataFromEndPoint(id, message);
    }

    private void onLocationReceived(Location receivedLocation){
        Intent intent = new Intent(appLogicActivity, CheckDistanceService.class);
        intent.putExtra("location", receivedLocation);
        appLogicActivity.startService(intent);
    }

    private void onDistanceWarningReceived(String senderId, String distance) {
        String senderName = LocalDataBase.otherUsers.get(senderId).getName();
        NotificationUtility.displayNotification("Entfernungswarnung",
                String.format("Teilnehmer %s ist %s Meter entfernt.",senderName,distance),
                NotificationCompat.PRIORITY_DEFAULT);
    }

    /**
     * Implements the actions to execute after fully receiving a document.
     */
    private void receivedFileParser(Payload payload, PayloadTransferUpdate update, String endpointId) {
        // Retrieve the filename and corresponding payload.
        File payloadFile = payload.asFile().asJavaFile();
        String fileName = filePayloadFilenames.remove(update.getPayloadId());
        if (fileName != null) {
            //Did we receive a Profile Picture?
            if (fileName.contains("PROF_PIC")) {
                profilePictureReceived(fileName, payloadFile, endpointId, payload);
            }
            //Did we receive an Image?
            else if (fileName.contains("IMAGE_PIC:")) {
                receivedImageFully(payloadFile, endpointId);
            }
            //We received a document which is not an image nor an profile picture
            else {
                receivedFileFully(payloadFile, endpointId);
            }
        } else {
            Log.i(TAG, "Filename is null...");
        }

    }

    private void profilePictureReceived(String fileName, File payloadFile, String endpointId, Payload payload) {

        int substringDividerIndex = fileName.indexOf(':');
        Log.i(TAG, "Name to trim: " + fileName);
        String payLoadTag = fileName.substring(0, substringDividerIndex);
        String bitMapSender = fileName.substring(substringDividerIndex + 1);
        //Store image
        //TODO: Move and rename file to something good (NOT WORKING?)
        //Uri uriToPic = FileUtility.storePayLoadUserProfile(fileName, payloadFile);
        Uri uriToPic = Uri.fromFile(payloadFile);
        Log.i(TAG, "CONTENT URI " + uriToPic);
        //Log.i(TAG, "ORIGINAL URI " + Uri.fromFile(payloadFile));
        Log.i(TAG, "BITMAP SENDER: " + bitMapSender);
        //Add to local DataBase
        if (bitMapSender.length() == 0)
            bitMapSender = endpointId;
        //Store bitmap
        LocalDataBase.addUriToID(uriToPic, bitMapSender);
        switch (payLoadTag) {
            //Presenter received a profile picture
            case "PROF_PIC_V":
                Log.i(TAG, "<<PROF_PIC_V>>");
                try {
                    //Send bitmap to all other endpoints
                    for (ConnectionEndpoint endpoint : appLogicActivity.getmAdvertiseService().getConnectedEndpoints()) {
                        String id = endpoint.getId();
                        //Send name
                        appLogicActivity.getmService().sendMessageTo(id,payload.getId() + ":PROF_PIC:" + bitMapSender + ":");
                        //Send file associated with the name
                        appLogicActivity.getmService().sendFile(id, payload.asFile().asParcelFileDescriptor());
                    }
                    //Send all other endpoint`s bitmap to the endpoint
                    for (ConnectionEndpoint e : appLogicActivity.getmAdvertiseService().getConnectedEndpoints()) {
                        String id = e.getId();
                        Uri uri = LocalDataBase.getProfilePictureUri(id);
                        if (uri == null || e.getId().equals(bitMapSender))
                            continue;
                        ParcelFileDescriptor file = appLogicActivity.getContentResolver().openFileDescriptor(uri, "r");
                        Payload profilePic = Payload.fromFile(file);
                        //Send name
                        appLogicActivity.getmService().sendMessageTo(endpointId, payload.getId() + ":PROF_PIC:" + id + ":");
                        //Send file associated with the name
                        appLogicActivity.getmService().sendFile(endpointId, profilePic.asFile().asParcelFileDescriptor());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            //Viewer received a profile picture
            case "PROF_PIC":
                Log.i(TAG, "<<PROF_PIC>>");
                break;
        }
    }

    private void receivedImageFully(File payloadFile, String endpointId) {
        //TODO: RenameFile
        //Display a notification.
        String name = idToName(endpointId);
        for (ConnectionEndpoint e : appLogicActivity.getmAdvertiseService().getConnectedEndpoints()) {
            if (e.getId().equals(endpointId)) {
                name = e.getName();
                break;
            }
        }
        NotificationUtility.displayNotification("Image received",
                String.format("%s has sent you an image.", name),
                NotificationCompat.PRIORITY_DEFAULT);
        Log.i(TAG, "Payload file name: " + payloadFile.getName());
        //ConnectionEndpoint connectionEndpoint = cM.getDiscoveredEndpoints().get(endpointId);
        //Update inbox-fragment.
        appLogicActivity.getInboxFragment().updateInboxFragment(Uri.fromFile(payloadFile));
    }

    private void receivedFileFully(File payloadFile, String endpointId) {
        //TODO: RenameFile
        //Display a notification.
        NotificationUtility.displayNotification("Document received",
                String.format("%s has sent you a document.", idToName(endpointId)),
                NotificationCompat.PRIORITY_DEFAULT);
        Log.i(TAG, "Payload file name: " + payloadFile.getName());
    }

    private String idToName(String endpointId){
        String name = "Someone";
        for (ConnectionEndpoint e : appLogicActivity.getmAdvertiseService().getConnectedEndpoints()) {
            if (e.getId().equals(endpointId)) {
                name = e.getName();
                break;
            }
        }
        return name;
    }
    /**
     * Gets called after receiving a voice stream
     */
    private void receivedVoiceStream(InputStream inputStream) {
        //TODO: Handle simultaneous receivedVoiceStream()
        AppLogicActivity.getVoiceTransmission().playAudio(inputStream);
    }

}
