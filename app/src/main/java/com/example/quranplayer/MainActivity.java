package com.example.quranplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.inspector.MetadataRetriever;
import androidx.viewpager2.widget.ViewPager2;

import com.artifex.mupdf.mini.DocumentActivity;
import com.artifex.mupdf.mini.DocumentActivityViewModel;
import com.example.quranplayer.adapters.ViewPagerAdapter;
import com.example.quranplayer.databinding.ActivityMainBinding;
import com.example.quranplayer.fragments.FilesFragment;
import com.example.quranplayer.fragments.PlayerFragment;
import com.example.quranplayer.datamodels.ItemModel;
import com.example.quranplayer.viewmodels.MainActivityViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@UnstableApi
public class MainActivity extends AppCompatActivity {
    private static final LinkedBlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor poolExecutor =
            new ThreadPoolExecutor(5, 10, 2, TimeUnit.MINUTES, linkedBlockingQueue);
    public static final Handler handler = new Handler(Looper.getMainLooper());
    private static PlayerFragment playerFragment;
    private static FilesFragment filesFragment;
    private static DocumentActivity documentActivity;
    private boolean albumDirExists;
    private static NotificationChannel channel;
    private static NotificationManager manager;
    private ArrayList<MediaItem> mediaItems = new ArrayList<>();
    private ArrayList<ItemModel> itemModels = new ArrayList<>();
    private ActivityMainBinding activityMainBinding;
    private static FrameLayout frameLayout;
    private static LoadingIndicator loadingIndicator;
    private Uri pdfUri;
    private static NotificationChannel channel1;
    private static ActivityResultLauncher<ActivityResultContracts.OpenDocumentTree> openDocumentTreeActivityResultLauncher;
    private static ActivityResultLauncher<ActivityResultContracts.OpenDocument> openDocumentActivityResultLauncher;
    private ViewPager2 viewPager2;
    private static MainActivityViewModel viewModel;

    @SuppressLint("RestrictedApi")
    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.VIBRATE", "android.permission.POST_NOTIFICATIONS"}
                , 20);
        setChannels();
        File albumDir = new File(getApplicationContext().getExternalFilesDir("").getPath() + "/albumDir");
        viewModel.getAlbumDirExists().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                albumDirExists = albumDir.exists();
            }
        });
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this);
        viewPager2 = activityMainBinding.viewPager;
        TabLayout tabLayout = activityMainBinding.tabLayout;
        AppBarLayout appBarLayout = activityMainBinding.appBarLay;
        CoordinatorLayout coordinatorLayout = activityMainBinding.coordinator;
        frameLayout = activityMainBinding.frameLayout;
        loadingIndicator = activityMainBinding.loading;
        Log.i("sortingEverythingAt", Calendar.getInstance(Locale.getDefault()).getTime().toString());
        viewModel.getItemModels().observe(this, (itemModels) -> {
            if (itemModels.isEmpty()) {
                poolExecutor.execute(() -> {
                    try {
                        viewModel.checkAndSortFiles(albumDir);
                    } catch (Exception e) {
                        if (e.getMessage() != null) {
                            Log.e("checkingAndSorting", e.getMessage());
                        }
                        if (e.getStackTrace() != null){
                            for (int i = 0; i < e.getStackTrace().length; i++){
                                Log.e("checkingAndSorting", e.getStackTrace()[i]+"");
                            }
                        }
                        Log.e("checkingAndSorting", e.toString());
                    }
                });
            } else {
                this.itemModels = itemModels;
            }
        });
        viewModel.getMediaItems().observe(this, (mediaItems) -> {
            if (!mediaItems.isEmpty()){
                this.mediaItems = mediaItems;
            }
        });
        Log.i("sortedEverythingIn", Calendar.getInstance(Locale.getDefault()).getTime().toString());
        Log.d("fragmentCountMain", getSupportFragmentManager().getFragments().size() + "");
        playerFragment = new PlayerFragment(getApplicationContext(), tabLayout, appBarLayout, coordinatorLayout, albumDirExists, mediaItems, itemModels,
                manager, channel);
        filesFragment = new FilesFragment(getApplicationContext(), this, playerFragment, viewPager2, albumDirExists, itemModels, mediaItems);
        documentActivity = new DocumentActivity(getApplicationContext(), this, pdfUri, DocumentFile.fromSingleUri(getApplicationContext(), pdfUri)
                .getType());
        Log.d("fragmentCountMain", getSupportFragmentManager().getFragments().size() + "");
        pagerAdapter.addFragment(playerFragment, "Player", AppCompatResources.getDrawable(getApplicationContext(), R.drawable.play_icon));
        pagerAdapter.addFragment(filesFragment, "Files", AppCompatResources.getDrawable(getApplicationContext(),
                R.drawable.folder));
        pagerAdapter.addFragment(documentActivity, "Mushaf", AppCompatResources.getDrawable(getApplicationContext(), R.drawable.quran));
        Log.i("viewPagerCount", viewPager2.getChildCount()+"");
        viewPager2.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout,
                viewPager2, (tab, pos) -> {
            tab.setText(pagerAdapter.getFragmentTitle(pos));
            tab.setIcon(pagerAdapter.getIcon(pos));
            viewPager2.setPageTransformer(new ViewPager2.PageTransformer() {
                @Override
                public void transformPage(@NonNull View page, float position) {

                }
            });
        }).attach();
        viewModel.getPageIndex().observe(this, (integer)->{
            if (integer == -1){
                viewModel.getPageIndex().setValue(viewPager2.getCurrentItem());
            }else {
                viewPager2.setCurrentItem(integer, true);
            }
        });
        viewModel.getIsItemModelsAndMediaItemsFull().observe(this, (full) -> {
            Log.i("full", full+"");
            if (full) {
                loadingIndicator.setVisibility(View.GONE);
                frameLayout.setVisibility(View.VISIBLE);
            }else if (!albumDirExists){
                loadingIndicator.setVisibility(View.GONE);
                frameLayout.setVisibility(View.VISIBLE);
            }
        });
        activityMainBinding.settingBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setPackage(getPackageName());
                intent.setClass(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        viewModel.getColor1().observe(
                this, new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer color1) {
                        int color5 = color1;
                        int color6 = viewModel.getColor2().getValue();
                        if (color5 != 0 && color6 != 0) {
                            tabLayout.setTabTextColors(color5, color6);
                            tabLayout.setBackgroundColor(Color.BLACK);
                            tabLayout.setSelectedTabIndicatorColor(color6);
                            tabLayout.setTabIconTint(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected}
                                    , new int[]{-android.R.attr.state_selected}},
                                    new int[]{color6, color5}));
                            appBarLayout.setBackgroundColor(Color.BLACK);
                            ((MaterialToolbar) appBarLayout.findViewById(R.id.toolBar)).setTitleTextColor(color6);
                            ((MaterialButton) appBarLayout.findViewById(R.id.settingBut)).setIconTint(ColorStateList.valueOf(color6));
                            pagerAdapter.setColor1(color5);
                            pagerAdapter.setColor2(color6);
                        }
                    }
                }
        );
        viewModel.getColor2().observe(this, color2->{
            int color5 = viewModel.getColor1().getValue();
            int color6 = color2;
            if (color5 != 0 && color6 != 0) {
                tabLayout.setTabTextColors(color5, color6);
                tabLayout.setBackgroundColor(Color.BLACK);
                tabLayout.setSelectedTabIndicatorColor(color6);
                tabLayout.setTabIconTint(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected}
                        , new int[]{-android.R.attr.state_selected}},
                        new int[]{color6, color5}));
                appBarLayout.setBackgroundColor(Color.BLACK);
                ((MaterialToolbar) appBarLayout.findViewById(R.id.toolBar)).setTitleTextColor(color6);
                ((MaterialButton) appBarLayout.findViewById(R.id.settingBut)).setIconTint(ColorStateList.valueOf(color6));
                pagerAdapter.setColor1(color5);
                pagerAdapter.setColor2(color6);
            }
        });
        viewModel.getGradientDrawable().observe(this, (gradientDrawable) -> {
            if (gradientDrawable != null) {
                (coordinatorLayout.findViewById(R.id.viewPager)).setBackground(gradientDrawable);
            }
        });

    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        poolExecutor.execute(() -> {
            if (requestCode == 20 && resultCode == Activity.RESULT_OK) {
                handler.post(() -> {
                    loadingIndicator.setVisibility(View.VISIBLE);
                    frameLayout.setVisibility(View.GONE);
                });
                File exFilesDir = getExternalFilesDir("");
                try {
                    assert exFilesDir != null;
                    File albumDir = new File(exFilesDir.getPath() + "/albumDir");
                    if (!albumDir.exists()) {
                        FileOutputStream outputStream = new FileOutputStream(albumDir);
                        assert data.getData() != null;
                        int flags = data.getFlags() & (Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(data.getData(), flags);
                        outputStream.write(Uri.parse(data.getDataString()).toString().getBytes());
                        outputStream.close();
                        viewModel.getAlbumDirExists().postValue(albumDir.exists());
                        try {
                            checkAndSortFiles(albumDir);
                            viewModel.getItemModels().postValue(itemModels);
                            viewModel.getMediaItems().postValue(mediaItems);
                            viewModel.getIsItemModelsAndMediaItemsFull().postValue(true);
                        } catch (Exception e) {
                            Log.e("checkAndSortRes", e.toString());
                        }
                        handler.post(() -> {
                            frameLayout.setVisibility(View.VISIBLE);
                            loadingIndicator.setVisibility(View.GONE);
                        });
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        if (requestCode == 21 && resultCode == Activity.RESULT_OK) {

            Uri uri = data.getData();
            try {
                poolExecutor.execute(() -> {
                    File exFilesDir = getExternalFilesDir("");
                    FileOutputStream outputStream;
                    try {
                        File pdfDest = new File(exFilesDir.getPath() + "/pdfDest");
                        if (!pdfDest.exists()) {
                            outputStream = new FileOutputStream(pdfDest);
                            assert data.getData() != null;
                            int flags = data.getFlags() & (Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(data.getData(), flags);
                            outputStream.write(Uri.parse(data.getDataString()).toString().getBytes());
                            outputStream.close();
                        }
                        handler.post(() -> {
                            documentActivity.setUri(uri);
                            documentActivity.setMimetype(DocumentFile.fromSingleUri(getApplicationContext(), uri).getType());
                            DocumentActivityViewModel viewModel1 = new ViewModelProvider(documentActivity).get(DocumentActivityViewModel.class);
                            viewModel1.getPdfUri().setValue(uri);
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                Log.e("PdfUriErr", e.toString());
            }
        }
    }

    private void checkAndSortFiles(File albumDir) throws Exception {
        if (albumDir.exists()) {
            albumDirExists = true;
            FileInputStream inputStream = new FileInputStream(albumDir);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder uri = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                uri.append(s);
            }
            DocumentFile file = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(uri.toString()));
            DocumentFile[] files = file.listFiles();
            setItems(files.length, files);
            Collections.sort(itemModels, (a, b) -> a.getName().compareTo(b.getName()));
            for (ItemModel itemModel : itemModels) {
                MediaItem mediaItem = new MediaItem.Builder().setUri(itemModel.getUri()).setMediaId(itemModels.indexOf(itemModel) + "").
                        setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(getMetaData(MediaItem.fromUri(itemModel.getUri()))).build())
                        .build();
                mediaItems.add(mediaItem);
            }
        } else {
            albumDirExists = false;
        }
    }

    private ArrayList<ItemModel> setItems(int itemCount, DocumentFile[] filesSorted) throws Exception {
        Bitmap bitmap;
        String title;
        String artist;
        for (int i = 0; i < itemCount; i++) {
            if (MediaFormatUtil.isAudioFormat(MediaFormat.createAudioFormat(filesSorted[i].getType(), 0, 0))) {
                MediaItem mediaItem = MediaItem.fromUri(filesSorted[i].getUri());
                Metadata metadata = getMetaData(mediaItem);
                //populate mediaItem with metaData
                if (metadata != null) {
                    mediaItem = new MediaItem.Builder().setUri(filesSorted[i].getUri())
                            .setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(metadata).build()).build();
                    //get bitmap
                    if (mediaItem.mediaMetadata.artworkData != null) {
                        bitmap = BitmapFactory.decodeByteArray(mediaItem.mediaMetadata.artworkData, 0, mediaItem.mediaMetadata.artworkData.length);
                    } else {
                        bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.quran_image);
                    }
                    //get title
                    if (mediaItem.mediaMetadata.title != null) {
                        title = mediaItem.mediaMetadata.title.toString();
                    } else {
                        title = filesSorted[i].getName();
                    }
                    //album artist
                    if (mediaItem.mediaMetadata.artist != null) {
                        artist = mediaItem.mediaMetadata.artist.toString();
                    } else {
                        artist = "unknown";
                    }
                } else {
                    title = filesSorted[i].getName();
                    bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.quran_image);
                    artist = "unknown";
                }
                //initialize object
                ItemModel itemModel = new ItemModel(artist, bitmap, title);
                itemModel.setUri(filesSorted[i].getUri());
                itemModel.setType(filesSorted[i].getType());
                itemModels.add(itemModel);
            }
        }
        return itemModels;
    }

    private Metadata getMetaData(MediaItem mediaItem) throws Exception {
        MetadataRetriever retriever = new MetadataRetriever.Builder(getApplicationContext(), mediaItem).build();
        ListenableFuture<TrackGroupArray> trackGroupArray = retriever.retrieveTrackGroups();
        Metadata metadata = trackGroupArray.get().get(0).getFormat(0).metadata;
        return metadata;
    }


    public static FilesFragment getFilesFragment() {
        return filesFragment;
    }

    public static FrameLayout getFrameLayout() {
        return frameLayout;
    }

    public static LoadingIndicator getLoadingIndicator() {
        return loadingIndicator;
    }

    public static PlayerFragment getPlayerFragment() {
        return playerFragment;
    }

    public static DocumentActivity getDocumentActivity() {
        return documentActivity;
    }

    private void setChannels() {
        if (viewModel.getManager() == null) {
            manager = ((NotificationManager) viewModel.getApplication().getSystemService(Context.NOTIFICATION_SERVICE));
            viewModel.setManager(manager);
        } else {
            manager = viewModel.getManager();
        }
        channel = viewModel.getChannel() == null ? new NotificationChannel("sessionNotify", "Session Notifications", NotificationManager.IMPORTANCE_DEFAULT) :
                viewModel.getChannel();
        channel1 = viewModel.getChannel1() == null ? new NotificationChannel("endNotify", "Session Notifications", NotificationManager.IMPORTANCE_HIGH) :
                viewModel.getChannel1();
        channel1.enableVibration(true);
        channel1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel1.setVibrationPattern(new long[]{1000, 3000, 4000});
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            channel1.setVibrationEffect(VibrationEffect.createOneShot(3000, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        ArrayList<NotificationChannel> channels = new ArrayList<>();
        channels.add(channel);
        channels.add(channel1);
        manager.createNotificationChannels(channels);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static NotificationChannel getChannel() {
        return channel;
    }

    public static NotificationChannel getChannel1() {
        return channel1;
    }

    public static NotificationManager getManager() {
        return viewModel.getManager();
    }
    public static void setMediaItems(ArrayList<MediaItem> mediaItems){
        viewModel.getMediaItems().postValue(mediaItems);
    }
    public static void setItemModels(ArrayList<ItemModel> itemModels){
        viewModel.getItemModels().postValue(itemModels);
    }
}