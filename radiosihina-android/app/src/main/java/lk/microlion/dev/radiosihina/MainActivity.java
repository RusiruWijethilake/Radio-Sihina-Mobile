package lk.microlion.dev.radiosihina;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private MaterialButton playButton, shareStream;
    private TextView txtNowPlaying, txtPresenter;
    private NavigationBarView navbar;
    private SimpleDraweeView streamImg;
    private ProgressBar bufferBar;

    private ViewFlipper appView, playerFlipper;

    private AlertDialog loadingPopup;

    private String streamUrl = "";
    private String streamName = "";
    private String streamBy = "";
    private Uri streamImgUrl;
    private boolean streamOnline = false;
    private boolean mediaReady = false;
    private boolean serviceBound = false;

    private RadioPlayerService player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        appView = findViewById(R.id.flipperView);
        playerFlipper = findViewById(R.id.playerFlipper);
        navbar = findViewById(R.id.bottom_navigation);
        playButton = findViewById(R.id.btnPlay);
        shareStream = findViewById(R.id.shareStream);
        txtNowPlaying = findViewById(R.id.txtProgramName);
        txtPresenter = findViewById(R.id.txtProgramBy);
        streamImg = findViewById(R.id.streamImage);
        bufferBar = findViewById(R.id.progressBar);

        loadingPopup = new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setBackground(getDrawable(R.drawable.shape_no_background))
                .setView(R.layout.dialog_loading)
                .create();

        loadingPopup.show();

        navbar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.page_1){
                    appView.setDisplayedChild(0);
                }else if(item.getItemId() == R.id.page_2){
                    appView.setDisplayedChild(1);
                }else if(item.getItemId() == R.id.page_3){
                    appView.setDisplayedChild(2);
                }else if(item.getItemId() == R.id.page_4){
                    appView.setDisplayedChild(3);
                }else{
                    return false;
                }
                return true;
            }
        });

        db.collection("live").document("radiosihina")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            showInfoMessage("Error loading data", "There is something wrong connecting to the server. " +
                                    "Please check your internet connection or email to developers.microlion@gmail.com");
                            loadingPopup.dismiss();
                            return;
                        }

                        if (value != null && value.exists()) {
                            if (value.getBoolean("status")) {
                                storage.getReferenceFromUrl(value.getString("imgurl")).getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            playerFlipper.setDisplayedChild(1);
                                            txtNowPlaying.setText(value.getString("nowplaying"));
                                            txtPresenter.setText(value.getString("presenter"));
                                            streamUrl = value.getString("streamurl");
                                            streamOnline = value.getBoolean("status");
                                            streamImg.setImageURI(uri);
                                            streamName = value.getString("nowplaying");
                                            streamBy = value.getString("presenter");
                                            streamImgUrl = uri;
                                            loadingPopup.dismiss();
                                        }
                                    });
                            } else {
                                playerFlipper.setDisplayedChild(0);
                                loadingPopup.dismiss();
                            }
                        } else {
                            playerFlipper.setDisplayedChild(0);
                            showInfoMessage("Error loading data", "There is something wrong connecting to the server. " +
                                    "Please check your internet connection or email to developers.microlion@gmail.com");
                            loadingPopup.dismiss();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        int selectedItem = navbar.getSelectedItemId();

        if(selectedItem== R.id.page_1){
            appView.setDisplayedChild(0);
        }else if(selectedItem == R.id.page_2){
            appView.setDisplayedChild(1);
        }else if(selectedItem == R.id.page_3){
            appView.setDisplayedChild(2);
        }else if(selectedItem == R.id.page_4){
            appView.setDisplayedChild(3);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            player.stopSelf();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    public void playAudio(View view) {
        bufferBar.setVisibility(View.VISIBLE);
        if (serviceBound) {
            unbindService(serviceConnection);
            player.stopSelf();
            playButton.setText("Play");
            playButton.setIconResource(R.drawable.ic_round_play_arrow_24);
            serviceBound = false;
        } else {
            if (streamOnline) {
                playAudio(streamUrl);
                playButton.setText("Stop");
                playButton.setIconResource(R.drawable.ic_baseline_stop_24);
            }
        }
        bufferBar.setVisibility(View.GONE);
    }

    private void showInfoMessage(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void shareStream(View view) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Listen to Radio Sihina live stream now. Go to https://radiosihina.ml "+
                "or Download the Radio Sihina Android or IOS app. Now playing "+streamName+" by "+streamBy);
        shareIntent.setType("text/*");
        startActivity(Intent.createChooser(shareIntent, "Share Radio Sihina"));
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RadioPlayerService.LocalBinder binder = (RadioPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(String media) {
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, RadioPlayerService.class);
            playerIntent.putExtra("streamurl", media);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send media with BroadcastReceiver
        }
    }
}