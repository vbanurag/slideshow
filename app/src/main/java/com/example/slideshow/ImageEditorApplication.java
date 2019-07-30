package com.example.slideshow;

import android.app.Application;

import com.createchance.imageeditor.IEManager;

public class ImageEditorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        IEManager.getInstance().init(this);

        Constants.setConstants(this);
    }
}
