package com.example.slideshow;

import android.content.Context;

import java.io.File;


public class Constants {

    public static File mBaseDir;

    public static int mScreenWidth, mScreenHeight;

    public static void setConstants(Context context) {
        mBaseDir = new File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "outputs");
    }
}
