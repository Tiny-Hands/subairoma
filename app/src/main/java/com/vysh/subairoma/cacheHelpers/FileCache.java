package com.vysh.subairoma.cacheHelpers;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Created by Vishal on 4/11/2017.
 */

public class FileCache {
    private File cacheDir;

    public FileCache(Context context) {

        //Find the dir at SDCARD to save cached images

        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //if SDCARD is mounted (SDCARD is present on device and mounted)
            Log.d("mylog", "Creating new cacheDir in SD Card");
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "taglocateimges");
        } else {
            // if checking on simulator the create cache dir in your application context
            Log.d("mylog", "Creating new cacheDir in Emulator");
            cacheDir = context.getCacheDir();
        }

        if (!cacheDir.exists()) {
            // create cache dir in your application context
            Log.d("mylog", "Creating new cacheDir in application context");
            cacheDir.mkdirs();
        }
    }

    public File getFile(String url) {
        //Identify images by hashcode or encode by URLEncoder.encode.
        String filename = String.valueOf(url.hashCode());
        File f = new File(cacheDir, filename);
        return f;

    }

    public void clear() {
        // list all files inside cache directory
        File[] files = cacheDir.listFiles();
        if (files == null)
            return;
        //delete all cache directory files
        for (File f : files)
            f.delete();
    }
}
