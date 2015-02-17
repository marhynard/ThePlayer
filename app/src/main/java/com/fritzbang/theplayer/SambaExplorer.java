package com.fritzbang.theplayer;

/**
 * Created by mrhynard on 2/14/2015.
 * comes from https://code.google.com/p/sambaexplorer/
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SambaExplorer extends android.app.ListActivity {
    public ArrayAdapter<String> mList;
    public String[] mListStrings;
    public String[] mListContents;
    public String mHost;
    int curListID=0;

    public boolean active;
    private String IPsubnet;

    private static String ipAddressToString(int addr) {
        StringBuffer buf = new StringBuffer();
        buf.append(addr  & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.').
                append((addr >>>= 8) & 0xff);
        return buf.toString();
    }

    private static String getIPsubnet(int addr) {
        StringBuffer buf = new StringBuffer();
        buf.append(addr  & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.').
                append((addr >>>= 8) & 0xff).append('.');
        return buf.toString();
    }

    Thread m_subnetScanThread;
    public int numThreadsRunning;
    public int serversScanned;
    class SubnetScanThread implements Runnable {
        public SambaExplorer mOwner;

        SubnetScanThread(SambaExplorer owner) {
            mOwner = owner;
        }

        @Override
        public void run() {

            int timeout = 1000;
            int start = 1;
            int end = 10;

            mOwner.numThreadsRunning++;

            if (mOwner.IPsubnet.endsWith("*")) {
                mOwner.IPsubnet = mOwner.IPsubnet.substring(0, mOwner.IPsubnet.length()-1);
            }


            for (int tries = 0; tries < 1; tries++) {
                for (int i=start;i<=end;i++) {
                    String serverName = new String(mOwner.IPsubnet+String.valueOf(i));
                    mOwner.serversScanned++;

                    while (!mOwner.active) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    try {
                        InetAddress serverAddr = InetAddress.getByName(serverName);
                        mOwner.getWindow().setFeatureInt(Window.FEATURE_PROGRESS, (int)(9999.0 * ((1.0+mOwner.serversScanned)/256)));
                        if (serverAddr.isReachable(timeout)) {
                            mOwner.AddListItem("smb://"+serverAddr.getCanonicalHostName()+"/");
                        }
                    } catch (Exception e) {


                        mOwner.AddListItem(e.getMessage());

                    }
                }
                timeout += 500;
            }

            if (mOwner.numThreadsRunning == 1) {
                // if we're the last thread running...


                Runnable alertDialog = new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SambaExplorer.this, "Finished scanning "+mOwner.serversScanned+" servers", Toast.LENGTH_SHORT);
                    }
                };

                runOnUiThread(alertDialog);
            }

            mOwner.numThreadsRunning--;
        }


    };

    private String mSubnetOverride;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.samba_explorer);
        mListStrings = new String[255];
        mListContents = new String[255];
        for (int i=0;i<255;i++) {
            mListStrings[i] = "";
            mListContents[i] = "";
        }

        ListView view = getListView();
        mList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListStrings);

        view.setAdapter(mList);
        view.setOnCreateContextMenuListener(this);





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





        mHost = null;

        Intent intent = getIntent();
        mHost = intent.getDataString();
        //   mSubnetOverride = intent.getStringExtra("subnet");

        if (mHost == null) {
            //m_subnetScanThread = new Thread(new SubnetScanThread(this));
            //m_subnetScanThread.start();

            //startActivity(new Intent(this,com.shank.SambaExplorer.PickHost.class));

            try{
                Intent i = new Intent("com.shank.portscan.PortScan.class");
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("market://search?q=NetScan"));
                startActivity(i);
            }
        } else {


            SetLastListItem("Connecting to "+mHost);

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

            SmbFile f;
            try {

                if (DownloadService.userAuth != null) {
                    f = new SmbFile( mHost, DownloadService.userAuth );
                } else {
                    f = new SmbFile( mHost );
                }

                if (f.canRead()) {

                    TraverseSMB(f,1);

                }
            } catch (SmbAuthException e) {
                startActivity(new Intent(this, com.fritzbang.theplayer.SambaLogin.class).putExtra("path", mHost));
            } catch (MalformedURLException e) {
                final MalformedURLException E = e;
                Runnable dialogPopup = new Runnable() {
                    @Override
                    public void run() {
                        String StackTrace="";
                        StackTraceElement[] Stack = E.getStackTrace();
                        for (int i=0; i<Stack.length; i++) {
                            StackTrace += Stack[i].toString() + "\n";
                        }
                        new AlertDialog.Builder(SambaExplorer.this)
                                .setMessage(StackTrace)
                                .setTitle(E.toString())
                                .show();
                    }
                };
                runOnUiThread(dialogPopup);
            } catch (SmbException e) {
                final SmbException E = e;
                Runnable dialogPopup = new Runnable() {
                    @Override
                    public void run() {
                        String StackTrace="";
                        StackTraceElement[] Stack = E.getStackTrace();
                        for (int i=0; i<Stack.length; i++) {
                            StackTrace += Stack[i].toString() + "\n";
                        }
                        new AlertDialog.Builder(SambaExplorer.this)
                                .setMessage(StackTrace)
                                .setTitle(E.toString())
                                .show();
                    }
                };
                runOnUiThread(dialogPopup);
            } catch (IOException e) {
                final IOException E = e;
                Runnable dialogPopup = new Runnable() {
                    @Override
                    public void run() {
                        String StackTrace="";
                        StackTraceElement[] Stack = E.getStackTrace();
                        for (int i=0; i<Stack.length; i++) {
                            StackTrace += Stack[i].toString() + "\n";
                        }
                        new AlertDialog.Builder(SambaExplorer.this)
                                .setMessage(StackTrace)
                                .setTitle(E.toString())
                                .show();
                    }
                };
                runOnUiThread(dialogPopup);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0,0,0,"Download");
        menu.add(0,1,1,"Rename");
        menu.add(0,2,2,"Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {


        return false;

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        String share = mListContents[position];
        if (share.startsWith("smb://")) {

            if (share.endsWith("/")) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(share));
                startActivity(intent);

            } else {
                // files

                DownloadService.QueueDownload(this, share);

            }

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, 0, 0, "Download All");
        menu.add(0, 1, 1, "Recursive Download All");
        menu.add(0, 2, 2, "Download Queue");
        menu.add(0, 3, 3, "Options");

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DownloadDirectory(new SmbFile(mHost),1);
                        } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case 1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DownloadDirectory(new SmbFile(mHost),255);
                        } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;

            case 2:
                Intent intent = new Intent(this, com.fritzbang.theplayer.DownloadQueue.class);
                startActivity(intent);
                break;
        }
        return false;
    }
    private Runnable updateAdapter = new Runnable() {
        @Override
        public void run() {
            mList.notifyDataSetChanged();
        }
    };

    public void ForceUpdate() {
        runOnUiThread(updateAdapter);
    }

    public void SetLastListItem(String str) {
        mListStrings[curListID] = str;
        ForceUpdate();
    }

    public void AddListItem(String server) {
        if (server.endsWith("/")) {
            String temp = server.substring(0,server.lastIndexOf('/'));
            mListStrings[curListID] = server.substring(temp.lastIndexOf('/'));
        } else {
            mListStrings[curListID] = server.substring(server.lastIndexOf('/'));
        }
        mListContents[curListID] = server;
        curListID++;
        ForceUpdate();

    }

    void DownloadDirectory( SmbFile f, int depth ) throws MalformedURLException, IOException {

        if( depth == 0 ) {
            return;
        }
        try{
            SmbFile[] l;

            l = f.listFiles();

            for(int i = 0; l != null && i < l.length; i++ ) {
                try {
                    if( l[i].isDirectory() ) {
                        DownloadDirectory( l[i], depth - 1 );
                    } else {
                        DownloadService.QueueDownload(this, l[i].getPath());
                    }

                    Thread.sleep(100);
                } catch( IOException ioe ) {

                }
            }

        }catch(Exception e){
            AddListItem(e.toString());
        }
    }

    void TraverseSMB( SmbFile f, int depth ) throws MalformedURLException, IOException {

        if( depth == 0 ) {
            return;
        }
        try{
            SmbFile[] l;

            l = f.listFiles();

            for(int i = 0; l != null && i < l.length; i++ ) {
                try {

                    AddListItem( l[i].getCanonicalPath());//.getDfsPath() );
                    if( l[i].isDirectory() ) {
                        TraverseSMB( l[i], depth - 1 );
                    }

                } catch (SmbAuthException e) {
                    startActivity(new Intent(this, com.fritzbang.theplayer.SambaLogin.class).putExtra("path", l[i].getCanonicalPath()));

                } catch( IOException ioe ) {

                }
            }

        } catch (SmbAuthException e) {
            startActivity(new Intent(this, com.fritzbang.theplayer.SambaLogin.class).putExtra("path", f.getCanonicalPath()));
        }catch(Exception e){
            AddListItem(e.toString());
        }
    }

}
