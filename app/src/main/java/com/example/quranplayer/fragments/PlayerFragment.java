package com.example.quranplayer.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.quranplayer.R;
import com.example.quranplayer.customviews.MyPlayerView;
import com.example.quranplayer.QuranPlayerService;
import com.example.quranplayer.databinding.ControllerLayout2Binding;
import com.example.quranplayer.databinding.ControllerLayoutBinding;
import com.example.quranplayer.databinding.PlayerFragmentBinding;
import com.example.quranplayer.databinding.PlayerLayoutBinding;
import com.example.quranplayer.datamodels.ItemModel;
import com.example.quranplayer.viewmodels.MainActivityViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.Formatter;

@UnstableApi
public class PlayerFragment extends Fragment {
    private MyPlayerView myPlayerView;
    private AppBarLayout appBarLayout;
    private TabLayout tabLayout;
    private CoordinatorLayout coordinatorLayout;
    private MediaController player;
    private Context context;
    private PlayerFragmentBinding playerFragmentBinding;
    private boolean albumDirExists;
    private ArrayList<MediaItem> mediaItems;
    private ArrayList<ItemModel> itemModels;
    private NotificationChannel channel;
    private NotificationManager manager;
    private AndroidViewModel sharedViewModel;
    private PlayerLayoutBinding playerLayoutBinding;
    private ControllerLayout2Binding controllerLayout2Binding;
    private ControllerLayoutBinding controllerLayoutBinding;

    public PlayerFragment(Context context, TabLayout tabLayout , AppBarLayout appBarLayout, CoordinatorLayout coordinatorLayout,
                          boolean albumDirExists, ArrayList<MediaItem> mediaItems, ArrayList<ItemModel> itemModels, NotificationManager manager
            , NotificationChannel channel) {
        this.appBarLayout = appBarLayout;
        this.tabLayout = tabLayout;
        this.coordinatorLayout = coordinatorLayout;
        this.context = context;
        this.albumDirExists = albumDirExists;
        this.mediaItems = mediaItems;
        this.itemModels = itemModels;
        this.channel = channel;
        this.manager = manager;
    }

    public PlayerFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = ViewModelProvider.create(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(),
                getDefaultViewModelCreationExtras()).get(MainActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        playerFragmentBinding = PlayerFragmentBinding.inflate(getLayoutInflater(), container, false);
        controllerLayoutBinding = ControllerLayoutBinding.bind(playerFragmentBinding.getRoot());
        playerLayoutBinding = controllerLayoutBinding.controller;
        controllerLayout2Binding = ControllerLayout2Binding.bind(playerLayoutBinding.getRoot());
        return playerFragmentBinding.getRoot();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        myPlayerView = playerFragmentBinding.playerView;
        myPlayerView.setPlayerFragmentViewModel(sharedViewModel);
        myPlayerView.setOwner(requireActivity());
        myPlayerView.setAll(ControllerLayoutBinding.bind(playerFragmentBinding.getRoot()));
        myPlayerView.setTabLayout(tabLayout);
        myPlayerView.setAppBarLayout(appBarLayout);
        myPlayerView.setCoordinatorLayout(coordinatorLayout);
        ((MainActivityViewModel) sharedViewModel).getAlbumDirExists().observe(requireActivity(), (exists)->{
            myPlayerView.setAlbumDirExists(exists);
        });
        ((MainActivityViewModel) sharedViewModel).getItemModels().observe(requireActivity(), (itemModels)->{
            if (!itemModels.isEmpty()){
                myPlayerView.setItemModels(itemModels);
            }
        });
        ((MainActivityViewModel) sharedViewModel).getMediaItems().observe(requireActivity(), (mediaItems)->{
            if (!mediaItems.isEmpty()){
                myPlayerView.setMediaItems(mediaItems);
            }
        });
        ((MainActivityViewModel) sharedViewModel).getIsItemModelsAndMediaItemsFull().observe(requireActivity(), (full)->{
            if (full){
                myPlayerView.setAll(ControllerLayoutBinding.bind(playerFragmentBinding.getRoot()));
                myPlayerView.setMediaItems(((MainActivityViewModel) sharedViewModel).getMediaItems().getValue());
                myPlayerView.setPlayer(((MainActivityViewModel) sharedViewModel).getPlayer().getValue());
                myPlayerView.recreate(manager, channel);
            }
        });
    }

    public void recreate(){
        playerFragmentBinding.playerView.setAlbumDirExists(albumDirExists);
        playerFragmentBinding.playerView.setMediaItems(mediaItems);
        playerFragmentBinding.playerView.setItemModels(itemModels);
        playerFragmentBinding.playerView.recreate(manager, channel);
    }

    public MediaController getPlayer() {
        return player;
    }

    @Override
    public void onStart() {
        super.onStart();
        SessionToken sessionToken = new SessionToken(sharedViewModel.getApplication().getApplicationContext()
                , new ComponentName(sharedViewModel.getApplication(), QuranPlayerService.class));
        ListenableFuture<MediaController> controllerListenableFuture = new MediaController.Builder(sharedViewModel.getApplication().getApplicationContext()
                , sessionToken).buildAsync();
        controllerListenableFuture.addListener(() -> {
            try {
                player = (controllerListenableFuture.get());
                addPlayerListener();
                ((MainActivityViewModel) sharedViewModel).getPlayer().setValue(player);
                android.util.Log.e("connect", ""+player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM));
                player.prepare();
                player.pause();
                myPlayerView.setPlayer(player);
                myPlayerView.recreate(manager, channel);
            } catch (Exception e) {
                Log.e("onStartPlayerFragment", e.toString());
            }
        }, MoreExecutors.directExecutor());
    }


    public void setAlbumDirExists(boolean albumDirExists) {
        this.albumDirExists = albumDirExists;
    }

    public void setItemModels(ArrayList<ItemModel> itemModels) {
        this.itemModels = itemModels;
    }

    public void setMediaItems(ArrayList<MediaItem> mediaItems) {
        this.mediaItems = mediaItems;
    }

    private void addPlayerListener() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                switch (playbackState) {
                    case Player.STATE_READY:
                        controllerLayout2Binding.timeLine.setDuration(player.getContentDuration());
                        controllerLayout2Binding.totalTime.setText(Util.getStringForTime(new StringBuilder(), new Formatter(), player.getDuration()));
                        break;
                    case Player.STATE_ENDED:
                        android.util.Log.i("PlaybackState", "" + playbackState);
                        break;
                    default:
                        android.util.Log.i("PlaybackState", "another state " + playbackState);
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                switch (reason) {
                    case ExoPlayer.MEDIA_ITEM_TRANSITION_REASON_SEEK:
                    case ExoPlayer.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED:
                    case ExoPlayer.MEDIA_ITEM_TRANSITION_REASON_AUTO:
                        if (mediaItem != null) {
                            controllerLayout2Binding.timeLine.setDuration(player.getContentDuration());
                            controllerLayout2Binding.totalTime.setText(Util.getStringForTime(new StringBuilder(), new Formatter(), player.getDuration()));
                            myPlayerView.setCoverAndBackGround(mediaItem, mediaItem.requestMetadata.mediaUri);
                        }
                        break;
                    default:
                        android.util.Log.e("Media Item Transition: ", "another event " + reason + " happened");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                android.util.Log.e("player err", error.toString());
                player.clearMediaItems();
                player.prepare();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                if (isPlaying) {
                    showPlayBut();
                } else {
                    showPauseBut();
                }
            }
        });
    }
    private void showPlayBut() {
        controllerLayout2Binding.play.setIcon(AppCompatResources.getDrawable(sharedViewModel.getApplication(), R.drawable.pause));
        myPlayerView.setPaused(false);
        myPlayerView.setPlaying(true);
    }
    private void showPauseBut() {
        controllerLayout2Binding.play.setIcon(AppCompatResources.getDrawable(sharedViewModel.getApplication(), R.drawable.play));
        myPlayerView.setPaused(true);
        myPlayerView.setPlaying(false);
    }
//    private void setListenersForButs() {
//        controllerLayout2Binding.volMax.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (muted) {
//                    controllerLayout2Binding.volMax.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.volume_max, null));
//                    muted = false;
//                    player.unmute();
//                } else {
//                    controllerLayout2Binding.volMax.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.mute, null));
//                    muted = true;
//                    player.mute();
//                }
//            }
//        });
//        controllerLayout2Binding.play.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                handlePlayPause();
//            }
//        });
//        controllerLayout2Binding.timeLine.addListener(new TimeBar.OnScrubListener() {
//            @Override
//            public void onScrubStart(TimeBar timeBar, long position) {
//                player.seekTo(position);
//            }
//
//            @Override
//            public void onScrubMove(TimeBar timeBar, long position) {
//                player.seekTo(position);
//            }
//
//            @Override
//            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
//                player.seekTo(position);
//            }
//        });
//        controllerLayout2Binding.seekForward.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                android.util.Log.e("nextBut", "" + player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM));
//                paused = true;
//                playing = false;
//                ended = false;
//                if (player.hasNextMediaItem()) {
//                    player.seekToNextMediaItem();
//                    android.util.Log.e("nextBut", "yes next");
//                } else {
//                    android.util.Log.e("nextBut", "no next");
//                }
//                if (player.getCurrentMediaItemIndex() == history) {
//                    player.seekTo(time);
//                }
//            }
//        });
//        controllerLayout2Binding.seekForward.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        controllerLayout2Binding.seekForward.animate().scaleX(0.83f).scaleY(0.83f).setDuration(100).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                                controllerLayout2Binding.seekForward.setScaleX(0.85f);
//                                controllerLayout2Binding.seekForward.setScaleY(0.85f);
//                                controllerLayout2Binding.seekForward.clearAnimation();
//                            }
//                        }).start();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                    case MotionEvent.ACTION_CANCEL:
//                        controllerLayout2Binding.seekForward.animate().scaleX(1f).scaleY(1f).setDuration(100).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                                controllerLayout2Binding.seekForward.clearAnimation();
//                            }
//                        }).start();
//                        android.util.Log.d("nextButTouch", "up successful");
//                        break;
//                    default:
//                        android.util.Log.d("nextButTouch", "event is " + event.getAction());
//                        break;
//                }
//                return false;
//            }
//        });
//        controllerLayout2Binding.seekBac.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                paused = true;
//                playing = false;
//                ended = false;
//                player.seekToPreviousMediaItem();
//                if (player.getCurrentMediaItemIndex() == history) {
//                    player.seekTo(time);
//                }
//            }
//        });
//        controllerLayout2Binding.seekBac.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        controllerLayout2Binding.seekBac.animate().scaleX(0.83f).scaleY(0.83f).setDuration(100).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                                controllerLayout2Binding.seekBac.setScaleX(0.85f);
//                                controllerLayout2Binding.seekBac.setScaleY(0.85f);
//                                controllerLayout2Binding.seekBac.clearAnimation();
//                            }
//                        }).start();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                    case MotionEvent.ACTION_CANCEL:
//                        controllerLayout2Binding.seekBac.animate().scaleX(1f).scaleY(1f).setDuration(100).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                                controllerLayout2Binding.seekBac.clearAnimation();
//                            }
//                        }).start();
//                        android.util.Log.d("preButTouch", "up successful");
//                        break;
//                    default:
//                        android.util.Log.d("preButTouch", "event is " + event.getAction());
//                        break;
//                }
//                return false;
//            }
//        });
//        controllerLayout2Binding.saveBut.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                controllerLayout2Binding.saveBut.setIcon(ResourcesCompat.getDrawable(sharedViewModel.getApplication().getResources(), R.drawable.save_success, null));
//                File historyFile = new File(sharedViewModel.getApplication().getExternalFilesDir("").getPath() + "/history");
//                try {
//                    if (player.getCurrentMediaItem() != null) {
//                        android.util.Log.i("saveBut Clicked", "mediaId: " + player.getCurrentMediaItem().mediaId);
//                        boolean delete = historyFile.delete();
//                        delete = historyFile.createNewFile();
//                        FileOutputStream outputStream = new FileOutputStream(historyFile);
//                        JSONObject jsonObject = new JSONObject();
//                        int index = Integer.parseInt(player.getCurrentMediaItem().mediaId);
//                        jsonObject.put("index", index);
//                        jsonObject.put("time", player.getCurrentPosition());
//                        time = player.getCurrentPosition();
//                        history = index;
//                        outputStream.write(jsonObject.toString().getBytes());
//                        outputStream.close();
//                    }
//                    poolExecutor.execute(() -> {
//                        try {
//                            Thread.sleep(1000);
//                            handler.post(() -> {
//                                controllerLayout2Binding.saveBut.animate().alpha(0f).setDuration(2000).setListener(new AnimatorListenerAdapter() {
//                                    @Override
//                                    public void onAnimationEnd(Animator animation) {
//                                        super.onAnimationEnd(animation);
//                                        controllerLayout2Binding.saveBut.setIcon(ResourcesCompat.getDrawable(playerFragmentViewModel.getApplication()
//                                                .getResources(), R.drawable.save, null));
//                                        controllerLayout2Binding.saveBut.animate().alphaBy(1).setDuration(2000).setListener(new AnimatorListenerAdapter() {
//                                            @Override
//                                            public void onAnimationEnd(Animator animation) {
//                                                super.onAnimationEnd(animation);
//                                                controllerLayout2Binding.saveBut.clearAnimation();
//                                            }
//                                        }).start();
//                                    }
//                                }).start();
//                            });
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
//                } catch (Exception e) {
//                    android.util.Log.e("saveButClick", e.toString());
//                }
//            }
//        });
//        controllerLayout2Binding.history.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                player.pause();
//                random = history;
//                setMediaItemsForPlayer();
//                controllerLayout2Binding.history.animate().rotationBy(-360).setDuration(1000).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        controllerLayout2Binding.history.clearAnimation();
//                    }
//                }).start();
//            }
//        });
//        controllerLayout2Binding.sessionStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                File defTime = new File(sharedViewModel.getApplication().getExternalFilesDir("").getPath() + "/defTime");
//                if (defTime.exists()) {
//                    poolExecutor.execute(() -> {
//                        FileInputStream inputStream;
//                        try {
//                            inputStream = new FileInputStream(defTime);
//                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                            StringBuilder builder = new StringBuilder();
//                            String s;
//                            while ((s = reader.readLine()) != null) {
//                                builder.append(s);
//                            }
//                            sessionDefTime = Integer.parseInt(builder.toString());
//                            inputStream.close();
//                            reader.close();
//
//                        } catch (Exception e) {
//                            android.util.Log.e("sessionBut", e.toString());
//                        }
//                    });
//                    if (sessionStarted) {
//                        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
//                        sessionStarted = false;
//                        startCounting = false;
//                    } else {
//                        AnimatedVectorDrawable vectorDrawable = ((AnimatedVectorDrawable) controllerLayout2Binding.sessionStart.getIcon());
//                        vectorDrawable.start();
//                        sessionStarted = true;
//                        startCounting = true;
//                        player.play();
//                    }
//                } else {
//                    Toast.makeText(sharedViewModel.getApplication(), "set time in settings first", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
}
