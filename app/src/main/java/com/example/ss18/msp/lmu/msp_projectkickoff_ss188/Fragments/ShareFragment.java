package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Activities.AppLogicActivity;
import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;
import com.google.android.gms.nearby.connection.Payload;

import java.io.FileNotFoundException;

/**
 * Class for selecting data/files and sharing them.
 * Read @see <a https://developer.android.com/guide/topics/providers/document-provider>this</a>
 * to see how it works in detail
 */
public class ShareFragment extends Fragment {
    private static final String TAG = "ShareFragment";
    /**
     * Code id for reading
     */
    private static final int READ_REQUEST_CODE = 42;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.share_fragment,container,false);
        return view;
    }
    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     * (Minimum API is 19)
     */
    public void performFileSearch() {
        Log.i(TAG,"Performing file search");

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter what we want to search for (*/* == everything)
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        Log.i(TAG, "Received onActivityResult");

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && resultData != null) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = resultData.getData();
            Log.i(TAG, "Uri: " + uri.toString());
            //dataToPayload
            try {
                sendDataToEndpoint(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        //Calling super is mandatory!
        super.onActivityResult(requestCode, resultCode, resultData);
    }
    /**
     * Sends data from a uri to (all) endpoints
     * @param uri
     */
    private void sendDataToEndpoint(Uri uri) throws FileNotFoundException {
        Payload payload = dataToPayload(uri);
        AppLogicActivity.getConnectionManager().sendPayload(payload);
    }

    /**
     * Transforms data (pictures, pdfs, etc..) from a URI into a payload so we can send data between
     * different devices
     * See @see <a https://developers.google.com/nearby/connections/android/exchange-data>this</a> for
     * more information
     */
    private Payload dataToPayload(Uri uri) throws FileNotFoundException {
        // Open the ParcelFileDescriptor for this URI with read access.
        ParcelFileDescriptor file = getContext().getContentResolver().openFileDescriptor(uri, "r");
        return Payload.fromFile(file);
    }

}