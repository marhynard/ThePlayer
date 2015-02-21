package com.fritzbang.theplayer;

/**
 * Created by mrhynard on 2/14/2015.
 * comes from https://code.google.com/p/sambaexplorer/
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

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
import android.widget.Button;
import android.widget.TextView;


import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SambaExplorer extends Activity {
    private static final String DEBUG_TAG = "SambaExplorer";
    public String mHost;
    TextView textViewStatus;
    TextView textViewAddress;
    TextView textViewNumFilesValue;
    Button buttonAddTracks;
    public boolean active;
    private String IPsubnet;
    int numberOfFiles = 0;
    ArrayList<String> fileList = new ArrayList<>();


    private static String getIPsubnet(int addr) {
        StringBuffer buf = new StringBuffer();
        buf.append(addr  & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.');
        return buf.toString();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samba_explorer);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        buttonAddTracks = (Button) findViewById(R.id.buttonAddFiles);
        textViewAddress = (TextView) findViewById(R.id.textViewAddressValue);
        textViewNumFilesValue = (TextView) findViewById(R.id.textViewNumFilesValue);

        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
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


        mHost = "smb://thepoop/prime/Music/";

        Log.d(DEBUG_TAG,"Connecting to "+mHost);

            jcifs.Config.setProperty("jcifs.encoding", "Cp1252");
            jcifs.Config.setProperty("jcifs.smb.lmCompatibility", "0");
            jcifs.Config.setProperty("jcifs.netbios.hostname", "AndroidPhone");

            jcifs.Config.registerSmbURLHandler();

            if (!mHost.startsWith("smb:/")) {
                if (mHost.startsWith("/")) {
                    mHost = "smb:/"+mHost+"/";
                } else {
                    mHost = "smb://"+mHost+"/";
                }
            }
            Log.d(DEBUG_TAG, "mHost: " + mHost);
            new SambaExplorerTask().execute(mHost);

    }

    class SambaExplorerTask extends AsyncTask<String, Integer, Integer> {

        private static final String DEBUG_TAG = "SambaExplorerTask";
        private ProgressDialog mDialog;


        String url;

        public SambaExplorerTask() {
//        mDialog = new ProgressDialog(context);
//            this.context = context;
//        mDialog.setMessage("Loading Files");
//        mDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {

            try {
                url = params[0];
                NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", "matt", "backyard");
                SmbFile dir = new SmbFile(url,auth);
                if(dir.listFiles().length > 0){
                    numberOfFiles = dir.listFiles().length;
                    publishProgress(1);
                for (SmbFile f : dir.listFiles()) {
                    if(f.isDirectory()) {
                        fileList.addAll(getFiles(f));
                    }
                    else {
                        if (f.getPath().toLowerCase().endsWith(".mp3")){
                            System.out.println(f.getName());
                            fileList.add(f.getPath());
                        }
                    }

                }
                    publishProgress(2);
                }else{
                    publishProgress(0);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (SmbException e) {
                e.printStackTrace();
            }

            return null;
        }

        private ArrayList<String> getFiles(SmbFile f) throws SmbException {

            if(f.isDirectory()) {
                fileList.addAll(getFiles(f));
            }
            else {
                if (f.getPath().toLowerCase().endsWith(".mp3")){
                    System.out.println(f.getName());
                    fileList.add(f.getPath());
                }
            }
            return null;
        }

        @Override
        public void onProgressUpdate(Integer... ints) {
            int progressUpdate = ints[0];
            switch(progressUpdate){
                case 0:
                    textViewStatus.setText("Not Connected");
                    buttonAddTracks.setEnabled(false);
                    break;
                case 1:
                    textViewStatus.setText("Connected");
                    buttonAddTracks.setEnabled(false);
                    break;
                case 2:
                    textViewStatus.setText("Connected");
                    buttonAddTracks.setEnabled(true);
                    textViewNumFilesValue.setText(numberOfFiles);
                    break;
            }
        }
    }
}
