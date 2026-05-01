package com.example.quranplayer.datamodels;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;

public class PdfItemModel {
    int index;
    Bitmap bitmap;
    PdfRenderer pdfRenderer;

    public PdfItemModel(Bitmap bitmap, int index, PdfRenderer pdfRenderer) {
        this.bitmap = bitmap;
        this.index = index;
        this.pdfRenderer = pdfRenderer;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getIndex() {
        return index;
    }

    public PdfRenderer getPdfRenderer() {
        return pdfRenderer;
    }
}
