package com.example.quranplayer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.MediaFormatUtil;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.inspector.MetadataRetriever;

import com.artifex.mupdf.mini.DocumentActivity;
import com.artifex.mupdf.mini.DocumentActivityViewModel;
import com.example.quranplayer.databinding.DialogLayoutBinding;
import com.example.quranplayer.databinding.SettingLayoutBinding;
import com.example.quranplayer.fragments.FilesFragment;
import com.example.quranplayer.fragments.PlayerFragment;
import com.example.quranplayer.datamodels.ItemModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;

@UnstableApi
public class SettingsActivity extends AppCompatActivity {
    private final ThreadPoolExecutor poolExecutor = MainActivity.poolExecutor;
    private final Handler handler = MainActivity.handler;
    private ActivityResultLauncher<ActivityResultContracts.OpenDocument> openDocumentActivityResultLauncher;
    private ActivityResultLauncher<ActivityResultContracts.OpenDocumentTree> openDocumentTreeActivityResultLauncher;
    private boolean albumDirExists;
    private final ArrayList<ItemModel> itemModels = new ArrayList<>();
    private final ArrayList<MediaItem> mediaItems = new ArrayList<>();
    private final FilesFragment filesFragment = MainActivity.getFilesFragment();
    private final PlayerFragment playerFragment = MainActivity.getPlayerFragment();
    private final DocumentActivity documentActivity = MainActivity.getDocumentActivity();
    private final FrameLayout frameLayout = MainActivity.getFrameLayout();
    private final LoadingIndicator loadingIndicator = MainActivity.getLoadingIndicator();
    private DialogLayoutBinding dialogLayoutBinding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SettingLayoutBinding settingLayoutBinding = SettingLayoutBinding.inflate(getLayoutInflater());
        openDocumentActivityResultLauncher = registerForActivityResult(new ActivityResultContract<ActivityResultContracts.OpenDocument, Uri>() {
            @Override
            public Uri parseResult(int i, @Nullable Intent intent) {
                if (intent != null) {
                    return intent.getData();
                } else {
                    return null;
                }
            }

            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, ActivityResultContracts.OpenDocument openDocument) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                return intent;
            }
        }, o -> {
            if (o != null) {
                poolExecutor.execute(() -> {
                    File exFilesDir = getExternalFilesDir("");
                    FileOutputStream outputStream;
                    try {
                        File pdfDest = new File(exFilesDir.getPath() + "/pdfDest");
                        if (pdfDest.delete()) {
                            outputStream = new FileOutputStream(pdfDest);
                            assert o != null;
                            int flags = (Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(o, flags);
                            outputStream.write(o.toString().getBytes());
                            outputStream.close();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    handler.post(() -> {
                        documentActivity.setUri(o);
                        documentActivity.setMimetype(DocumentFile.fromSingleUri(getApplicationContext(), o).getType());
                        DocumentActivityViewModel viewModel = new ViewModelProvider(documentActivity).get(DocumentActivityViewModel.class);
                        viewModel.getPdfUri().setValue(o);
                    });
                });
            }
        });
        openDocumentTreeActivityResultLauncher = registerForActivityResult(new ActivityResultContract<ActivityResultContracts.OpenDocumentTree, Uri>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, ActivityResultContracts.OpenDocumentTree openDocumentTree) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                return intent;
            }

            @Override
            public Uri parseResult(int i, @Nullable Intent intent) {
                if (intent != null) {
                    return intent.getData();
                } else {
                    return null;
                }
            }
        }, o -> {
            if (o != null) {
                poolExecutor.execute(() -> {
                    handler.post(() -> {
                        loadingIndicator.setVisibility(View.VISIBLE);
                        frameLayout.setVisibility(View.GONE);
                    });
                    File exFilesDir = getExternalFilesDir("");
                    try {
                        assert exFilesDir != null;
                        File albumDir = new File(exFilesDir.getPath() + "/albumDir");
                        if (albumDir.delete()) {
                            FileOutputStream outputStream = new FileOutputStream(albumDir);
                            int flags = (Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            getContentResolver().takePersistableUriPermission(o, flags);
                            outputStream.write(o.toString().getBytes());
                            outputStream.close();
                            albumDirExists = albumDir.exists();
                            try {
                                checkAndSortFiles(albumDir);
                                MainActivity.setMediaItems(mediaItems);
                                MainActivity.setItemModels(itemModels);
                            } catch (Exception e) {
                                Log.e("checkAndSortRes", e.toString());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("recreatingPlayer", e.toString());
                    }
                    handler.post(() -> {
                        frameLayout.setVisibility(View.VISIBLE);
                        loadingIndicator.setVisibility(View.GONE);
                    });
                });
            }
        });
        setContentView(settingLayoutBinding.getRoot());
        settingLayoutBinding.backBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        settingLayoutBinding.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    if (documentActivity != null) {
                        openDocumentActivityResultLauncher.launch(new ActivityResultContracts.OpenDocument(), ActivityOptionsCompat.makeBasic());
                    } else {
                        Toast.makeText(SettingsActivity.this, "Please wait a little", Toast.LENGTH_SHORT).show();
                    }
                } else if (position == 0) {
                    if (playerFragment != null && filesFragment != null) {
                        openDocumentTreeActivityResultLauncher.launch(new ActivityResultContracts.OpenDocumentTree(), ActivityOptionsCompat.makeBasic());
                    } else {
                        Toast.makeText(SettingsActivity.this, "Please wait a little", Toast.LENGTH_SHORT).show();
                    }
                } else if (position == 2) {
                    setMaterialDialog().show();
                }
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        return super.onContextItemSelected(item);
    }

    private void checkAndSortFiles(File albumDir) throws Exception {
        if (albumDir.exists()) {
            albumDirExists = true;
            FileInputStream inputStream = new FileInputStream(albumDir);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder uri = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                uri.append(s);
            }
            DocumentFile file = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(uri.toString()));
            DocumentFile[] files = file.listFiles();
            setItems(files.length, files);
            Collections.sort(itemModels, (a, b) -> a.getName().compareTo(b.getName()));
            for (ItemModel itemModel : itemModels) {
                MediaItem mediaItem = new MediaItem.Builder().setUri(itemModel.getUri()).setMediaId(itemModels.indexOf(itemModel) + "").setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(getMetaData(MediaItem.fromUri(itemModel.getUri()))).build()).build();
                mediaItems.add(mediaItem);
            }
        } else {
            albumDirExists = false;
        }
    }

    private ArrayList<ItemModel> setItems(int itemCount, DocumentFile[] filesSorted) throws Exception {
        Bitmap bitmap;
        String title;
        String artist;
        for (int i = 0; i < itemCount; i++) {
            if (MediaFormatUtil.isAudioFormat(MediaFormat.createAudioFormat(filesSorted[i].getType(), 0, 0))) {
                MediaItem mediaItem = MediaItem.fromUri(filesSorted[i].getUri());
                Metadata metadata = getMetaData(mediaItem);
                //populate mediaItem with metaData
                if (metadata != null) {
                    mediaItem = new MediaItem.Builder().setUri(filesSorted[i].getUri()).setMediaMetadata(new MediaMetadata.Builder().populateFromMetadata(metadata).build()).build();
                    //get bitmap
                    if (mediaItem.mediaMetadata.artworkData != null) {
                        bitmap = BitmapFactory.decodeByteArray(mediaItem.mediaMetadata.artworkData, 0, mediaItem.mediaMetadata.artworkData.length);
                    } else {
                        bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.quran_image);
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
                    bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.quran_image);
                    artist = "unknown";
                }
                //initialize object
                ItemModel itemModel = new ItemModel(artist, bitmap, title);
                itemModel.setUri(filesSorted[i].getUri());
                itemModel.setType(filesSorted[i].getType());
                itemModels.add(itemModel);
            }
        }
        return itemModels;
    }

    private Metadata getMetaData(MediaItem mediaItem) throws Exception {
        MetadataRetriever retriever = new MetadataRetriever.Builder(getApplicationContext(), mediaItem).build();
        ListenableFuture<TrackGroupArray> trackGroupArray = retriever.retrieveTrackGroups();
        Metadata metadata = trackGroupArray.get().get(0).getFormat(0).metadata;
        return metadata;
    }

    private boolean isAllNums(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AlertDialog setMaterialDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(SettingsActivity.this, R.style.MaterialAlertDialog_Material3_Mine);
        dialogLayoutBinding = DialogLayoutBinding.inflate(getLayoutInflater());
        dialogBuilder.setView(dialogLayoutBinding.getRoot());
        AlertDialog alertDialog = dialogBuilder.create();
        dialogLayoutBinding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        dialogLayoutBinding.ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = dialogLayoutBinding.textInputLayout.getEditText().getText().toString();
                Log.i("input", s);
                if (isAllNums(s)) {
                    poolExecutor.execute(() -> {
                        File exFilesDir = getExternalFilesDir("");
                        FileOutputStream outputStream;
                        try {
                            File defTime = new File(exFilesDir.getPath() + "/defTime");
                            if (defTime.exists()) {
                                if (defTime.delete()) {
                                    outputStream = new FileOutputStream(defTime);
                                    if (s != null) {
                                        outputStream.write(s.getBytes());
                                    }
                                    outputStream.close();
                                }
                            } else {
                                outputStream = new FileOutputStream(defTime);
                                if (s != null) {
                                    outputStream.write(s.getBytes());
                                }
                                outputStream.close();
                            }
                        } catch (Exception e) {
                            Log.e("savingTime", e.toString());
                        }
                    });
                } else {
                    Toast.makeText(getApplicationContext(), "is not a number", Toast.LENGTH_SHORT).show();
                }
                alertDialog.cancel();
            }
        });
        dialogLayoutBinding.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!isAllNums(s.toString())) {
                    dialogLayoutBinding.textInputLayout.setErrorEnabled(true);
                    dialogLayoutBinding.textInputLayout.setError("is not a number");
                } else {
                    dialogLayoutBinding.textInputLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
        return alertDialog;
    }
}
