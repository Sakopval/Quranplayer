package com.example.quranplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.legacy.PlaybackStateCompat;

import com.google.common.collect.ImmutableList;

@SuppressLint("RestrictedApi")
public class QuranPlayerService extends MediaSessionService {
    private static final String MY_MEDIA_ROOT_ID = "QURAN_PLAYER";
    private static final String TAG = "QuranSession";
    private ExoPlayer player;
    private MediaSession mediaSession;
    private PlaybackStateCompat playbackStateCompat;
    Notification.MediaStyle notification;

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(getApplicationContext())
                .setAudioAttributes(new AudioAttributes.Builder().build(), true)
                .setWakeMode(PowerManager.PARTIAL_WAKE_LOCK).build();
        CommandButton commandButton = new CommandButton.Builder(CommandButton.ICON_NEXT).setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).build();
        MediaSession.Callback callback = new MediaSession.Callback() {
            @Override
            public MediaSession.ConnectionResult onConnect(MediaSession session, MediaSession.ControllerInfo controller) {
                Player.Commands commands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon().addAllCommands()
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM).build();
                Log.d(TAG, "session connected");
                return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(commands).build();
            }
        };
        mediaSession = new MediaSession.Builder(getApplicationContext(), player)
                .setCommandButtonsForMediaItems(ImmutableList.of(commandButton)).setCallback(callback).build();
        Log.d(TAG, "from Service");
    }

    @Override
    public void onDestroy() {
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        stopSelf();
        super.onDestroy();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        if (controllerInfo.isPackageNameVerified()) {
            return mediaSession;
        }
        return null;
    }
}
