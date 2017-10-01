package com.example.android.uploaddemov2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button UploadBtn, SelectBtn, PictureBtn;
    private SignInButton signInButton;
    private ImageView imgView;
    private TextView logInDisplay;
    private View ocrDisplay;
    private TextView ocrText;
    private TextView ocrLabel;
    private View mloadingIndicator;
    private View pictureLayout;

    private final int SELECT_IMG_REQUEST = 1;
    private static final int REQUEST_TAKE_PHOTO = 9002;
    private static final int RC_SIGN_IN = 9001;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    //State flags
    private boolean showCameraDialog = false;
    private boolean selectedPhotoMode = false;
    private boolean snapshotPhotoMode = false;

    private Bitmap mBitmap;
    private static String mCurrentSnapPhotoPath;
    private static Uri currentPhotoUri;
    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount acct;

    //private String UploadUrl = "https://nodejs-weather-service.herokuapp.com/upload-android";
    private String UploadUrl = "http://192.168.1.64:3000/upload-android"; //insert local host here


    public static final String LOG_TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UploadBtn = (Button) findViewById(R.id.uploadBtn);
        SelectBtn = (Button) findViewById(R.id.selectBtn);
        PictureBtn = (Button) findViewById(R.id.pictureBtn);

        imgView = (ImageView) findViewById(R.id.imageView);
        logInDisplay = (TextView) findViewById(R.id.logInDisplay);
        ocrDisplay = findViewById(R.id.ocrDisplay);
        ocrText = (TextView) findViewById(R.id.ocrText);
        ocrLabel = (TextView)findViewById(R.id.ocrLabel);
        ocrText.setMovementMethod(new ScrollingMovementMethod());

        pictureLayout = findViewById(R.id.pictureLayout);
        mloadingIndicator = findViewById(R.id.loading_indicator);


        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        PictureBtn.setOnClickListener(this);
        SelectBtn.setOnClickListener(this);
        UploadBtn.setOnClickListener(this);
        UploadBtn.setEnabled(false);
        signInButton.setOnClickListener(this);

        Log.i(LOG_TAG, "SDK: " + Build.VERSION.SDK_INT);
        Log.i(LOG_TAG, "Starting Google Sign-in");

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.i("Google Sign-In: ", "Connection failed.");
                        //do something
                    }
                } /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.pictureBtn:
                dispatchTakePictureIntent();
                break;
            case R.id.selectBtn:
                selectImage();
                break;
            case R.id.uploadBtn:
                uploadImage();
                break;

        }
    }

    private void signIn() {
//        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
//                startActivityForResult(signInIntent, RC_SIGN_IN);
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//
//
//            }
//        };
//        task.execute();

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, SELECT_IMG_REQUEST);

    }

    private void dispatchTakePictureIntent() {

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 23 && (permissionCheck != PackageManager.PERMISSION_GRANTED)) {

            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        } else {
            takePhoto();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentSnapPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                currentPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_IMG_REQUEST && resultCode == RESULT_OK && data != null) {
            //Choose a photo
            Uri path = data.getData();
            currentPhotoUri = path;

            try {
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                imgView.setImageBitmap(mBitmap);
                pictureLayout.setVisibility(View.VISIBLE);
                imgView.setVisibility(View.VISIBLE);

                ocrText.setVisibility(View.GONE);
                ocrLabel.setVisibility(View.GONE);
                ocrDisplay.setVisibility(View.GONE);


                selectedPhotoMode = true;
                snapshotPhotoMode = false;

                UploadBtn.setEnabled(true);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
                //Snapshotted photo
                Log.i("MainActivity", "Displaying snapshotted photo.");

                //Thumbnail:
//            Bundle extras = data.getExtras();
//            mBitmap = (Bitmap) extras.get("data");

                //Full photo
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentPhotoUri);
                    imgView.setImageBitmap(mBitmap);
                    pictureLayout.setVisibility(View.VISIBLE);
                    imgView.setVisibility(View.VISIBLE);

                    ocrText.setVisibility(View.GONE);
                    ocrLabel.setVisibility(View.GONE);
                    ocrDisplay.setVisibility(View.GONE);

                    selectedPhotoMode = false;
                    snapshotPhotoMode = true;

                    UploadBtn.setEnabled(true);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        else if (requestCode == RC_SIGN_IN) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);

            }
        }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(LOG_TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            //mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
            updateUI(true);
            Log.i(LOG_TAG, "Login successful. Token: " + acct.getIdToken());
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
            Log.i(LOG_TAG, "Login not successful");
        }
    }

    private void updateUI(boolean loggedIn) {
        if (loggedIn) {
            signInButton.setVisibility(View.GONE);
            logInDisplay.setText(acct.getDisplayName() + " (" + acct.getEmail() + ")");
        }

        else {


        }

    }

    private void uploadImage() {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, UploadUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.i(LOG_TAG, response);
                            //JSONObject jsonObject = new JSONObject(response); commented out until server response is configured properly

                            Toast.makeText(MainActivity.this, "Image uploaded to server.", Toast.LENGTH_LONG).show();
                            imgView.setImageResource(0);
                            mloadingIndicator.setVisibility(View.GONE);
                            imgView.setVisibility(View.GONE);
                            pictureLayout.setVisibility(View.GONE);

                            ocrDisplay.setVisibility(View.VISIBLE);
                            ocrText.setVisibility(View.VISIBLE);
                            ocrLabel.setVisibility(View.VISIBLE);

                            ocrText.setText(response);


                            if (snapshotPhotoMode)
                            {
                                //delete photo
                                File fdelete = new File(mCurrentSnapPhotoPath);
                                if (fdelete.exists()) {
                                    if (fdelete.delete()) {
                                        Log.i(LOG_TAG, "Snapshotted photo deleted");

                                    } else {
                                        Log.i(LOG_TAG, "Error: Snapshotted photo not deleted");
                                    }
                                }
                                else
                                {
                                    Log.i(LOG_TAG, "Error: Snapshotted photo not found");
                                }
                            }

                            UploadBtn.setEnabled(false);


//                        } catch (JSONException e) {
//                            e.printStackTrace();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(LOG_TAG, "Upload failed.");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                //params.put("name", Name.getText().toString().trim());
                Log.i("MainActivity", "File name: " + getFileName(currentPhotoUri));
                params.put("name", getFileName(currentPhotoUri));
                params.put("image", imageToString(mBitmap));
                return params;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.i(LOG_TAG, "Response code: " + response.statusCode);
                return super.parseNetworkResponse(response);
            }
        };

        mloadingIndicator.setVisibility(View.VISIBLE);
        MySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);

    }

    private String imageToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgBytes, Base64.DEFAULT); //return bitmap in form of string
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_CAMERA_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show();
                    takePhoto();

                }

                else {
                    //Permission denied
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.CAMERA)) {

                        //Optional explanation - set state flag
                        showCameraDialog = true;

                    } else {
                        //Never ask again checked
                    }

                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (showCameraDialog)
        {
            showCameraDialog = false;
            showDialog();
        }

    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    //Dialog
    protected void showDialog() {
        CameraDialog newFragment = CameraDialog.newInstance(
                R.string.camera_dialog_title);
        newFragment.show(getSupportFragmentManager(), "CameraDialog");
    }

    @TargetApi(23)
    public void doPositiveClick() {
        // Do stuff here.
        Log.i("CameraDialog", "Positive click!");
        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
    }

    @TargetApi(23)
    public void doNegativeClick() {
        // Do stuff here.
        Log.i("CameraDialog", "Negative click!");
    }


}
