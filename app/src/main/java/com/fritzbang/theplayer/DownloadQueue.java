package com.fritzbang.theplayer;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class DownloadQueue extends ListActivity {
    public ArrayAdapter<String> mList;
    public String[] mListStrings;
    boolean active;
    private Thread m_updateThread;

    private int curListID = 0;

    public class UpdateQueueThread implements Runnable {
        private DownloadQueue activity;


        public UpdateQueueThread(DownloadQueue act) {
            activity = act;
        }

        public void run() {
            while (true) {
                while (!activity.active) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                int qt = DownloadService.QueueTop - 1;

                for (int i = 2047; i > qt; i--) {
                    mListStrings[i] = "";
                }

                int u = 0;
                for (int i = qt; i >= 0; i--) {
                    mListStrings[i] = DownloadService.GetDownloadQueue(u++);
                }

                runOnUiThread(updateAdapter);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.samba_explorer);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 1);

        mListStrings = new String[2048];
        for (int i = 0; i < 2048; i++) mListStrings[i] = "";

        ListView view = getListView();

        mList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListStrings);
        view.setAdapter(mList);

        view.setLongClickable(true);


        m_updateThread = new Thread(new UpdateQueueThread(this));
        m_updateThread.start();

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {

        menu.add(0, 0, 0, "Remove" + menuInfo.toString());
    }

    public boolean onContextItemSelected(android.view.MenuItem menuitem) {
        switch (menuitem.getItemId()) {
            case 0:


                break;

        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();

        active = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        active = true;
    }

    private Runnable updateAdapter = new Runnable() {
        @Override
        public void run() {
            mList.notifyDataSetChanged();
        }
    };

    public void SetLastLine(String contents) {
        mListStrings[curListID] = contents;
        runOnUiThread(updateAdapter);
    }

    public void AddLine(String contents) {
        mListStrings[curListID] = contents;
        curListID++;

        runOnUiThread(updateAdapter);

    }

    public void onLongClick(View view) {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (DownloadService.servicePaused == false) {
            menu.add(0, 0, 0, "Pause Downloads");
        } else {
            menu.add(0, 0, 0, "Resume Downloads");
        }
        menu.add(0, 1, 1, "Cancel All Downloads");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                DownloadService.servicePaused = !DownloadService.servicePaused;
                if (DownloadService.servicePaused) {
                    DownloadService.showNotification("Download service paused.");
                } else {
                    DownloadService.showNotification("Download service unpaused.");
                }
                break;

            case 1:
                DownloadService.QueueTop = 1;
                DownloadService.serviceCancel = true;
                DownloadService.showNotification("Download queue cleared.");
                break;
        }
        return false;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        String contents = l.getItemAtPosition(position).toString();

    }


}