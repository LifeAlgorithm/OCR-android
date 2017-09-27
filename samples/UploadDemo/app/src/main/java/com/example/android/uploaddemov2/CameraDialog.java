package com.example.android.uploaddemov2;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by Armand on 9/24/2017.
 */

public class CameraDialog extends DialogFragment {

    public static CameraDialog newInstance(int title) {
        CameraDialog frag = new CameraDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title"); //resource ID

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Without this permission, the app will not be able to start the process. Are you sure you want to deny this permission?")
                .setTitle(title)
                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        ((MainActivity)getActivity()).doPositiveClick();
                    }
                })
                .setNegativeButton("I'm sure", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        ((MainActivity)getActivity()).doNegativeClick();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
