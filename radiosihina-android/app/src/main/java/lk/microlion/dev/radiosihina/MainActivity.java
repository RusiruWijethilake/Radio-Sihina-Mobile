package lk.microlion.dev.radiosihina;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;
import com.google.firebase.storage.FirebaseStorage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements NetworkStateReceiver.NetworkStateReceiverListener, AudioManager.OnAudioFocusChangeListener {

    private final boolean serviceBound = false;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private MaterialButton shareStream;
    private TextView txtNowPlaying, txtPresenter, txtWhyAdsOne, txtLibraryName, txtLibraryBy, txtLibraryDate;
    private WebView webViewPlayer, webViewLibraryPlayer;
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
    private boolean isInternetAvailable = false;
    private HashMap<String, String> programMap;

    private Notification.Builder playerNotification;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;

    private NetworkStateReceiver networkStateReceiver;
    private boolean playerPaused = false;

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
        shareStream = findViewById(R.id.shareStream);
        txtNowPlaying = findViewById(R.id.txtProgramName);
        txtPresenter = findViewById(R.id.txtProgramBy);
        txtWhyAdsOne = findViewById(R.id.txtWhyAdsOne);
        webViewPlayer = findViewById(R.id.webViewPlayer);
        streamImg = findViewById(R.id.streamImage);
        bufferBar = findViewById(R.id.progressBar);
        adPlayerBottom = findViewById(R.id.adPlayerBottom);
        selectLibrary = findViewById(R.id.selectLibrary);
        libraryProgramList = findViewById(R.id.libraryProgramList);
        libraryPlayer = findViewById(R.id.libraryPlayer);
        txtLibraryName = findViewById(R.id.txtLibraryPlayerName);
        txtLibraryBy = findViewById(R.id.txtLibraryPlayerBy);
        txtLibraryDate = findViewById(R.id.txtLibraryPlayerDate);
        webViewLibraryPlayer = findViewById(R.id.webViewLibrayPlayer);
        presenterView = findViewById(R.id.presenterView);

        webViewPlayer.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        webViewPlayer.setBackgroundColor(0x00000000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webViewPlayer.setForceDarkAllowed(true);
        }
        WebSettings webSettings = webViewPlayer.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webViewLibraryPlayer.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        webViewLibraryPlayer.setBackgroundColor(0x00000000);
        webViewLibraryPlayer.setSaveEnabled(true);
        webViewLibraryPlayer.setSaveFromParentEnabled(true);
        webViewLibraryPlayer.setWebViewClient(new WebViewClient());
        webViewLibraryPlayer.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                showInfoMessage("Web", url);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            webViewLibraryPlayer.setForceDarkAllowed(true);
        }
        WebSettings webLibrarySettings = webViewLibraryPlayer.getSettings();
        webLibrarySettings.setJavaScriptEnabled(true);

        webSettings.setAllowContentAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webLibrarySettings.setAllowContentAccess(true);
        webLibrarySettings.setAppCacheEnabled(true);
        webLibrarySettings.setMediaPlaybackRequiresUserGesture(false);
        webLibrarySettings.setAllowContentAccess(true);
        webLibrarySettings.setAllowFileAccess(true);
        webLibrarySettings.setBlockNetworkLoads(false);
        webLibrarySettings.setAllowUniversalAccessFromFileURLs(true);
        webLibrarySettings.setAllowFileAccessFromFileURLs(true);
        webLibrarySettings.setSupportMultipleWindows(true);
        webLibrarySettings.setDomStorageEnabled(true);
        webLibrarySettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webLibrarySettings.setLoadWithOverviewMode(true);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        startNetworkBroadcastReceiver(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioAttributes.FLAG_LOW_LATENCY)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this::onAudioFocusChange)
                    .build();
        }

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
                if (item.getItemId() == R.id.page_1) {
                    appView.setDisplayedChild(0);
                } else if (item.getItemId() == R.id.page_2) {
                    appView.setDisplayedChild(1);
                    //}else if(item.getItemId() == R.id.page_3){
                    //    appView.setDisplayedChild(2);
                } else if (item.getItemId() == R.id.page_4) {
                    appView.setDisplayedChild(3);
                } else {
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
                                                                                        String programBy = program.getString("firstname") + " " + program.getString("lastname");
                                                                                        ArrayList<String> list = new ArrayList<>();
                                                                                        list.add(0, programName);
                                                                                        list.add(1, programBy);
                                                                                        list.add(2, date);
                                                                                        list.add(3, link);
                                                                                        arrayList.add(list);
                                                                                        loadingPopup.dismiss();
                                                                                        libraryProgramList.setAdapter(new LibraryAdapter(arrayList, MainActivity.this));
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
                                                streamImg.setImageURI(uri);
                                                streamName = value.getString("nowplaying");
                                                streamBy = value.getString("presenter");
                                                loadingPopup.dismiss();
                                                webViewPlayer.loadData("<center><audio controls><source src='"+streamUrl+"' type='audio/mpeg'></audio></center>", "", "");
                                            }
                                        });
                            } else {
                                webViewPlayer.loadData("<center>Stream Offline</center>", "", "");
                                loadingPopup.dismiss();
                                playerFlipper.setDisplayedChild(0);
                            }
                        } else {
                            webViewPlayer.loadData("<center>Stream Offline</center>", "", "");
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

    private void startNetworkBroadcastReceiver(Context currentContext) {
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener((NetworkStateReceiver.NetworkStateReceiverListener) currentContext);
        registerNetworkBroadcastReceiver(currentContext);
    }

    public void registerNetworkBroadcastReceiver(Context currentContext) {
        currentContext.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void unregisterNetworkBroadcastReceiver(Context currentContext) {
        currentContext.unregisterReceiver(networkStateReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        int selectedItem = navbar.getSelectedItemId();

        registerNetworkBroadcastReceiver(this);

        if (selectedItem == R.id.page_1) {
            appView.setDisplayedChild(0);
        } else if (selectedItem == R.id.page_2) {
            appView.setDisplayedChild(1);
            //}else if(selectedItem == R.id.page_3){
            //    appView.setDisplayedChild(2);
        } else if (selectedItem == R.id.page_4) {
            appView.setDisplayedChild(3);
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterNetworkBroadcastReceiver(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Listen to Radio Sihina live stream now. Go to https://radiosihina.ml " +
                "or Download the Radio Sihina Android or IOS app. Now playing " + streamName + " by " + streamBy);
        shareIntent.setType("text/*");
        startActivity(Intent.createChooser(shareIntent, "Share Radio Sihina"));
    }

    public void showSelectedLibraryPlayer(String s, String s1, String s2, String s3) {
        libraryPlayer.setVisibility(View.VISIBLE);
        txtLibraryName.setText(s);
        txtLibraryBy.setText(s1);
        txtLibraryDate.setText(s2);
        webViewPlayer.reload();
        webViewLibraryPlayer.loadData("<center><audio preload='none' controls><source src='https://firebasestorage.googleapis.com/v0/b/radio-sihina.appspot.com/o/programs-recordings%2FRetro%20Hour%20-%202022-06-11.mp3?alt=media&token=18b3bc8a-cce3-4187-b838-c5cc42fba1f6' type='audio/mpeg'></audio></center>", "", "");
    }

    @Override
    public void networkAvailable() {
        isInternetAvailable = true;
    }

    @Override
    public void networkUnavailable() {
        isInternetAvailable = false;
        Toast.makeText(this, "Player stopped due to network lost", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        }
        webViewLibraryPlayer.reload();
        webViewPlayer.reload();
        libraryPlayer.setVisibility(View.GONE);
        notificationManager.cancel(0);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                webViewPlayer.reload();
                webViewLibraryPlayer.reload();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager.abandonAudioFocusRequest(focusRequest);
                }
                libraryPlayer.setVisibility(View.GONE);
                notificationManager.cancel(0);
                break;
        }
    }
}