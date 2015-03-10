package com.fritzbang.theplayer;

/**
 * Created by mrhynard on 2/14/2015.
 * comes from https://code.google.com/p/sambaexplorer/
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class SambaExplorer extends Activity {
    private static final String DEBUG_TAG = "SambaExplorer";
    public String mHost;
    TextView textViewStatus;
    TextView textViewAddressValue;
    TextView textViewNumFilesValue;
    Button buttonAddTracks;
    public boolean active;
    private String IPsubnet;
    int numberOfFiles = 0;
    ArrayList<String> fileList = new ArrayList<>();
    ArrayList<String> downloadList = new ArrayList<>();
    int directoryCount = 0;
    String directoryLocation = "";
    //String baseHostDirectory = "smb://thepoop/prime";
    String baseHostDirectory = "smb://yetanother";
    String username = "";
    String password = "";
    ProgressDialog mDialog;
    Context context;


    private static String getIPsubnet(int addr) {
        StringBuffer buf = new StringBuffer();
        buf.append(addr & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.');
        return buf.toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samba_explorer);
        directoryLocation = getIntent().getStringExtra("directoryLocation");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        baseHostDirectory = getIntent().getStringExtra("domain");


        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        buttonAddTracks = (Button) findViewById(R.id.buttonAddFiles);
        textViewAddressValue = (TextView) findViewById(R.id.textViewAddressValue);
        textViewNumFilesValue = (TextView) findViewById(R.id.textViewNumFilesValue);

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni.getType() == ConnectivityManager.TYPE_MOBILE) {
            new AlertDialog.Builder(this)
                    .setMessage("This application is meant for WIFI networks.")
                    .show();
            return;
        }

        WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        DhcpInfo info = wifi.getDhcpInfo();
        IPsubnet = getIPsubnet(info.ipAddress);

        mHost = baseHostDirectory + "/Music/";

        Log.d(DEBUG_TAG, "Connecting to " + mHost);

        jcifs.Config.setProperty("jcifs.encoding", "Cp1252");
        jcifs.Config.setProperty("jcifs.smb.lmCompatibility", "0");
        jcifs.Config.setProperty("jcifs.netbios.hostname", "AndroidPhone");

        jcifs.Config.registerSmbURLHandler();

        if (!mHost.startsWith("smb:/")) {
            if (mHost.startsWith("/")) {
                mHost = "smb:/" + mHost + "/";
            } else {
                mHost = "smb://" + mHost + "/";
            }
        }
        Log.d(DEBUG_TAG, "mHost: " + mHost);
        new SambaExplorerTask().execute(mHost);

        buttonAddTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(DEBUG_TAG,directoryLocation);
                new AddTracksTask(directoryLocation,SambaExplorer.this).execute();
            }
        });
    }

    class SambaExplorerTask extends AsyncTask<String, Integer, Integer> {
        String url;

        public SambaExplorerTask() {
        }

        @Override
        protected Integer doInBackground(String... params) {

            try {
                url = params[0];
                NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
                SmbFile dir = new SmbFile(url, auth);

                if (dir.listFiles().length > 0) {
                    numberOfFiles = dir.listFiles().length;
                    publishProgress(1);
                    getFiles(dir);
                    numberOfFiles = fileList.size();
                    //Log.d(DEBUG_TAG,"Directory Count: " + directoryCount);
                    publishProgress(2);
                } else {
                    publishProgress(0);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (SmbException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void getFiles(SmbFile dir) throws SmbException {
            directoryCount++;
            for (SmbFile f : dir.listFiles()) {
                if (f.isDirectory()) {
                    //Log.d(DEBUG_TAG,f.getName());
                    getFiles(f);
                } else {
                    if (f.getPath().toLowerCase().endsWith(".mp3")) {
                        //Log.d(DEBUG_TAG,f.getCanonicalPath());
                        fileList.add(f.getCanonicalPath());
                        numberOfFiles = fileList.size();
                        publishProgress(1);
                    }
                }

            }
        }

        @Override
        public void onProgressUpdate(Integer... ints) {
            int progressUpdate = ints[0];
            switch (progressUpdate) {
                case 0:
                    textViewStatus.setText("Not Connected");
                    textViewAddressValue.setText(url);
                    buttonAddTracks.setEnabled(false);
                    break;
                case 1:
                    textViewStatus.setText("Connected");
                    textViewAddressValue.setText(url);
                    buttonAddTracks.setEnabled(false);
                    textViewNumFilesValue.setText("" + numberOfFiles);
                    break;
                case 2:
                    textViewStatus.setText("Connected");
                    textViewAddressValue.setText(url);
                    buttonAddTracks.setEnabled(true);
                    textViewNumFilesValue.setText("" + numberOfFiles);
                    break;
            }
        }
    }

    private class AddTracksTask extends AsyncTask<String, Integer, Integer> {
        String directoryLocation = "";
        public AddTracksTask(String directoryLocation,Context context) {
            this.directoryLocation = directoryLocation;
            mDialog = new ProgressDialog(context);
            // Do your dialog stuff here
            mDialog.setMessage("Finding Files");
            mDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            try {
                long spaceLeft = (new File(directoryLocation)).getUsableSpace();
                Random randomGenerator = new Random();
                downloadList.clear();
                while(spaceLeft >= 0) {

                    //pick file from list
                    int randomInt = randomGenerator.nextInt(fileList.size());
                    String fileName = fileList.get(randomInt);
                    fileList.remove(randomInt);

                    NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
                    SmbFile fileToCopy = new SmbFile(fileName, auth);

                    //TODO check to see if it is on device or in database

                    //TODO insert file info into database

                    long fileSize = fileToCopy.length();
                    if(fileSize < spaceLeft){
                        if(!isOnDevice(fileName)){
                            downloadList.add(fileName);

                            //copyFile(fileToCopy);

                            spaceLeft = spaceLeft - fileSize;

                        }
                    }
                    if(fileList.isEmpty())
                        spaceLeft = -1;

                    publishProgress(1,fileList.size(),downloadList.size(),(int)spaceLeft);
                }
                publishProgress(2);
                copyFiles();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (SmbException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        public void onProgressUpdate(Integer... ints) {
            if(ints[0] == 1) {
                //mDialog.setProgress(ints[0]);
                mDialog.setMessage("Files Left: " + ints[1] + "\nNumberFiles: " + ints[2] + "\nSpace Left: " + ints[3]);
                //textViewSpace.setText(ThePlayerActivity.updateSpaceAvailable(new File(directory)));
            }else if(ints[0] == 2){
                mDialog.setMessage("All files found");
            }

        }
        public void onPostExecute(Integer result) {
            mDialog.dismiss();
        }
    }

    private void copyFiles() {

    }

    private void copyFile(SmbFile fileToCopy) throws IOException {

        String fileName = fileToCopy.getCanonicalPath().replace(baseHostDirectory,directoryLocation);
        Log.d(DEBUG_TAG,fileName);
        String finalDirectory = fileName.substring(0,fileName.lastIndexOf("/"));
        File tmpFile = new File(finalDirectory);
        tmpFile.mkdirs();

        SmbFileInputStream in = new SmbFileInputStream(fileToCopy);

        FileOutputStream out = new FileOutputStream(fileName);


        long t0 = System.currentTimeMillis();

        byte[] b = new byte[8192];
        int n, tot = 0;
        while ((n = in.read(b)) > 0) {
            out.write(b, 0, n);
            tot += n;

            if (DownloadService.serviceCancel) {
                DownloadService.serviceCancel = false;
                break;
            }
        }

        long t = System.currentTimeMillis() - t0;

        in.close();
        out.close();
    }

    private boolean isOnDevice(String fileName) {
        return false;
    }
}
