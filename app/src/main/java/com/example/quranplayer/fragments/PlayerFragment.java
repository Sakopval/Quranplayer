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
import androidx.media3.common.Timeline;
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
import com.example.quranplayer.viewmodels.MainActivityViewModel;
import com.example.quranplayer.viewmodels.PlayerFragmentViewModel;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

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
    private NotificationChannel channel;
    private NotificationManager manager;
    private AndroidViewModel sharedViewModel;
    private PlayerLayoutBinding playerLayoutBinding;
    private ControllerLayout2Binding controllerLayout2Binding;
    private ControllerLayoutBinding controllerLayoutBinding;
    private PlayerFragmentViewModel viewModel;

    public PlayerFragment(Context context, TabLayout tabLayout , AppBarLayout appBarLayout, CoordinatorLayout coordinatorLayout, NotificationManager manager
            , NotificationChannel channel) {
        this.appBarLayout = appBarLayout;
        this.tabLayout = tabLayout;
        this.coordinatorLayout = coordinatorLayout;
        this.context = context;
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
        viewModel = new ViewModelProvider(this).get(PlayerFragmentViewModel.class);
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
        myPlayerView.setSharedViewModel(sharedViewModel);
        myPlayerView.setViewModel(viewModel);
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
                if (!viewModel.isPlaying()){
                    player.pause();
                }
                viewModel.setLastTime(player.getCurrentPosition());
                myPlayerView.setPlayer(player);
                myPlayerView.recreate(manager, channel);
            } catch (Exception e) {
                Log.e("onStartPlayerFragment", e.toString());
            }
        }, MoreExecutors.directExecutor());
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
        viewModel.setPlaying(true);
    }
    private void showPauseBut() {
        controllerLayout2Binding.play.setIcon(AppCompatResources.getDrawable(sharedViewModel.getApplication(), R.drawable.play));
        myPlayerView.setPaused(true);
        myPlayerView.setPlaying(false);
        viewModel.setPlaying(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
