package com.example.quranplayer.viewmodels;

import androidx.lifecycle.ViewModel;

public class PlayerFragmentViewModel extends ViewModel {
    private boolean playing = false;
    private long lastTime = 0;

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
}
