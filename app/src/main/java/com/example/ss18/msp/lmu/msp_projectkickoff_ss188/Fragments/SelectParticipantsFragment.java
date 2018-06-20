package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.BottomNavigationItemView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities.AppLogicActivity;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionEndpoint;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Adapters.ViewerAdapter;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Voice.VoiceTransmission;

public class SelectParticipantsFragment extends Fragment {
    private static final String TAG = "SelectParticipants";
    private static View mainView;
    private static ConnectionManager connectionManager;
    private static ViewerAdapter viewerAdapter = null;
    private VoiceTransmission voiceTransmission;
    private ProgressBar progressBar;
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.fragment_participants,container,false);
        connectionManager = AppLogicActivity.getConnectionManager();
        progressBar = mainView.findViewById(R.id.progressBar);
        if(viewerAdapter == null)
            viewerAdapter = new ViewerAdapter(getContext());
        else if(viewerAdapter.getCount() > 0)
            progressBar.setVisibility(View.GONE);
        ListView listView = mainView.findViewById(R.id.viewerList);
        listView.setAdapter(viewerAdapter);
        updateParticipantsGUI(null,connectionManager.getEstablishedConnections().size(),
                connectionManager.getDiscoveredEndpoints().size());
        //Define pokeItem
        BottomNavigationItemView pokeItem = mainView.findViewById(R.id.vibrieren);
        pokeItem.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    startPoking();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    endPoking();
                }
                return true;
            }
        });
        //Define VoiceChatButton
        BottomNavigationItemView voiceChatItem = mainView.findViewById(R.id.voiceChat);
        voiceChatItem.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    startRecordingVoice();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopRecordingVoice();
                }
                return true;
            }
        });
        voiceTransmission = AppLogicActivity.getVoiceTransmission();
        //TODO: Define GPS button... @Laureen?

        return mainView;
    }
    /**
     * Updates the amount of participants on the GUI
     */
    public void updateParticipantsGUI(ConnectionEndpoint e,int newSize, int maxSize){
        TextView textView = mainView.findViewById(R.id.numberOfParticipants);
        if(maxSize == 0) {
            textView.setText(R.string.leer);
            progressBar.setVisibility(View.VISIBLE);
        }
        else {
            textView.setText(newSize + "|" + maxSize);
            progressBar.setVisibility(View.GONE);
        }
        if(newSize == 0){
            progressBar.setVisibility(View.VISIBLE);
        }
        //Update listView
        if(e == null)
            return;
        Log.i(TAG,"UpdateParticipantsGUI ID: " + e.getId());
        if(connectionManager.getEstablishedConnections().containsKey(e.getId())) {
            viewerAdapter.add(e);
        }else{
            viewerAdapter.remove(e);
        }
        viewerAdapter.notifyDataSetChanged();
    }

    /**
     * Displays options to manage (allow/deny) file sharing with devices.
     * That is selecting devices you want to enable file sharing
     * @param view
     */
    public void manageParticipants(View view){
        Log.i(TAG,"Participants button clicked");
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.selectDevices);
        final ConnectionEndpoint[] discoveredDevices = connectionManager.getDiscoveredEndpoints().values().toArray(new ConnectionEndpoint[0]);
        final boolean[] selectedDevices = new boolean[discoveredDevices.length]; //Default to be true(selected)

        //We found no device
        if(discoveredDevices.length == 0){
            builder.setMessage(R.string.noDevicesFound);
            builder.setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }//We found devices
        else {
            final String[] deviceNicknames = new String[discoveredDevices.length];
            //Assign nicknames
            for (int i = 0; i < discoveredDevices.length; i++) {
                deviceNicknames[i] = discoveredDevices[i].getName();
                if(connectionManager.getEstablishedConnections().containsKey(discoveredDevices[i].getId()))
                    selectedDevices[i] = true;
            }

            DialogInterface.OnMultiChoiceClickListener dialogInterface = new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int device,
                                    boolean isChecked) {
                    Log.i(TAG,"Device checked: " + isChecked + " | " + discoveredDevices[device].getName());
                    selectedDevices[device] = isChecked;
                }
            };

            // Specify the list array, the items to be selected by default (null for none),
            // and the listener through which to receive callbacks when items are selected
            builder.setMultiChoiceItems(deviceNicknames, selectedDevices, dialogInterface);
            // Set the action buttons
            builder.setPositiveButton(R.string.selectAll, null);
            builder.setNegativeButton(R.string.deselectAll, null);
            builder.setNeutralButton(R.string.okay, null );
        }
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Select all
                for (int i = 0; i < discoveredDevices.length; i++) {
                    dialog.getListView().setItemChecked(i, true);
                    selectedDevices[i] = true;
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Deselect all
                for (int i = 0; i < discoveredDevices.length; i++) {
                    dialog.getListView().setItemChecked(i, false);
                    selectedDevices[i] = false;
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.i(TAG,"Presenter window onDismiss()");
                for (int device = 0; device < selectedDevices.length; device++) {
                    ConnectionEndpoint endpoint = discoveredDevices[device];
                    boolean isChecked = selectedDevices[device];
                    boolean newEndpoint = !connectionManager.getEstablishedConnections().containsKey(endpoint.getId())
                            && !connectionManager.getPendingConnections().containsKey(endpoint.getId());
                    if (isChecked && newEndpoint) {
                            Log.i(TAG,"Accepting connection for " + endpoint.getName());
                        // If the user checked the item, add it to the selected items, if not already connected
                            connectionManager.getPendingConnections().put(endpoint.getId(), endpoint);
                            connectionManager.acceptConnectionIfPending(endpoint);
                    } else if(!isChecked && !newEndpoint)
                        connectionManager.disconnectFromEndpoint(endpoint.getId());
                }
            }
        });
    }

    /**
     * Starts sending voice
     */
    private void startRecordingVoice(){
        //TODO: Display something
        voiceTransmission.startRecordingVoice();
        Toast.makeText(getContext(), "Zeichnet auf...", Toast.LENGTH_SHORT).show();

    }

    /**
     * Ends recording voice
     */
    private void stopRecordingVoice(){
        //TODO: Display something
        voiceTransmission.stopRecordingVoice();
        Toast.makeText(getContext(), "Aufzeichnung beendet.", Toast.LENGTH_SHORT).show();

    }
    /**
     * Sends vibration message to viewers
     */
    private void startPoking(){
        connectionManager.getPayloadSender().sendPokeMessage();
    }

    /**
     * Sends STOP vibration message to viewers
     */
    private void endPoking(){
        connectionManager.getPayloadSender().sendStopPokingMessage();
    }
    public void updateParticipantsAvatar() {
        viewerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        endPoking();
        super.onDestroy();
    }

    public void reset() {
        viewerAdapter = null;
    }
}
