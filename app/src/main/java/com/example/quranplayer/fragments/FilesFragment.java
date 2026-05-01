package com.example.quranplayer.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.quranplayer.MainActivity;
import com.example.quranplayer.adapters.RecyclerAdapter;
import com.example.quranplayer.databinding.FilesFragmentBinding;
import com.example.quranplayer.datamodels.ItemModel;
import com.example.quranplayer.viewmodels.MainActivityViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

@UnstableApi
public class FilesFragment extends Fragment {
    private static boolean found = false;
    private Context context;
    private Activity main;
    private LifecycleOwner owner;
    private final ThreadPoolExecutor poolExecutor = MainActivity.poolExecutor;
    private static final Handler handler = MainActivity.handler;
    private static File exFilesDir;
    private Button addFolderBut;
    private RecyclerView recyclerView;
    private Fragment playerFragment;
    private ViewPager2 pager2;
    private View fragmentView;
    private Bundle savedInstance;
    private FilesFragmentBinding filesFragmentBinding;
    private boolean albumDirExists;
    private ArrayList<MediaItem> mediaItems;
    private ArrayList<ItemModel> itemModels;
    private boolean recreating;
    private MainActivityViewModel sharedViewModel;

    public FilesFragment(Context context, Activity main, Fragment playerView, ViewPager2 pager2,
                         boolean albumDirExists, ArrayList<ItemModel> itemModels, ArrayList<MediaItem> mediaItems) {
        this.context = context;
        this.main = main;
        exFilesDir = context.getExternalFilesDir("");
        this.playerFragment = playerView;
        this.pager2 = pager2;
        this.albumDirExists = albumDirExists;
        this.itemModels = itemModels;
        this.mediaItems = mediaItems;
    }

    public FilesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = ViewModelProvider.create(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(),
                getDefaultViewModelCreationExtras()).get(MainActivityViewModel.class);
        owner = requireActivity();
        main = ((MainActivity) getActivity());
    }

    public void setRecyclerAdapter() {
        try {
            sharedViewModel.getAlbumDirExists().observe(requireActivity(), (exists) -> {
                if (exists) {
                    recyclerView.setVisibility(View.GONE);
                    addFolderBut.setVisibility(View.GONE);
                    filesFragmentBinding.loadingIndicator.setVisibility(View.VISIBLE);
                    RecyclerAdapter recyclerAdapter = new RecyclerAdapter(playerFragment,
                            sharedViewModel.getMediaItems().getValue(), sharedViewModel.getItemModels().getValue(),
                            owner, sharedViewModel);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(recyclerAdapter);
                    filesFragmentBinding.loadingIndicator.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            if (e.getMessage() != null)
                Log.e("settingRecAdapt", e.getMessage());
            else
                Log.e("settingRecAdapt", e.toString());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        filesFragmentBinding = FilesFragmentBinding.inflate(inflater, container, false);
        return filesFragmentBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.fragmentView = view;
        this.savedInstance = savedInstanceState;
        addFolderBut = filesFragmentBinding.addFolderBut;
        recyclerView = filesFragmentBinding.filesRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setHasFixedSize(true);
        recyclerView.setRecycledViewPool(new RecyclerView.RecycledViewPool());
        setRecyclerAdapter();
        //set onClickListener for addFolderBut
        addFolderBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!found) {
                    poolExecutor.execute(() -> {
                        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        picker.addCategory(Intent.CATEGORY_DEFAULT);
                        main.startActivityForResult(picker, 20);
                    });
                }
            }
        });
        sharedViewModel.getItemModels().observe(requireActivity(), (itemModels) -> {
            Log.i("itemModelsChanged", !itemModels.isEmpty() + "");
            if (!itemModels.isEmpty()) {
                setRecyclerAdapter();
            }
        });
    }

    public void recreate() {
        recreating = true;
        setRecyclerAdapter();
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

    public void setMain(Activity main) {
        this.main = main;
    }
}
