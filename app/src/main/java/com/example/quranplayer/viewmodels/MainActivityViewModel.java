package com.example.quranplayer.viewmodels;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaFormat;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.inspector.MetadataRetriever;

import com.example.quranplayer.R;
import com.example.quranplayer.datamodels.ItemModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivityViewModel extends AndroidViewModel {
    private final Context context;
    private NotificationManager manager;
    private NotificationChannel channel;
    private NotificationChannel channel1;
    private int random = -1;
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isMainActive = new MutableLiveData<>();
    private final MutableLiveData<Boolean> albumDirExists = new MutableLiveData<>(false);
    private final MutableLiveData<ArrayList<MediaItem>> mediaItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ArrayList<ItemModel>> itemModels = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> color1 = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> color2 = new MutableLiveData<>(0);
    private final MutableLiveData<GradientDrawable> gradientDrawable = new MutableLiveData<>(null);
    private final MutableLiveData<Player> player = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isItemModelsAndMediaItemsFull = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> pageIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<Boolean> sessionStarted = new MutableLiveData<>(false);
    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        context = application.getApplicationContext();
    }

    public Context getContext() {
        return context;
    }

    public NotificationManager getManager() {
        return manager;
    }

    public void setManager(NotificationManager manager) {
        this.manager = manager;
    }

    public NotificationChannel getChannel1() {
        return channel1;
    }

    public void setChannel1(NotificationChannel channel1) {
        this.channel1 = channel1;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public MutableLiveData<Boolean> getIsMainActive() {
        return isMainActive;
    }

    public MutableLiveData<ArrayList<MediaItem>> getMediaItems() {
        return mediaItems;
    }

    public MutableLiveData<ArrayList<ItemModel>> getItemModels() {
        return itemModels;
    }

    public MutableLiveData<Boolean> getAlbumDirExists() {
        return albumDirExists;
    }

    public MutableLiveData<Integer> getPageIndex() {
        return pageIndex;
    }

    @OptIn(markerClass = UnstableApi.class)
    public void checkAndSortFiles(File albumDir) throws Exception {
        if (albumDir.exists()) {
            albumDirExists.postValue(true);
            setItems(albumDir);
            Collections.sort(itemModels.getValue(), (a, b) -> a.getName().compareTo(b.getName()));
            for (ItemModel itemModel : itemModels.getValue()) {
                MediaItem mediaItem = new MediaItem.Builder().setUri(itemModel.getUri()).setMediaId(itemModels.getValue().indexOf(itemModel) + "").
                        setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(getMetaData(MediaItem.fromUri(itemModel.getUri()))).build())
                        .build();
                mediaItems.getValue().add(mediaItem);
            }
        } else {
            albumDirExists.postValue(false);
        }
        isItemModelsAndMediaItemsFull.postValue(!itemModels.getValue().isEmpty() && !mediaItems.getValue().isEmpty());
    }

    @OptIn(markerClass = UnstableApi.class)
    public ArrayList<ItemModel> setItems(File albumDir) throws Exception {
        Bitmap bitmap;
        String title;
        String artist;
        DocumentFile[] filesSorted = getDocumentFiles(albumDir);
        for (int i = 0; i < filesSorted.length; i++) {
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
                        bitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.quran_image);
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
                    bitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.quran_image);
                    artist = "unknown";
                }
                //initialize object
                ItemModel itemModel = new ItemModel(artist, bitmap, title);
                itemModel.setUri(filesSorted[i].getUri());
                itemModel.setType(filesSorted[i].getType());
                itemModels.getValue().add(itemModel);
            }
        }
        return itemModels.getValue();
    }

    @OptIn(markerClass = UnstableApi.class)
    private Metadata getMetaData(MediaItem mediaItem) throws Exception {
        MetadataRetriever retriever = new MetadataRetriever.Builder(this.getContext(), mediaItem).build();
        ListenableFuture<TrackGroupArray> trackGroupArray = retriever.retrieveTrackGroups();
        Metadata metadata = trackGroupArray.get().get(0).getFormat(0).metadata;
        return metadata;
    }

    public MutableLiveData<Integer> getColor1() {
        return color1;
    }

    public MutableLiveData<Integer> getColor2() {
        return color2;
    }

    public MutableLiveData<GradientDrawable> getGradientDrawable() {
        return gradientDrawable;
    }

    public MutableLiveData<Boolean> getConnected() {
        return connected;
    }

    public MutableLiveData<Player> getPlayer() {
        return player;
    }

    public MutableLiveData<Boolean> getSessionStarted() {
        return sessionStarted;
    }

    public MutableLiveData<Boolean> getIsItemModelsAndMediaItemsFull() {
        return isItemModelsAndMediaItemsFull;
    }

    public int getRandom() {
        return random;
    }

    public void setRandom(int random) {
        this.random = random;
    }
    private DocumentFile[] getDocumentFiles(File albumDir)throws Exception{
        FileInputStream inputStream = new FileInputStream(albumDir);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder uri = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null) {
            uri.append(s);
        }
        DocumentFile file = DocumentFile.fromTreeUri(this.getContext(), Uri.parse(uri.toString()));
        DocumentFile[] files = file.listFiles();
        return files;
    }
}
