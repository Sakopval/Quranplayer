package com.example.quranplayer.datamodels;

import android.graphics.Bitmap;
import android.net.Uri;

public class ItemModel {
    private String name;
    private String artist;
    private Bitmap image;
    private Uri uri;
    private String type;

    public ItemModel(String artist, Bitmap image, String name) {
        this.artist = artist;
        this.image = image;
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
