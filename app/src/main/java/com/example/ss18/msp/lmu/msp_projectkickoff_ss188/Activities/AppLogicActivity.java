package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionEndpoint;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.DataBase.LocalDataBase;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.ChatFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.SelectPresenterFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.ShareFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.InboxFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.TabPageAdapter;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.SelectParticipantsFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments.LiveViewFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Presentation.PresentationFragment;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Users.User;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility.AppContext;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Voice.VoiceTransmission;

import java.util.ArrayList;

import static com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager.getAppLogicActivity;


public class AppLogicActivity extends BaseActivity implements AppContext {

    public final ArrayList<ServiceConnection> serviceConnections = new ArrayList<>();

    /**
     * Tag for Logging/Debugging
     */
    private static final String TAG = "SECONDARY_ACTIVITY";


    /**
     * The role of the user (Presenter/Spectator)
     */
    private static User userRole;

    private SelectPresenterFragment selectPresenterFragment;
    private ShareFragment shareFragment;
    private SelectParticipantsFragment selectParticipantsFragment;
    private InboxFragment inboxFragment;
    private ChatFragment chatFragment;
    private TabPageAdapter tabPageAdapter;
    private static VoiceTransmission voiceTransmission;
    private ViewPager viewPager;
    private static ConnectionManager connectionManager;
    private AppLogicActivity appLogicActivity;
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG,name +"SERVICE DISCCONECTED");
            if(getAppLogicActivity() != null)
                getAppLogicActivity().serviceConnections.remove(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG,"onServiceConnected()");
            ConnectionManager.ConnectionManagerBinder myBinder = (ConnectionManager.ConnectionManagerBinder) service;
            connectionManager = myBinder.getService();
            connectionManager.setUpConnectionsClient(appLogicActivity);
            //Set up tabs
            tabPageAdapter = new TabPageAdapter(getSupportFragmentManager());
            switch (getUserRole().getRoleType()) {

                case SPECTATOR:
                    startDiscovering();
                    //Add tabs for spectator
                    tabPageAdapter.addFragment(selectPresenterFragment = new SelectPresenterFragment(), "Gruppen");
                    tabPageAdapter.addFragment(new LiveViewFragment(), "Live");
                    tabPageAdapter.addFragment(inboxFragment = new InboxFragment(), "Bilder");
                    tabPageAdapter.addFragment(chatFragment = new ChatFragment(), "Chat");
                    selectPresenterFragment.reset();
                    break;
                case PRESENTER:
                    startAdvertising();
                    //Add tabs for presenter
                    tabPageAdapter.addFragment(selectParticipantsFragment = new SelectParticipantsFragment(), "Teilnehmer");
                    tabPageAdapter.addFragment(new PresentationFragment(), getString(R.string.presentation_tabName));
                    tabPageAdapter.addFragment(shareFragment = new ShareFragment(), "Bilder");
                    tabPageAdapter.addFragment(chatFragment = new ChatFragment(), "Chat");
                    selectParticipantsFragment.reset();
                    break;
                default:
                    Log.e(TAG, "Role type missing!");
                    return;

            }
            chatFragment.setAdapter();

            viewPager = findViewById(R.id.pager);
            viewPager.setAdapter(tabPageAdapter);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);

            voiceTransmission = new VoiceTransmission();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(R.layout.activity_app_logic);
    }

    @Override
    protected void onStart() {
        super.onStart();

        appLogicActivity = this;

        getSupportActionBar().setTitle(LocalDataBase.getUserName()); //TODO

        //Get object from intent
        setUserRole((User) getIntent().getSerializableExtra("UserRole"));
        Log.i(TAG, "Secondary activity created as: " + getUserRole().getRoleType());
        //Connection
        final Intent intent = new Intent(this, ConnectionManager.class);
        startService(intent);
        bindService(intent, mServiceConnection, this.BIND_AUTO_CREATE);
        serviceConnections.add(mServiceConnection);
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
        if (selectPresenterFragment != null)
            selectPresenterFragment.updateDeviceList(endpoint);
    }

    //Advertising and Discovery

    /**
     * Calls startAdvertising() on the connectionManager
     */
    private void startAdvertising() {
        Toast.makeText(this, R.string.startDiscovering, Toast.LENGTH_LONG).show();
        connectionManager.startAdvertising();
    }

    /**
     * Calls stopAdvertising() on the connectionManager
     */
    private void stopAdvertising() {
        connectionManager.stopAdvertising();
    }

    /**
     * Calls startDiscovering() on the connectionManager
     */
    private void startDiscovering() {
        Toast.makeText(this, R.string.startAdvertising, Toast.LENGTH_LONG).show();
        connectionManager.startDiscovering();
    }

    /**
     * Calls stopDiscovering() on the connectionManager
     */
    private void stopDiscovering() {
        connectionManager.stopDiscovering();
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
     * @param view
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
        for (ServiceConnection s: serviceConnections) {
            try {
                unbindService(s);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        serviceConnections.clear();
        connectionManager.terminateConnection();
        if (chatFragment != null) {
            chatFragment.clearContent();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getUserRole().getRoleType() == User.UserRole.PRESENTER)
            getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void displayShortMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a variaty of setttings forthe presenter conserning connection management,etc..
     */
    public void DisplayExtendedSettings(MenuItem item) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings);

        final String[] deviceNicknames = {"Autom. Verbindung",
                "Chat anonymisieren",
                "Fehlender Chatverlauf zusenden" ,
                "Fehlende Bilder zusenden",};

        DialogInterface.OnMultiChoiceClickListener dialogInterface = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index,
                                boolean isChecked) {
                LocalDataBase.connectionSettings[index] = isChecked;
            }
        };

        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
        builder.setMultiChoiceItems(deviceNicknames, LocalDataBase.connectionSettings, dialogInterface);
        // Set the action buttons
        builder.setNeutralButton(R.string.okay, null );
        builder.show();
    }

    //Getters and Setters

    public InboxFragment getInboxFragment() {
        return inboxFragment;
    }

    public ChatFragment getChatFragment() {
        return chatFragment;
    }

    public SelectPresenterFragment getSelectPresenterFragment() {
        return selectPresenterFragment;
    }

    public ShareFragment getShareFragment() {
        return shareFragment;
    }

    public SelectParticipantsFragment getSelectParticipantsFragment() {
        return selectParticipantsFragment; }

    public static VoiceTransmission getVoiceTransmission() {
        return voiceTransmission;
    }

    public ViewPager getViewPager() {
        return viewPager;
    }
}
