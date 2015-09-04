package com.fritzbang.theplayer;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DownloadManager {
    private static final String DEBUG_TAG = "DownloadManager";

    @SuppressLint("SdCardPath")
    // private final static String PATH = "/data/data/com.fritzbang.downlow/";
    // // put
    // the
    // downloaded
    // file
    // here
    private final static String PATH = "/mnt/sdcard/com.fritzbang.downlow/";

    // TODO create a folder for each podcast to put the files in

    public static String DownloadFromUrl(String path, String imageURL) { // this
        // is
        // the
        // downloader method

        String fileName = imageURL.substring(imageURL.lastIndexOf("/") + 1,
                imageURL.length());

        String filename;
        if (path == null)
            filename = PATH + fileName;
        else
            filename = path + "/" + fileName;

        BufferedInputStream bis = null;
        FileOutputStream fos = null;

        try {

            URL url = new URL(imageURL); // you can write here any link
            File file = new File(filename);

            long startTime = System.currentTimeMillis();
            Log.d(DEBUG_TAG, "download begining");
            Log.d(DEBUG_TAG, "download url:" + url);
            Log.d(DEBUG_TAG, "downloaded file name:" + fileName);
            /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();

			/*
			 * Define InputStreams to read from the URLConnection.
			 */
            InputStream is = ucon.getInputStream();
            bis = new BufferedInputStream(is);
			/*
			 * Define Output stream
			 */
            fos = new FileOutputStream(file);

			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1).
			 */
            // ByteArrayBuffer baf = new ByteArrayBuffer(50);
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

        } catch (MalformedURLException e) {
            filename = null;
            // TODO take out the stack trace handle MalformedURLException
            e.printStackTrace();

        } // TODO take out the stack trace handle FileNotFoundException
        catch (FileNotFoundException e) {
            filename = null;
            e.printStackTrace();
        } // TODO take out the stack trace handle IOException
        catch (IOException e) {
            filename = null;
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                }// TODO take out the stack trace handle IOException
                catch (IOException e) {
                    filename = null;
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                }// TODO take out the stack trace handle IOException
                catch (IOException e) {
                    filename = null;
                    e.printStackTrace();
                }
            }
        }
        return filename;
    }
}
