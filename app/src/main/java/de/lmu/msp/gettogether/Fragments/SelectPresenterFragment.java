package de.lmu.msp.gettogether.Fragments;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;

import de.lmu.msp.gettogether.Adapters.PresenterAdapter;
import de.lmu.msp.gettogether.Connection.ConnectionEndpoint;
import de.lmu.msp.gettogether.Connection.ConnectionManager;
import de.lmu.msp.gettogether.R;

import static de.lmu.msp.gettogether.Connection.ConnectionManager.getAppLogicActivity;

public class SelectPresenterFragment extends Fragment {
    private static final String TAG = "SelectPresenter";
    private ListView availablePresenters;
    private ListView establishedPresenters;
    private static ListAdapter availAdapter = null;
    private static ListAdapter estAdapter = null;
    private Button pendingButton;
    private TextView joinedTitle;
    private TextView availableTitle;
    private static ConnectionManager cM;
    private ProgressBar progressBar;

    /**
     * Views to display when at least on endpoint is found
     */
    private HashSet<View> viewDevicesFound = new HashSet<>();
    /**
     * Views to display when no endpoint is found
     */
    private HashSet<View> viewNoDevices = new HashSet<>();
    private boolean connected = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
            Log.i(TAG,name +"SERVICE DISCCONECTED");
            if(getAppLogicActivity() != null)
                getAppLogicActivity().serviceConnections.remove(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ConnectionManager.ConnectionManagerBinder myBinder = (ConnectionManager.ConnectionManagerBinder) service;
            cM = myBinder.getService();
            connected = true;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_available_presenters, container, false);

        if (!connected) {
            Intent intent = new Intent(getAppLogicActivity(), ConnectionManager.class);
            getAppLogicActivity().bindService(intent, mServiceConnection, getAppLogicActivity().BIND_AUTO_CREATE);
            getAppLogicActivity().serviceConnections.add(mServiceConnection);
        }

        viewDevicesFound.addAll(Arrays.asList(
                availablePresenters = view.findViewById(R.id.presentersListView_available),
                establishedPresenters = view.findViewById(R.id.presentersListView_joined),
                availableTitle = view.findViewById(R.id.presentersListViewTitle_available),
                joinedTitle = view.findViewById(R.id.presentersListViewTitle_established),
                pendingButton = view.findViewById(R.id.presentersListViewTitle_pending)));
        viewNoDevices.addAll(Arrays.asList(view.findViewById(R.id.noDevicesFound),
                view.findViewById(R.id.hint01)));
        progressBar = view.findViewById(R.id.progressBar01);


        //Set adapters
        if (availAdapter == null)
            availAdapter = new PresenterAdapter(getContext(), false);
        if (estAdapter == null)
            estAdapter = new PresenterAdapter(getContext(), true);
        else if (estAdapter.getCount() > 0)
            progressBar.setVisibility(View.GONE);
        availablePresenters.setAdapter(availAdapter);
        establishedPresenters.setAdapter(estAdapter);

        //Set up clickListeners for the individual items and lists
        //On Click: Displays list of pending connections as a dialog
        pendingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "setOnClickListener() " + cM.getPendingConnections().toString());
                final ConnectionEndpoint[] endps = cM.getPendingConnections().values().toArray(new ConnectionEndpoint[0]);
                if (endps.length == 0)
                    return;
                final String[] deviceNicknames = new String[endps.length];
                //Assign nicknames
                for (int i = 0; i < deviceNicknames.length; i++) {
                    deviceNicknames[i] = endps[i].getName();
                }
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.pending_devices);
                builder.setItems(deviceNicknames, null);
                builder.create().show();
            }
        });

        //Hide GUI we do not want
        if(estAdapter.getCount()+estAdapter.getCount() == 0) {
            for (View view_it : viewNoDevices)
                view_it.setVisibility(View.VISIBLE);
        }
        for (View view_it : viewDevicesFound)
            view_it.setVisibility(View.GONE);
        if (availAdapter.getCount() > 0) {
            availableTitle.setVisibility(View.VISIBLE);
            availablePresenters.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
        if (estAdapter.getCount() > 0) {
            joinedTitle.setVisibility(View.VISIBLE);
            establishedPresenters.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }

        updateJoinedPresentersAvatar();

        return view;
    }

    /**
     * Removes an endPoint from the adapters (for example when he disconnects)
     */
    public void removeEndpointFromAdapters(ConnectionEndpoint connectionEndpoint) {
        Log.i(TAG, "REMOVE ENDPOINT FROM ADAPTERS");
        ((PresenterAdapter) availablePresenters.getAdapter()).remove(connectionEndpoint);
        ((PresenterAdapter) establishedPresenters.getAdapter()).remove(connectionEndpoint);
        if (cM == null || cM.getPendingConnections().size() == 0) {
            pendingButton.setVisibility(View.GONE);
        } else{
            pendingButton.setText(getString(R.string.requests_pending_count,cM.getPendingConnections().size()));
        }
    }

    /**
     * Updates the list view displaying all devices an advertiser can connect/
     * or has already connected to
     */
    public synchronized void updateDeviceList(ConnectionEndpoint endpoint) {
        Log.i(TAG, "updateDeviceList( " + endpoint + " )");
        //We found no device
        if (cM == null || cM.getDiscoveredEndpoints().size() == 0) {
            for (View view : viewDevicesFound)
                view.setVisibility(View.GONE);
            for (View viewNoDevice : viewNoDevices)
                viewNoDevice.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
        }//We found devices
        else {
            //Hide GUI we do not want
            for (View view : viewNoDevices)
                view.setVisibility(View.GONE);
            for (View view : viewDevicesFound)
                view.setVisibility(View.VISIBLE);
            //Update lists
            updateListViews(endpoint);
            //Progress bar
            if (estAdapter.getCount() > 0)
                progressBar.setVisibility(View.GONE);
            else if (availAdapter.getCount() > 0) {
                progressBar.getIndeterminateDrawable()
                        .setColorFilter(ContextCompat.getColor(getContext(), R.color.greenAccent), PorterDuff.Mode.SRC_IN);
            } else {
                progressBar.getIndeterminateDrawable()
                        .setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_IN);
            }
        }
    }

    /**
     * Removes and endpoint from all listviews but in our specified one, where the endpoint will
     * be added
     */
    private void updateListViews(ConnectionEndpoint endpoint) {
        ListView targetListView = null;
        if(cM == null || endpoint == null)
            return;
        else if (cM.getEstablishedConnections().containsKey(endpoint.getId()))
            targetListView = establishedPresenters;
        else if (!cM.getPendingConnections().containsKey(endpoint.getId()))
            targetListView = availablePresenters;
        //Add or replace element form listView
        HashSet<ListView> listViews = new HashSet<>(Arrays.asList(establishedPresenters, availablePresenters, null));
        for (ListView listView : listViews) {
            PresenterAdapter presenterAdapter = null;
            if (listView != null)
                presenterAdapter = (PresenterAdapter) listView.getAdapter();
            //Rename is necessary
            final String displayName = endpoint.getName();
            //Update listView
            if (listView == targetListView) {
                //Add endpoint to list
                if (presenterAdapter != null) {
                    presenterAdapter.add(endpoint);
                    Log.i(TAG, "Added to list: " + displayName);
                }
            } else if (listView != null) {
                //Remove endpoint from list
                presenterAdapter.remove(endpoint);
                //Hide if empty
                if (presenterAdapter.getCount() == 0) {
                    listView.setVisibility(View.GONE);
                    if (listView == availablePresenters)
                        availableTitle.setVisibility(View.GONE);
                    else if (listView == establishedPresenters)
                        joinedTitle.setVisibility(View.GONE);
                }
            }
            if (cM.getPendingConnections().size() == 0)
                pendingButton.setVisibility(View.GONE);
            else {
                pendingButton.setText(getString(R.string.requests_pending_count,cM.getPendingConnections().size()));
            }
        }
    }

    public void updateJoinedPresentersAvatar() {
        ((PresenterAdapter) establishedPresenters.getAdapter()).notifyDataSetChanged();
    }

    public void reset() {
        estAdapter = null;
        availAdapter = null;
    }
}
