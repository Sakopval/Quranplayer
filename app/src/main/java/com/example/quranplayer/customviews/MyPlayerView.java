package com.example.quranplayer.customviews;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.session.MediaController;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.inspector.MetadataRetriever;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import com.example.quranplayer.MainActivity;
import com.example.quranplayer.R;
import com.example.quranplayer.adapters.ViewPagerAdapter;
import com.example.quranplayer.databinding.ControllerLayout2Binding;
import com.example.quranplayer.databinding.ControllerLayoutBinding;
import com.example.quranplayer.databinding.PlayerLayoutBinding;
import com.example.quranplayer.datamodels.ItemModel;
import com.example.quranplayer.viewmodels.MainActivityViewModel;
import com.example.quranplayer.viewmodels.PlayerFragmentViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Random;
import java.util.concurrent.ThreadPoolExecutor;

@UnstableApi
public class MyPlayerView extends PlayerView {
    private boolean paused = true;
    private boolean playing = false;
    private boolean ended = false;
    private boolean muted = false;
    private boolean albumDirExists = false;
    private final int COLOR_CONST = Color.argb(255, 0, 5, 5);
    private LifecycleOwner owner;
    private ArrayList<MediaItem> mediaItems = new ArrayList<>();
    private ArrayList<ItemModel> itemModels = null;
    private final ThreadPoolExecutor poolExecutor = MainActivity.poolExecutor;
    private AndroidViewModel sharedViewModel;
    private PlayerFragmentViewModel viewModel;
    private final Handler handler = MainActivity.handler;
    private Player player;
    private MyController controlView;
    private DefaultTimeBar timeBar;
    private MaterialButton pausedBut;
    private TextView totalTime;
    private TextView timePassed;
    private MaterialButton volBut;
    private MaterialButton nextBut;
    private MaterialButton previousBut;
    private MaterialButton saveBut;
    private MaterialButton histBut;
    private MaterialButton sessionBut;
    private TextView title;
    private ImageView cover;
    private ImageView background;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private Notification.Builder builder;
    private Notification.Builder endNotify;
    private NotificationManager manager = MainActivity.getManager();
    private Notification notification;
    private RemoteViews views;
    private CoordinatorLayout coordinatorLayout;
    private int random;
    private int history = -1;
    private long time;
    private boolean sessionStarted;
    private long sessionDefTime = -1;


    private void handlePlayPause() {
        if (playing) {
            showPauseBut();
            if (player != null) {
                player.pause();
            }
        } else if (paused) {
            showPlayBut();
            if (player != null) {
                player.play();
            }
            if (ended) {
                ended = false;
                timeBar.setPosition(0);
                player.seekTo(0);
                player.play();
            }
        }
    }

    private void setButsColorAndTabs(int color1, int color2, GradientDrawable gradientDrawable) {
        ColorStateList.valueOf(color1);
        //compares the color at bottom to itself for controllers
        int color3 = ((int) (ColorUtils.calculateLuminance(color1) < 0.5 ? color1 * 2 : color1 * 0.2));
        pausedBut.setIconTint(ColorStateList.valueOf(color3));
        previousBut.setIconTint(ColorStateList.valueOf(color3));
        nextBut.setIconTint(ColorStateList.valueOf(color3));
        volBut.setIconTint(ColorStateList.valueOf(color3));
        saveBut.setIconTint(ColorStateList.valueOf(color3));
        timeBar.setPlayedColor(color3);
        timeBar.setScrubberColor(color3);
        totalTime.setTextColor(color3);
        timePassed.setTextColor(color3);
        histBut.setIconTint(ColorStateList.valueOf(color3));
        sessionBut.setIconTint(ColorStateList.valueOf(color3));

        //for title only the color at center
        int color4 = ((int) (ColorUtils.calculateLuminance(color2) < 0.5 ? color2 * 2 : color2 * 0.2));
        title.setTextColor(color4);

        final int color5 = ColorUtils.calculateContrast(color1, COLOR_CONST) < 3 ? ((int) (color1 * 1.3)) : color1;
        final int color6 = ColorUtils.calculateContrast(color2, COLOR_CONST) < 3 ? ((int) (color2 * 1.3)) : color2;
        ((MainActivityViewModel) sharedViewModel).getColor2().setValue(color6);
        ((MainActivityViewModel) sharedViewModel).getColor1().setValue(color5);
        if (tabLayout == null){
            ((MainActivityViewModel) sharedViewModel).getIsMainActive().setValue(false);
        }
        ((MainActivityViewModel) sharedViewModel).getIsMainActive().observe(
                owner, new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean aBoolean) {
                        if (aBoolean){
                            tabLayout.setTabTextColors(color5, color6);
                            tabLayout.setBackgroundColor(COLOR_CONST);
                            tabLayout.setSelectedTabIndicatorColor(color6);
                            tabLayout.setTabIconTint(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected}
                                    , new int[]{-android.R.attr.state_selected}},
                                    new int[]{color6, color5}));
                            appBarLayout.setBackgroundColor(COLOR_CONST);
                            ((MaterialToolbar) appBarLayout.findViewById(R.id.toolBar)).setTitleTextColor(color6);
                            ((MaterialButton) appBarLayout.findViewById(R.id.settingBut)).setIconTint(ColorStateList.valueOf(color6));
                            (coordinatorLayout.findViewById(R.id.viewPager)).setBackground(gradientDrawable);
                            ViewPagerAdapter pagerAdapter = ((ViewPagerAdapter) ((ViewPager2) coordinatorLayout
                                    .findViewById(R.id.viewPager)).getAdapter());
                            pagerAdapter.setColor1(color5);
                            pagerAdapter.setColor2(color6);
                        }
                    }
                }
        );
    }

    public void setCoverAndBackGround(MediaItem mediaItem, Uri fileUri) {
        final MediaItem mediaItem1 = mediaItem;
        Log.i("from setCoverAndBack Tag:", mediaItem.mediaId);
        poolExecutor.execute(() -> {
            try {
                MetadataRetriever retriever = new MetadataRetriever.Builder(sharedViewModel.getApplication(), mediaItem).build();
                ListenableFuture<TrackGroupArray> trackGroupListenableFutureTask = retriever.retrieveTrackGroups();
                Metadata metadata = trackGroupListenableFutureTask.get().get(0).getFormat(0).metadata;
                final MediaItem mediaItem2 = mediaItem1.buildUpon().setUri(fileUri)
                        .setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(metadata).build()).build();
                Bitmap bitmap = null;
                Palette palette;
                if (mediaItem2.mediaMetadata.artworkData != null) {
                    bitmap = BitmapFactory.decodeByteArray(mediaItem2.mediaMetadata.artworkData, 0, mediaItem2.mediaMetadata.artworkData.length);
                }
                try {
                    System.out.println("Palette bitmap: " + "trying");
                    palette = Palette.from(bitmap).generate();
                } catch (Exception e) {
                    Log.e("Palette bitmap: ", e.toString());
                    bitmap = BitmapFactory.decodeResource(sharedViewModel.getApplication().getResources(), R.drawable.quran_image);
                    palette = Palette.from(bitmap).generate();
                }
                int color = palette.getVibrantColor(getResources().getColor(R.color.red, null));
                int color1 = palette.getDominantColor(getResources().getColor(R.color.red, null));
                GradientDrawable gradientDrawable = new GradientDrawable();
                if (ColorUtils.calculateContrast(color, color1) < 3) {
                    color = ((int) (color * 0.9));
                }
                gradientDrawable.setColors(new int[]{color, color1, COLOR_CONST});
                gradientDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                ((MainActivityViewModel) sharedViewModel).getGradientDrawable().postValue(gradientDrawable);
                int finalColor = color;
                Bitmap finalBitmap = bitmap;
                int finalColor1 = color1;
                handler.post(() -> {
                    cover.setImageBitmap(finalBitmap);
                    if (mediaItem2.mediaMetadata.title != null) {
                        title.setText(mediaItem2.mediaMetadata.title);
                    } else {
                        title.setText(itemModels.get(random).getName());
                    }
                    ((View) background.getParent()).setBackground(gradientDrawable);
                    setButsColorAndTabs(finalColor1, finalColor, gradientDrawable);
                });
            } catch (Exception e) {
                Log.e("PlayerView setCoverMethod: ", e.toString());
            }
        });
    }

    private void showPauseBut() {
        pausedBut.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
        paused = true;
        playing = false;
    }

    private void showPlayBut() {
        pausedBut.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
        paused = false;
        playing = true;
    }

    private void setListenersForButs() {
        volBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (muted) {
                    volBut.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.volume_max, null));
                    muted = false;
                    player.unmute();
                } else {
                    volBut.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.mute, null));
                    muted = true;
                    player.mute();
                }
            }
        });
        pausedBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayPause();
            }
        });
        timeBar.addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {
                player.seekTo(position);
            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {
                player.seekTo(position);
            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                player.seekTo(position);
            }
        });
        nextBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("nextBut", "" + player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM));
                paused = true;
                playing = false;
                ended = false;
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    Log.e("nextBut", "yes next");
                } else {
                    Log.e("nextBut", "no next");
                }
                if (player.getCurrentMediaItemIndex() == history) {
                    player.seekTo(time);
                }
            }
        });
        nextBut.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        nextBut.animate().scaleX(0.83f).scaleY(0.83f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                nextBut.setScaleX(0.85f);
                                nextBut.setScaleY(0.85f);
                                nextBut.clearAnimation();
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        nextBut.animate().scaleX(1f).scaleY(1f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                nextBut.clearAnimation();
                            }
                        }).start();
                        Log.d("nextButTouch", "up successful");
                        break;
                    default:
                        Log.d("nextButTouch", "event is " + event.getAction());
                        break;
                }
                return false;
            }
        });
        previousBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                paused = true;
                playing = false;
                ended = false;
                player.seekToPreviousMediaItem();
                if (player.getCurrentMediaItemIndex() == history) {
                    player.seekTo(time);
                }
            }
        });
        previousBut.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        previousBut.animate().scaleX(0.83f).scaleY(0.83f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                previousBut.setScaleX(0.85f);
                                previousBut.setScaleY(0.85f);
                                previousBut.clearAnimation();
                            }
                        }).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        previousBut.animate().scaleX(1f).scaleY(1f).setDuration(100).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                previousBut.clearAnimation();
                            }
                        }).start();
                        Log.d("preButTouch", "up successful");
                        break;
                    default:
                        Log.d("preButTouch", "event is " + event.getAction());
                        break;
                }
                return false;
            }
        });
        saveBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBut.setIcon(ResourcesCompat.getDrawable(sharedViewModel.getApplication().getResources(), R.drawable.save_success, null));
                File historyFile = new File(sharedViewModel.getApplication().getExternalFilesDir("").getPath() + "/history");
                try {
                    if (player.getCurrentMediaItem() != null) {
                        Log.i("saveBut Clicked", "mediaId: " + player.getCurrentMediaItem().mediaId);
                        boolean delete = historyFile.delete();
                        delete = historyFile.createNewFile();
                        FileOutputStream outputStream = new FileOutputStream(historyFile);
                        JSONObject jsonObject = new JSONObject();
                        int index = Integer.parseInt(player.getCurrentMediaItem().mediaId);
                        jsonObject.put("index", index);
                        jsonObject.put("time", player.getCurrentPosition());
                        time = player.getCurrentPosition();
                        history = index;
                        outputStream.write(jsonObject.toString().getBytes());
                        outputStream.close();
                    }
                    poolExecutor.execute(() -> {
                        try {
                            Thread.sleep(1000);
                            handler.post(() -> {
                                saveBut.animate().alpha(0f).setDuration(2000).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        saveBut.setIcon(ResourcesCompat.getDrawable(sharedViewModel.getApplication()
                                                .getResources(), R.drawable.save, null));
                                        saveBut.animate().alphaBy(1).setDuration(2000).setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                saveBut.clearAnimation();
                                            }
                                        }).start();
                                    }
                                }).start();
                            });
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (Exception e) {
                    Log.e("saveButClick", e.toString());
                }
            }
        });
        histBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
                random = history;
                ((MainActivityViewModel) sharedViewModel).setRandom(random);
                setMediaItemsForPlayer();
                histBut.animate().rotationBy(-360).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        histBut.clearAnimation();
                    }
                }).start();
            }
        });
        sessionBut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                File defTime = new File(sharedViewModel.getApplication().getExternalFilesDir("").getPath() + "/defTime");
                if (defTime.exists()) {
                    poolExecutor.execute(() -> {
                        FileInputStream inputStream;
                        try {
                            inputStream = new FileInputStream(defTime);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            StringBuilder builder = new StringBuilder();
                            String s;
                            while ((s = reader.readLine()) != null) {
                                builder.append(s);
                            }
                            sessionDefTime = Integer.parseInt(builder.toString());
                            inputStream.close();
                            reader.close();

                        } catch (Exception e) {
                            Log.e("sessionBut", e.toString());
                        }
                    });
                    if (sessionStarted) {
                        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
                        sessionStarted = false;
                        ((MainActivityViewModel) sharedViewModel).getSessionStarted().setValue(sessionStarted);
                    } else {
                        AnimatedVectorDrawable vectorDrawable = ((AnimatedVectorDrawable) sessionBut.getIcon());
                        vectorDrawable.start();
                        sessionStarted = true;
                        notification = builder.setDefaults(NotificationCompat.DEFAULT_ALL).build();
                        player.play();
                        CountDownTimer timer = new CountDownTimer(sessionDefTime*60*1000, 1000) {
                            @Override
                            public void onFinish() {
                                ((MainActivityViewModel) sharedViewModel).getManager().notify(2, endNotify.build());
                                sessionStarted = false;
                            }

                            @Override
                            public void onTick(long millisUntilFinished) {
                                if (sessionDefTime != -1 && playing && sessionStarted) {
                                    views.setTextViewText(R.id.left, "left for session: " + Util.getStringForTime(new StringBuilder(), new Formatter(),
                                            millisUntilFinished));
                                    ((MainActivityViewModel) sharedViewModel).getManager().notify(1, notification);
                                    Log.i("sessionTime", Util.getStringForTime(new StringBuilder(), new Formatter(), millisUntilFinished));
                                }
                            }
                        };
                        ((MainActivityViewModel) sharedViewModel).getSessionStarted().observe(owner, sessionStarted->{
                            if (sessionStarted){
                                timer.start();
                            }else {
                                timer.cancel();
                            }
                        });
                        ((MainActivityViewModel) sharedViewModel).getSessionStarted().setValue(sessionStarted);
                    }
                } else {
                    Toast.makeText(sharedViewModel.getApplication(), "set time in settings first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleTimeBarProgress() {
        controlView.setProgressUpdateListener(new PlayerControlView.ProgressUpdateListener() {
            @Override
            public void onProgressUpdate(long position, long bufferedPosition) {
                timeBar.setPosition(position);
                timePassed.setText(Util.getStringForTime(new StringBuilder(), new Formatter(), position));
                if (viewModel.getLastTime() != position){
                    viewModel.setLastTime(position);
                }
            }
        });
    }

    private void setMediaItemsForPlayer() {
        poolExecutor.execute(() -> {
            try {
                if (history == -1) {
                    if (albumDirExists) {
                        setRandomSong();
                        MediaItem mediaItem = mediaItems.get(random);
                        setCoverAndBackGround(mediaItem, mediaItem.requestMetadata.mediaUri);
                        Log.i("from setMediaItem time is: ", "" + time);
                        if (viewModel.getLastTime() == 0 ){
                            viewModel.setLastTime(time);
                        }
                        handler.post(() -> {
                            ((MainActivityViewModel) sharedViewModel).getPlayer().observe(owner, (player)->{
                                if (player != null) {
                                    player.clearMediaItems();
                                    player.setMediaItems(mediaItems, random, viewModel.getLastTime());
                                }
                            });
                        });
                    } else {
                        setRandomSong();
                    }
                } else {
                    MediaItem mediaItem = mediaItems.get(history);
                    handler.post(() -> {
                        player.seekToDefaultPosition(history);
                        player.seekTo(viewModel.getLastTime());
                    });
                }
            } catch (Exception e) {
                if (e.getMessage() != null){
                    Log.e("PlayerView", e.getMessage());
                }
                if (e.getStackTrace() != null){
                    for (int i = 0; i < e.getStackTrace().length; i++) {
                        Log.e("PlayerView", e.getStackTrace()[i].getMethodName()+" "+e.getStackTrace()[i].getLineNumber());
                    }
                }
                Log.e("PlayerView", e.toString());
            }
        });
    }

    private void setRandomSong() throws Exception {
        File historyFile = new File(sharedViewModel.getApplication().getExternalFilesDir("").getPath() + "/history");
        if (albumDirExists) {
            MediaItem randomSong;
            if (historyFile.exists()) {
                handler.post(() -> {
                    ((View) getParent()).findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
                    setVisibility(View.VISIBLE);
                });
                FileInputStream inputStream = new FileInputStream(historyFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) {
                    builder.append(s);
                }
                JSONObject jsonObject = new JSONObject(builder.toString());
                random = jsonObject.getInt("index");
                history = jsonObject.getInt("index");
                randomSong = mediaItems.get(random);
                time = jsonObject.getLong("time");
                inputStream.close();
            } else {
                handler.post(() -> {
                    ((View) getParent()).findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
                    setVisibility(View.VISIBLE);
                });
                Random random1 = new Random();
                random = ((MainActivityViewModel) sharedViewModel).getRandom() == -1 ? random1.nextInt(mediaItems.size()) :
                ((MainActivityViewModel) sharedViewModel).getRandom();
                ((MainActivityViewModel) sharedViewModel).setRandom(random);
                randomSong = mediaItems.get(random);
            }
        } else {
            Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(sharedViewModel.getApplication()
                            .getPackageName()).
                    appendPath(R.raw.try4 + "").build();
            MediaItem mediaItem = MediaItem.fromUri(uri);
            handler.post(() -> {
                ((View) getParent()).findViewById(R.id.loadingIndicator).setVisibility(View.GONE);
                setVisibility(View.VISIBLE);
                ((MainActivityViewModel) sharedViewModel).getPlayer().observe(owner, player->{
                    if (player != null && !albumDirExists){
                        player.setMediaItem(mediaItem, viewModel.getLastTime());
                        player.prepare();
                    }
                });
            });
        }
    }

    public void setAlbumDirExists(boolean albumDirExists) {
        this.albumDirExists = albumDirExists;
    }

    public void setMediaItems(ArrayList<MediaItem> mediaItems) {
        this.mediaItems = mediaItems;
    }

    public void setItemModels(ArrayList<ItemModel> itemModels) {
        this.itemModels = itemModels;
    }

    @SuppressLint("RestrictedApi")
    public MyPlayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //set player for this view and control view
        setUseController(false);

        //sets player
        if (player != null) {
            setMediaItemsForPlayer();
            setListenersForButs();
            handleTimeBarProgress();
        }
    }

    public MyPlayerView(Context context) {
        super(context);
    }

    @Nullable
    @Override
    public Player getPlayer() {
        return player;
    }

    public void recreate(NotificationManager manager, NotificationChannel channel) {
        this.manager = manager;
        player = ((MainActivityViewModel) sharedViewModel).getPlayer().getValue();
        history = -1;
        controlView.setPlayer(player);
        ((View) getParent()).findViewById(R.id.loadingIndicator).setVisibility(View.VISIBLE);
        setVisibility(View.GONE);
        setMediaItemsForPlayer();
        setListenersForButs();
        handleTimeBarProgress();
    }

    public void setAppBarLayout(AppBarLayout appBarLayout) {
        this.appBarLayout = appBarLayout;
    }

    public void setTabLayout(TabLayout tabLayout) {
        this.tabLayout = tabLayout;
        if (tabLayout == null){
            ((MainActivityViewModel) sharedViewModel).getIsMainActive().setValue(false);
        }
    }

    public void setCoordinatorLayout(CoordinatorLayout coordinatorLayout) {
        this.coordinatorLayout = coordinatorLayout;
    }

    public void setPlayer(MediaController player) {
        this.player = player;
    }

    public void setAll(ControllerLayoutBinding controllerLayoutBinding) {
        PlayerLayoutBinding playerLayoutBinding = controllerLayoutBinding.controller;
        ControllerLayout2Binding controllerLayout2Binding = ControllerLayout2Binding.bind(playerLayoutBinding.getRoot());
        controlView = playerLayoutBinding.controller;
        pausedBut = controllerLayout2Binding.play;
        saveBut = controllerLayout2Binding.saveBut;
        nextBut = controllerLayout2Binding.seekForward;
        previousBut = controllerLayout2Binding.seekBac;
        volBut = controllerLayout2Binding.volMax;
        timeBar = controllerLayout2Binding.timeLine;
        histBut = controllerLayout2Binding.history;
        sessionBut = controllerLayout2Binding.sessionStart;
        timePassed = controllerLayout2Binding.timePassed;
        totalTime = controllerLayout2Binding.totalTime;
        title = controllerLayoutBinding.title;
        cover = controllerLayoutBinding.cover;
        background = controllerLayoutBinding.background;
        manager = ((MainActivityViewModel) sharedViewModel).getManager();
        views = new RemoteViews(sharedViewModel.getApplication().getPackageName(), R.layout.notification_layout);
        builder = new Notification.Builder(sharedViewModel.getApplication(), "sessionNotify").setSmallIcon(R.mipmap.ic_launcher)
                .setCustomBigContentView(views).setOngoing(true).setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText("Quran");
        }
        endNotify = new Notification.Builder(sharedViewModel.getApplication(), "endNotify")
                .setSmallIcon(R.mipmap.ic_launcher).setContentText("Time ended");
        ((MainActivityViewModel) sharedViewModel).getItemModels().observe(owner, (itemModels)->{
            setItemModels(itemModels);
        });
        ((MainActivityViewModel) sharedViewModel).getMediaItems().observe(owner, (items)->{
            setMediaItems(items);
            recreate(((MainActivityViewModel) sharedViewModel).getManager(), null);
        });
    }

    public void setSharedViewModel(AndroidViewModel sharedViewModel) {
        this.sharedViewModel = sharedViewModel;
    }
    public void setViewModel(PlayerFragmentViewModel viewModel){
        this.viewModel = viewModel;
    }

    public void setOwner(LifecycleOwner owner) {
        this.owner = owner;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

}
