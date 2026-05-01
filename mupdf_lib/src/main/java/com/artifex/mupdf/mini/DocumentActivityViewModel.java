package com.artifex.mupdf.mini;

import android.app.Application;
import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.artifex.mupdf.fitz.Document;

public class DocumentActivityViewModel extends AndroidViewModel {
    public DocumentActivityViewModel(@NonNull Application application) {
        super(application);
    }

    private final MutableLiveData<Uri> pdfUri = new MutableLiveData<>(null);
    private Document document;

    public MutableLiveData<Uri> getPdfUri() {
        return pdfUri;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
