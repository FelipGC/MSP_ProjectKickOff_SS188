package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.AbstractConnectionService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionEndpoint;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.BaseMessage;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.ChatMessage;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.IMessageDistributionService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.JsonMessageDistributionService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.MessageDistributionBinder;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.OnMessageParsedCallback;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Messages.SystemMessage;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.OldConnection.ConnectionManager;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.IAdvertiseService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.IDiscoveryService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.NearbyAdvertiseService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.NearbyDiscoveryService;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DataBase.LocalDataBase;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.ChatFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.InboxFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Presentation.Fragments.LiveViewFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.SelectParticipantsFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.SelectPresenterFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.ShareFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.TabPageAdapter;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Presentation.Fragments.PresentationFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Users.User;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.AppContext;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.NotificationUtility;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Voice.VoiceTransmission;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

public class AppLogicActivity extends BaseActivity implements AppContext {
    /**
     * Tag for Logging/Debugging
     */
    private static final String TAG = "SECONDARY_ACTIVITY";

    /**
     * A reference to the nearby connection manager object
     */
    private static ConnectionManager connectionManager;

    private IDiscoveryService discoveryService;
    private IAdvertiseService advertiseService;
    private IMessageDistributionService distributionService;

    public IDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public IAdvertiseService getAdvertiseService() {
        return advertiseService;
    }

    public IMessageDistributionService getDistributionService() {
        return distributionService;
    }

    public AbstractConnectionService getConnectionService(){
        if(discoveryService!=null){
            return (AbstractConnectionService) discoveryService;
        }
        if(advertiseService!=null) {
            return (AbstractConnectionService) advertiseService;
        }
        return null;
    }

    /**
     * The role of the user (Presenter/Spectator)
     */
    private static User userRole;

    private SelectPresenterFragment selectPresenterFragment;
    private ShareFragment shareFragment;
    private SelectParticipantsFragment selectParticipantsFragment;
    private InboxFragment inboxFragment;
    private ChatFragment chatFragment;
    private final static VoiceTransmission voiceTransmission = new VoiceTransmission();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(R.layout.activity_app_logic);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(LocalDataBase.getUserName()); //TODO
        }

        //Get object from intent
        setUserRole((User) getIntent().getSerializableExtra("UserRole"));
        Log.i(TAG, "Secondary activity created as: " + getUserRole().getRoleType());
        //Connection
        connectionManager = ConnectionManager.getInstance(); //Singleton
        connectionManager.setUpConnectionsClient(this);

        //Set up tabs
        TabPageAdapter tabPageAdapter = new TabPageAdapter(getSupportFragmentManager());
        switch (getUserRole().getRoleType()) {

            case SPECTATOR:
                startDiscovering();
                //Add tabs for spectator
                tabPageAdapter.addFragment(selectPresenterFragment = new SelectPresenterFragment(), "Presenters");
                tabPageAdapter.addFragment(inboxFragment = new InboxFragment(), "Inbox");
                tabPageAdapter.addFragment(new LiveViewFragment(), "Live");
                tabPageAdapter.addFragment(chatFragment = new ChatFragment(), "Chat");
                break;
            case PRESENTER:
                startAdvertising();
                //Add tabs for presenter
                tabPageAdapter.addFragment(selectParticipantsFragment = new SelectParticipantsFragment(), "Participants");
                tabPageAdapter.addFragment(new PresentationFragment(), getString(R.string.presentation_tabName));
                tabPageAdapter.addFragment(shareFragment = new ShareFragment(), "Share");
                tabPageAdapter.addFragment(chatFragment = new ChatFragment(), "Chat");
                break;
            default:
                Log.e(TAG, "Role type missing!");
                return;

        }
        Intent intent = new Intent(this, JsonMessageDistributionService.class);
        bindService(intent, distributionConnection, Context.BIND_AUTO_CREATE);

        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(tabPageAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * Updates the amount of participants on the GUI
     */
    public void updateParticipantsGUI(ConnectionEndpoint e, int newSize, int maxSize) {
        selectParticipantsFragment.updateParticipantsGUI(e, newSize, maxSize);
    }

    /**
     * Updates the amount of presenters on the GUI
     */
    public void updatePresentersGUI(ConnectionEndpoint endpoint) {
        //if (selectPresenterFragment != null)
          //  selectPresenterFragment.updateDeviceList(endpoint);
    }


    /**
     * Starts a Nearby service with a given serviceID
     * @param serviceClass Class of the service that should be started
     */
    private void startNearbyService(Class<? extends AbstractConnectionService> serviceClass, ServiceConnection connection){
        stopNearbyService();
        Intent serviceIntent = new Intent(this, serviceClass);
        serviceIntent.setPackage(this.getPackageName());
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_AUTO_CREATE);
    }

    /**
     * Stops all potential Nearby-Services
     */
    private void stopNearbyService(){
        Intent serviceIntent = new Intent(this, NearbyAdvertiseService.class);
        stopService(serviceIntent);
        serviceIntent = new Intent(this, NearbyDiscoveryService.class);
        stopService(serviceIntent);
    }

    /**
     * Calls startAdvertising() on the connectionManager
     */
    private void startAdvertising() {
        Toast.makeText(this, R.string.startDiscovering, Toast.LENGTH_LONG).show();
        startNearbyService(NearbyAdvertiseService.class, advertiseConnection);
    }

    /**
     * Calls startDiscovering() on the connectionManager
     */
    private void startDiscovering() {
        Toast.makeText(this, R.string.startDiscovering, Toast.LENGTH_LONG).show();
        startNearbyService(NearbyDiscoveryService.class,discoveryConnection);
    }

    //Getters & Setters
    public static User getUserRole() {
        return userRole;
    }

    public static void setUserRole(User userRole) {
        Log.i(TAG, "User changed his role to: " + userRole.getRoleType().toString());
        AppLogicActivity.userRole = userRole;
    }

    /**
     * Displays options to manage (allow/deny) file sharing with devices.
     * That is selecting devices you want to enable file sharing
     *
     */
    public void manageParticipants(View view) {
        selectParticipantsFragment.manageParticipants(view);
    }

    /**
     * Gets executed when a presentor presses to "select file" button inside the fragment_share
     */
    public void selectFileButtonClicked(View view) {
        if (shareFragment == null)
            return;
        shareFragment.performFileSearch();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy() -> terminating nearby connection");
        connectionManager.terminateConnection();
        if (chatFragment != null) {
            chatFragment.clearContent();
        }
        super.onDestroy();
    }
    //Getters and Setters

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public InboxFragment getInboxFragment() {
        return inboxFragment;
    }

    public ChatFragment getChatFragment() {
        return chatFragment;
    }

    public SelectPresenterFragment getSelectPresenterFragment() {
        return selectPresenterFragment;
    }

    public SelectParticipantsFragment getSelectParticipantsFragment() {
        return selectParticipantsFragment;
    }

    public static VoiceTransmission getVoiceTransmission() {
        return voiceTransmission;
    }

    @Override
    public void displayShortMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private ServiceConnection discoveryConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "DISCOVERY SERVICE CONNECTED");
            NearbyDiscoveryService.NearbyDiscoveryBinder binder = (NearbyDiscoveryService.NearbyDiscoveryBinder)service;
            discoveryService = binder.getService();
            discoveryService.register(new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String s, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                    Log.i(TAG, "Endpunkt gefunden");
                    selectPresenterFragment.updatePresenterLists();
                }

                @Override
                public void onEndpointLost(@NonNull String s) {
                    Log.i(TAG, "EndpointLost");
                    selectPresenterFragment.updatePresenterLists();
                }
            });
            discoveryService.register(new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
                    selectPresenterFragment.updatePendingButton();
                }

                @Override
                public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
                    selectPresenterFragment.updatePresenterLists();
                }

                @Override
                public void onDisconnected(@NonNull String s) {
                    selectPresenterFragment.updatePresenterLists();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "DISCOVERY SERVICE DISCONNECTED");
        }
    };
    private ServiceConnection advertiseConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "ADVERTISE SERVICE CONNECTED");
            NearbyAdvertiseService.NearbyAdvertiseBinder binder = (NearbyAdvertiseService.NearbyAdvertiseBinder)service;
            advertiseService = binder.getService();
            advertiseService.register(new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String s, @NonNull ConnectionInfo connectionInfo) {
                    Log.i(TAG,"onConnectionInitiated");
                    selectParticipantsFragment.updateParticipants();
                }

                @Override
                public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
                    switch (connectionResolution.getStatus().getStatusCode()){
                        case ConnectionsStatusCodes.STATUS_OK:
                            String name = advertiseService.getNameOfEndpoint(s);
                            if(name != null){
                                advertiseService.broadcastMessage((new SystemMessage(getString(R.string.chat_user_connected,name))).toJsonString());
                            }
                            break;
                    }
                    selectParticipantsFragment.updateParticipants();
                }

                @Override
                public void onDisconnected(@NonNull String s) {
                    // does not work for now
                    // presenter does not know name of leaving endpoint
                    /*String name = advertiseService.getNameOfEndpoint(s);
                    if(name != null) {
                        advertiseService.broadcastMessage((new SystemMessage(getResources().getString(R.string.chat_user_disconnected, name))).toJsonString());
                    }*/
                    selectParticipantsFragment.updateParticipants();
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "ADVERTISE SERVICE DISCONNECTED");
        }
    };
    private ServiceConnection distributionConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MessageDistributionBinder binder = (MessageDistributionBinder) service;
            distributionService = binder.getService();
            distributionService.register(new OnMessageParsedCallback() {
                @Override
                public void onMessageParsed(@NonNull BaseMessage message) {
                    if(message.getClass() == ChatMessage.class){
                        chatFragment.addReceivedMessage((ChatMessage) message);
                        NotificationUtility.displayChatNotification(AppLogicActivity.this,((ChatMessage) message).getSender());
                    }else if(message.getClass() == SystemMessage.class){
                        chatFragment.displaySystemNotification((SystemMessage)message);
                    }
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
}
