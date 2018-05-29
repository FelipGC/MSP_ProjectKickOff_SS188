package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities.AppLogicActivity;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionEndpoint;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;

public class ParticipantsFragment extends Fragment {
    private static final String TAG = "ParticipantsFragment";
    private static View mainView;
    private static ConnectionManager connectionManager;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = inflater.inflate(R.layout.participants_fragment,container,false);
        connectionManager = AppLogicActivity.getConnectionManager();
        return mainView;
    }
    /**
     * Updates the amount of participants on the GUI
     */
    public void updateParticipantsGUI(int newSize){
        TextView textView = mainView.findViewById(R.id.numberOfParticipants);
        textView.setText(newSize + "");
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
        final boolean[] devicesSelectedByDefault = null; //We may want to change that later
        final ConnectionEndpoint[] discoveredDevices = connectionManager.getDiscoveredEndpoints().values().toArray(new ConnectionEndpoint[0]);
        //We found no device
        if(discoveredDevices.length == 0){
            builder.setMessage(R.string.noDevicesFound);
            builder.setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //dialog.dismiss();
                }
            });
        }//We found devices
        else {
            final String[] deviceNicknames = new String[discoveredDevices.length];
            //Assign nicknames
            for (int i = 0; i < discoveredDevices.length; i++)
                deviceNicknames[i] = discoveredDevices[i].getName();

            DialogInterface.OnMultiChoiceClickListener dialogInterface = new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int device,
                                    boolean isChecked) {
                    if (isChecked) {
                        // If the user checked the item, add it to the selected items
                        connectionManager.getPendingConnections().put(discoveredDevices[device].getId(), discoveredDevices[device]);
                    } else if (connectionManager.getPendingConnections().containsKey(discoveredDevices[device].getId())) {
                        // Else, if the item is already in the array, remove it
                        connectionManager.getPendingConnections().remove(discoveredDevices[device].getId());
                    }
                }
            };

            // Specify the list array, the items to be selected by default (null for none),
            // and the listener through which to receive callbacks when items are selected
            builder.setMultiChoiceItems(deviceNicknames, devicesSelectedByDefault, dialogInterface);
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
                    connectionManager.getPendingConnections().put(discoveredDevices[i].getId(),discoveredDevices[i]);
                }
            }
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Deselect all
                for (int i = 0; i < discoveredDevices.length; i++){
                    dialog.getListView().setItemChecked(i, false);
                if(connectionManager.getPendingConnections().containsKey(discoveredDevices[i].getId()))
                    connectionManager.getPendingConnections().remove(discoveredDevices[i].getId());
                }
            }
        });
        //Connect to selected devices after dismissing the dialog!
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                connectionManager.requestConnectionForSelectedDevices();
            }
        });
    }
}