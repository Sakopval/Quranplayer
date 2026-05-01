package com.example.quranplayer.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.inspector.MetadataRetriever;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.quranplayer.QuranPlayerService;
import com.example.quranplayer.customviews.MyPlayerView;
import com.example.quranplayer.R;
import com.example.quranplayer.databinding.FileItemBinding;
import com.example.quranplayer.datamodels.ItemModel;
import com.example.quranplayer.viewmodels.MainActivityViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

@UnstableApi
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private static int indexPressed;
    private static Fragment playerFragment;
    private final LifecycleOwner owner;
    private ViewHolder viewHolder;
    private final ArrayList<MediaItem> mediaItems;
    private final ArrayList<ItemModel> itemModels;
    private MainActivityViewModel viewModel;

    public RecyclerAdapter(Fragment playerView, ArrayList<MediaItem> mediaItems, ArrayList<ItemModel> itemModels,
                           LifecycleOwner owner, MainActivityViewModel viewModel) {
        playerFragment = playerView;
        this.itemModels = itemModels;
        this.mediaItems = mediaItems;
        this.owner = owner;
        this.viewModel = viewModel;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final int COLOR_CONST = Color.argb(255, 0, 5, 5);
        private FileItemBinding fileItemBinding;
        private ArrayList<ItemModel> itemModels;
        private ArrayList<MediaItem> mediaItems;

        public ViewHolder(@NonNull FileItemBinding binding, ArrayList<ItemModel> itemModels, ArrayList<MediaItem> mediaItems,
                          MainActivityViewModel viewModel, LifecycleOwner owner) {
            super(binding.getRoot());
            this.fileItemBinding = binding;
            this.itemModels = itemModels;
            this.mediaItems = mediaItems;
            ((MaterialCardView) fileItemBinding.cardItem).setBackgroundTintList(ColorStateList.valueOf(COLOR_CONST));
            if (!fileItemBinding.cardItem.hasOnClickListeners()) {
                fileItemBinding.cardItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        indexPressed = getAbsoluteAdapterPosition();
                        viewModel.getPlayer().observe(owner, (player)->{
                            if (player != null) {
                                boolean isAudio = MediaFormatUtil.isAudioFormat(MediaFormat.createAudioFormat(itemModels.get(indexPressed).getType(), 0, 0));
                                if (isAudio) {
                                    Log.i("MimeType", isAudio + "");
                                    player.seekToDefaultPosition(indexPressed);
                                    viewModel.setRandom(indexPressed);
                                    viewModel.getPageIndex().setValue(0);
                                } else {
                                    Toast.makeText(viewModel.getApplication(), "is not an audio", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FileItemBinding binding = FileItemBinding.inflate(LayoutInflater.from(viewModel.getApplication()), parent, false);
        ViewHolder viewHolder = new ViewHolder(binding, itemModels, mediaItems, viewModel, owner);
        this.viewHolder = viewHolder;
        return viewHolder;
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewModel.getColor2().observe(owner, (color2)->{
            holder.fileItemBinding.fileName.setTextColor(color2);
        });
        viewModel.getColor1().observe(owner, (color1)->{
            holder.fileItemBinding.artist.setTextColor(color1);
        });
        holder.fileItemBinding.fileName.setText(itemModels.get(holder.getAbsoluteAdapterPosition()).getName());
        holder.fileItemBinding.coverImage.setImageBitmap(itemModels.get(holder.getAbsoluteAdapterPosition()).getImage());
        holder.fileItemBinding.artist.setText(itemModels.get(holder.getAbsoluteAdapterPosition()).getArtist());
    }


    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int getItemCount() {
        Log.d("RecyclerAdapter", itemModels.size() + "");
        return itemModels.size();
    }

    private Metadata getMetaData(MediaItem mediaItem) throws Exception{
        MetadataRetriever retriever = new MetadataRetriever.Builder(viewModel.getApplication(), mediaItem).build();
        ListenableFuture<TrackGroupArray> trackGroupArray = retriever.retrieveTrackGroups();
        Metadata metadata = trackGroupArray.get().get(0).getFormat(0).metadata;
        return metadata;
    }
}
