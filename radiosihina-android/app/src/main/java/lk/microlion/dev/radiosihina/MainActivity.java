package lk.microlion.dev.radiosihina;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.Timestamp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private MaterialButton playButton, shareStream, btnLibraryPlay;
    private TextView txtNowPlaying, txtPresenter, txtWhyAdsOne, txtLibraryName, txtLibraryBy, txtLibraryDate;
    private AutoCompleteTextView selectLibrary;
    private NavigationBarView navbar;
    private SimpleDraweeView streamImg;
    private ProgressBar bufferBar;
    private AdView adPlayerBottom;
    private RecyclerView libraryProgramList, presenterView;
    private MaterialCardView libraryPlayer;

    private ViewFlipper appView, playerFlipper;

    private AlertDialog loadingPopup;

    private String streamUrl = "";
    private String streamName = "";
    private String streamBy = "";
    private Uri streamImgUrl;
    private boolean streamOnline = false;
    private final boolean mediaReady = false;
    private boolean serviceBound = false;
    private HashMap<String, String> programMap;

    private MediaPlayer player;
    private Notification.Builder playerNotification;
    private NotificationManager notificationManager;

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
        txtWhyAdsOne = findViewById(R.id.txtWhyAdsOne);
        streamImg = findViewById(R.id.streamImage);
        bufferBar = findViewById(R.id.progressBar);
        adPlayerBottom = findViewById(R.id.adPlayerBottom);
        selectLibrary = findViewById(R.id.selectLibrary);
        libraryProgramList = findViewById(R.id.libraryProgramList);
        libraryPlayer = findViewById(R.id.libraryPlayer);
        txtLibraryName = findViewById(R.id.txtLibraryPlayerName);
        txtLibraryBy = findViewById(R.id.txtLibraryPlayerBy);
        txtLibraryDate = findViewById(R.id.txtLibraryPlayerDate);
        btnLibraryPlay = findViewById(R.id.btnLibraryPlayerPlay);
        presenterView = findViewById(R.id.presenterView);

        player = new MediaPlayer();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        loadingPopup = new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setBackground(getDrawable(R.drawable.shape_no_background))
                .setView(R.layout.dialog_loading)
                .create();

        libraryProgramList.setLayoutManager(new LinearLayoutManager(this));
        libraryProgramList.animate();

        loadingPopup.show();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                txtWhyAdsOne.setVisibility(View.VISIBLE);
            }
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        adPlayerBottom.loadAd(adRequest);

        navbar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.page_1){
                    appView.setDisplayedChild(0);
                }else if(item.getItemId() == R.id.page_2){
                    appView.setDisplayedChild(1);
                //}else if(item.getItemId() == R.id.page_3){
                //    appView.setDisplayedChild(2);
                }else if(item.getItemId() == R.id.page_4){
                    appView.setDisplayedChild(3);
                }else{
                    return false;
                }
                return true;
            }
        });

        selectLibrary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                loadingPopup.show();
                String itemId = programMap.keySet().toArray()[position].toString();
                ArrayList<ArrayList<String>> arrayList = new ArrayList<ArrayList<String>>();

                db.collection("library").whereEqualTo("id", itemId).whereLessThanOrEqualTo("date", Timestamp.now()).orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String id = document.getString("id");
                                        String link = document.getString("link");
                                        String date = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aaa").format(document.getDate("date"));
                                        db.collection("programs").document(id).get()
                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> t) {
                                                        if (t.isSuccessful()) {
                                                            DocumentSnapshot program = t.getResult();
                                                            if (program.exists()) {
                                                                String programName = program.getString("name");
                                                                String presenterId = program.getString("presenter");
                                                                db.collection("presenters").document(presenterId).get()
                                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<DocumentSnapshot> t) {
                                                                                if (t.isSuccessful()) {
                                                                                    DocumentSnapshot program = t.getResult();
                                                                                    if (program.exists()) {
                                                                                        String programBy = program.getString("firstname")+" "+program.getString("lastname");
                                                                                        ArrayList<String> list = new ArrayList<>();
                                                                                        list.add(0, programName);
                                                                                        list.add(1, programBy);
                                                                                        list.add(2, date);
                                                                                        list.add(3, link);
                                                                                        arrayList.add(list);
                                                                                        loadingPopup.dismiss();
                                                                                        libraryProgramList.setAdapter(new LibraryAdapter(arrayList, player, MainActivity.this));
                                                                                    } else {
                                                                                        showInfoMessage("Error Loading Data", "No recordings from this listener");
                                                                                        loadingPopup.dismiss();
                                                                                    }
                                                                                } else {
                                                                                    showInfoMessage("Error Loading Data", t.getException().toString());
                                                                                    loadingPopup.dismiss();
                                                                                }
                                                                            }
                                                                        });
                                                            } else {
                                                                showInfoMessage("Error Loading Data", "No recordings from this listener");
                                                                loadingPopup.dismiss();
                                                            }
                                                        } else {
                                                            showInfoMessage("Error Loading Data", t.getException().toString());
                                                            loadingPopup.dismiss();
                                                        }
                                                    }
                                                });

                                    }
                                    loadingPopup.dismiss();
                                } else {
                                    showInfoMessage("Error Loading Data", task.getException().toString());
                                    loadingPopup.dismiss();
                                }
                            }
                        });
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
                                loadingPopup.dismiss();
                                playerFlipper.setDisplayedChild(0);
                            }
                        } else {
                            loadingPopup.dismiss();
                            playerFlipper.setDisplayedChild(0);
                            //showInfoMessage("Error loading data", "There is something wrong connecting to the server. " +
                            //        "Please check your internet connection or email to developers.microlion@gmail.com");
                        }
                    }
                });

        db.collection("programs")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return;
                        }

                        programMap = new HashMap<String, String>();

                        for (QueryDocumentSnapshot doc : value) {
                            if (doc.get("name") != null) {
                                programMap.put(doc.getId(), doc.getString("name"));
                            }
                        }

                        selectLibrary.setAdapter(new ArrayAdapter(MainActivity.this, R.layout.library_list_item, programMap.values().toArray()));
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
        //}else if(selectedItem == R.id.page_3){
        //    appView.setDisplayedChild(2);
        }else if(selectedItem == R.id.page_4){
            appView.setDisplayedChild(3);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        playButton.setEnabled(false);

        if(btnLibraryPlay.getText().toString().equals("Stop")){
            player.stop();
            btnLibraryPlay.setText("Play");
            btnLibraryPlay.setIconResource(R.drawable.ic_round_play_arrow_24);
            libraryPlayer.setVisibility(View.GONE);
        }

        if (player.isPlaying()) {
            player.stop();
            notificationManager.cancel(0);
            playButton.setText("Play");
            playButton.setIconResource(R.drawable.ic_round_play_arrow_24);
        } else {
            if (streamOnline) {
                try {
                    player = new MediaPlayer();
                    player.setWakeMode(MainActivity.this, PowerManager.PARTIAL_WAKE_LOCK);
                    player.setDataSource(streamUrl);
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setAudioAttributes(new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setLegacyStreamType(AudioAttributes.FLAG_LOW_LATENCY)
                                    .build());
                    player.prepare();
                    player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            bufferBar.setVisibility(View.VISIBLE);
                            if(percent == 100.00){
                                bufferBar.setVisibility(View.INVISIBLE);
                            }
                        }
                    });
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        playerNotification = new Notification.Builder(MainActivity.this)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("Radio Sihina Live")
                                .setContentText("Now playing "+streamName+" by "+streamBy)
                                .setLargeIcon(Icon.createWithResource(MainActivity.this, R.drawable.logo_transparent_background))
                                .setStyle(new Notification.MediaStyle().setMediaSession(new MediaSession(MainActivity.this, "RADIO-SIHINA-LIVE").getSessionToken()))
                                .setOngoing(true)
                                .setAutoCancel(false)
                                .setPriority(Notification.PRIORITY_HIGH);
                        notificationManager.notify(0, playerNotification.build());
                    }else{
                        playerNotification = new Notification.Builder(MainActivity.this)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("Radio Sihina Live")
                                .setContentText("Now playing "+streamName+" by "+streamBy)
                                .setOngoing(true)
                                .setAutoCancel(false)
                                .setPriority(Notification.PRIORITY_HIGH);
                        notificationManager.notify(0, playerNotification.build());
                    }
                    playButton.setText("Stop");
                    playButton.setIconResource(R.drawable.ic_baseline_stop_24);
                } catch (IOException e) {
                    Toast.makeText(this, "Can't play the stream. Check you connection.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        bufferBar.setVisibility(View.INVISIBLE);
        playButton.setEnabled(true);
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

    public void showSelectedLibraryPlayer(String s, String s1, String s2, String s3) {
        libraryPlayer.setVisibility(View.VISIBLE);
        txtLibraryName.setText(s);
        txtLibraryBy.setText(s1);
        txtLibraryDate.setText(s2);
        btnLibraryPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnLibraryPlay.getText().toString().equals("Play")) {
                    if(player.isPlaying()){
                        player.stop();
                        notificationManager.cancel(0);
                        playButton.setText("Play");
                        playButton.setIconResource(R.drawable.ic_round_play_arrow_24);
                    }
                    try {
                        player = new MediaPlayer();
                        player.setWakeMode(MainActivity.this, PowerManager.PARTIAL_WAKE_LOCK);
                        player.setDataSource(s3);
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        player.setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setLegacyStreamType(AudioAttributes.FLAG_LOW_LATENCY)
                                .build());
                        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.start();
                            }
                        });
                        player.prepare();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            playerNotification = new Notification.Builder(MainActivity.this)
                                    .setSmallIcon(R.mipmap.ic_launcher_round)
                                    .setContentTitle(s)
                                    .setContentText(s2)
                                    .setLargeIcon(Icon.createWithResource(MainActivity.this, R.drawable.logo_transparent_background))
                                    .setStyle(new Notification.MediaStyle().setMediaSession(new MediaSession(MainActivity.this, "RADIO-SIHINA-LIVE").getSessionToken()))
                                    .setOngoing(true)
                                    .setAutoCancel(false)
                                    .setPriority(Notification.PRIORITY_HIGH);
                            notificationManager.notify(0, playerNotification.build());
                        }else{
                            playerNotification = new Notification.Builder(MainActivity.this)
                                    .setSmallIcon(R.mipmap.ic_launcher_round)
                                    .setContentTitle(s)
                                    .setContentText(s2)
                                    .setOngoing(true)
                                    .setAutoCancel(false)
                                    .setPriority(Notification.PRIORITY_HIGH);
                            notificationManager.notify(0, playerNotification.build());
                        }
                        btnLibraryPlay.setText("Stop");
                        btnLibraryPlay.setIconResource(R.drawable.ic_baseline_stop_24);
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, "Can't play the stream. Check you connection.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    player.stop();
                    btnLibraryPlay.setText("Play");
                    btnLibraryPlay.setIconResource(R.drawable.ic_round_play_arrow_24);
                    libraryPlayer.setVisibility(View.GONE);
                }
            }
        });
    }

    public void ifPlayingStop(String s, String s1) {
        if(!(txtLibraryName.getText().toString().equals(s) && txtLibraryDate.getText().toString().equals(s1))){
            player.stop();
            btnLibraryPlay.setText("Play");
            btnLibraryPlay.setIconResource(R.drawable.ic_round_play_arrow_24);
            libraryPlayer.setVisibility(View.GONE);
        }
    }
}