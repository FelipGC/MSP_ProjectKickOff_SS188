package de.lmu.msp.gettogether.Adapters;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import de.lmu.msp.gettogether.Connection.ConnectionEndpoint;
import de.lmu.msp.gettogether.Connection.ConnectionManager;
import de.lmu.msp.gettogether.R;

import static de.lmu.msp.gettogether.Connection.ConnectionManager.getAppLogicActivity;

/**
 * Adapter for storing/handling (available) presenters for the "SelectPresenterFragment"
 */
public class PresenterAdapter extends BaseAdapter {

    private boolean enableSwitch;
    private final String TAG = "PresenterAdapter";
    private final ArrayList<ConnectionEndpoint> endpointList = new ArrayList<ConnectionEndpoint>();
    private Context context;

    private static ConnectionManager cM;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,name +"SERVICE DISCCONECTED");
            if(getAppLogicActivity() != null)
                getAppLogicActivity().serviceConnections.remove(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectionManager.ConnectionManagerBinder myBinder = (ConnectionManager.ConnectionManagerBinder) service;
            cM = myBinder.getService();
        }
    };


    public PresenterAdapter(Context context, boolean enableSwitch) {
        this.context = context;
        this.enableSwitch = enableSwitch;
        Intent intent = new Intent(getAppLogicActivity(), ConnectionManager.class);
        getAppLogicActivity().bindService(intent, mServiceConnection, getAppLogicActivity().BIND_AUTO_CREATE);
        getAppLogicActivity().serviceConnections.add(mServiceConnection);
    }

    public boolean contains(String id) {
        for (ConnectionEndpoint e : endpointList) {
            if (e.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public void add(ConnectionEndpoint endpoint) {
        //Add the new endpoint
        this.endpointList.add(endpoint);
        Log.i(TAG, "Added to PresenterAdapter: " + endpoint.getName() + " with id: " + endpoint.getId());
        notifyDataSetChanged(); // to render the list we need to notify
    }

    /**
     * Removes an ConnectionEndpoint from the list if it exists
     *
     * @param endpoint The endpoint to replace
     */
    public void remove(ConnectionEndpoint endpoint) {
        Log.i(TAG, "Remove from PresenterAdapter: " + endpoint.getName() + " with id: " + endpoint.getId());
        for (ConnectionEndpoint e : endpointList) {
            if (e.getId().equals(endpoint.getId())) {
                this.endpointList.remove(e);
                break;
            }
        }
        notifyDataSetChanged(); // to render the list we need to notify
    }

    @Override
    public int getCount() {
        return endpointList.size();
    }

    @Override
    public Object getItem(int position) {
        return endpointList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Creates a presenter entry
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView()");
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        final ConnectionEndpoint connectionEndpoint = endpointList.get(position);
        convertView = messageInflater.inflate(R.layout.list_item_presenter, null);
        Switch sw = convertView.findViewById(R.id.switch1);
        sw.setChecked(enableSwitch);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) clickedItemInEstablished(connectionEndpoint);
                else clickedItemInAvailable(connectionEndpoint);
            }
        });
        TextView name = (TextView) convertView.findViewById(R.id.name);
        ImageView picture = (ImageView) convertView.findViewById(R.id.avatar);
        name.setText(connectionEndpoint.getName());
        picture.setImageURI(connectionEndpoint.getProfilePicture());
        convertView.setTag(connectionEndpoint);
        return convertView;
    }

    private void clickedItemInAvailable(final ConnectionEndpoint endpoint) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(context.getResources().getString(R.string.join_group));
        dialog.setMessage(context.getResources().getString(R.string.wait_for_accept, endpoint.getName()));
        dialog.setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = dialog.create();
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //OnClick: Add to pending list
                Toast.makeText(context,
                        context.getResources().getString(R.string.request_sent, endpoint.getName()),
                        Toast.LENGTH_SHORT).show();
                cM.getPendingConnections().put(endpoint.getId(), endpoint);
                cM.requestConnection(endpoint);
                cM.updatePresenters(endpoint);
            }
        });
        alertDialog.show();
    }

    /**
     * Displays a dialog after for asking the user if he really wants to unsubscribe and
     * disconnect from a presenter (endpoint).
     */
    private void clickedItemInEstablished(final ConnectionEndpoint connectionEndpoint) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.leave_presentation_title);
        dialog.setMessage(context.getResources().getString(R.string.leave_presentation_body,
                connectionEndpoint.getName()));
        dialog.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Click and ticked
                dialog.dismiss();
            }
        });
        dialog.setPositiveButton(R.string.leave, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Disconnect from endpoint
                cM.disconnectFromEndpoint(connectionEndpoint.getId());
                cM.updatePresenters(connectionEndpoint);
            }
        });

        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }
}
