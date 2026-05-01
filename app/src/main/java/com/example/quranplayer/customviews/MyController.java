package com.example.quranplayer.customviews;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerControlView;

@UnstableApi
public class MyController extends PlayerControlView {
    public MyController(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyController(Context context) {
        super(context);
    }

    @Override
    public void hide() {

    }

    @Override
    public void hideImmediately() {
    }

    @Override
    public int getShowTimeoutMs() {
        return 0;
    }
}
