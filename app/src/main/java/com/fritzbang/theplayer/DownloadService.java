package com.fritzbang.theplayer;

/**
 * Created by mrhynard on 2/14/2015.
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;


public class DownloadService extends Service {
    public static String[] DownloadQueue = new String[2048];
    public static int QueueTop = 0;
    public static boolean serviceStarted = false;
    public static boolean servicePaused = false;
    public static Activity mOwner;

    private static int retryCount = 0;
    protected static boolean serviceCancel;
    protected static NtlmPasswordAuthentication userAuth;

    static void ProvideLoginCredentials(String domain, String username, String password) {
        try {
            userAuth = new NtlmPasswordAuthentication(null, username, password);
        } catch (Exception e) {
            showNotification(username + "@" + domain + " failed to authenticate");
        }
    }

    static void QueueDownload(final Activity owner, String download) {

        if (DownloadService.QueueTop >= 2047) {
            showNotification("Download queue full! some files not queued");
            return;
        }

        DownloadService.mOwner = owner;
        DownloadService.DownloadQueue[DownloadService.QueueTop++] = download;


        try {
            if (DownloadService.serviceStarted == false) {
                Intent service = new Intent(owner, DownloadService.class);
                owner.startService(service);
            }
        } catch (Exception e) {

            final Exception E = e;
            Runnable dialogPopup1 = new Runnable() {
                @Override
                public void run() {
                    String StackTrace = "";
                    StackTraceElement[] Stack = E.getStackTrace();
                    for (int i = 0; i < Stack.length; i++) {
                        StackTrace += Stack[i].toString() + "\n";
                    }
                    new AlertDialog.Builder(owner)
                            .setMessage(StackTrace)
                            .setTitle(E.toString())
                            .show();
                }
            };
            owner.runOnUiThread(dialogPopup1);
        }


    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    Thread mDownloadThread = new Thread(new Runnable() {
        @Override
        public void run() {
            DownloadService.serviceStarted = true;

            showNotification("Download service started.");

            while (QueueTop > 0) {
                try {
                    SmbFile f;

                    DownloadService.retryCount++;
                    if (DownloadService.retryCount > 4) {
                        DownloadService.retryCount = 1;
                        DownloadService.QueueTop--;

                        if (DownloadService.QueueTop < 0) {
                            break;
                        }
                    }

                    int filenameIndex = DownloadService.DownloadQueue[DownloadService.QueueTop - 1].lastIndexOf("/") + 1;
                    String fileName = DownloadService.DownloadQueue[DownloadService.QueueTop - 1].substring(filenameIndex);

                    showNotification("Downloading " + fileName);


                    if (DownloadService.userAuth != null) {
                        f = new SmbFile(DownloadService.DownloadQueue[DownloadService.QueueTop - 1], DownloadService.userAuth);
                    } else {
                        f = new SmbFile(DownloadService.DownloadQueue[DownloadService.QueueTop - 1]);
                    }

                    f = new SmbFile(DownloadService.DownloadQueue[DownloadService.QueueTop - 1]);


                    SmbFileInputStream in = new SmbFileInputStream(f);
                    FileOutputStream out = new FileOutputStream("/sdcard/download/" + fileName);


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

                    DownloadService.QueueTop--;

                    showNotification("Finished downloading " + fileName + "(" + (tot / 1024) + "kb) in " + (t / 1000.0) + "sec");


                    while (DownloadService.servicePaused) {
                        Thread.sleep(1000);
                    }
                } catch (SmbAuthException e) {

                    startActivity(new Intent(mOwner, com.fritzbang.theplayer.SambaLogin.class));

                } catch (MalformedURLException e) {
                    final MalformedURLException E = e;
                    Runnable dialogPopup1 = new Runnable() {
                        @Override
                        public void run() {
                            String StackTrace = "";
                            StackTraceElement[] Stack = E.getStackTrace();
                            for (int i = 0; i < Stack.length; i++) {
                                StackTrace += Stack[i].toString() + "\n";
                            }
                            new AlertDialog.Builder(mOwner)
                                    .setMessage(StackTrace)
                                    .setTitle(E.toString())
                                    .show();
                        }
                    };
                    mOwner.runOnUiThread(dialogPopup1);
                } catch (SmbException e) {
                    final SmbException E = e;
                    Runnable dialogPopup1 = new Runnable() {
                        @Override
                        public void run() {
                            String StackTrace = "";
                            StackTraceElement[] Stack = E.getStackTrace();
                            for (int i = 0; i < Stack.length; i++) {
                                StackTrace += Stack[i].toString() + "\n";
                            }
                            new AlertDialog.Builder(mOwner)
                                    .setMessage(StackTrace)
                                    .setTitle(E.toString())
                                    .show();
                        }
                    };
                    mOwner.runOnUiThread(dialogPopup1);
                } catch (UnknownHostException e) {
                    final UnknownHostException E = e;
                    Runnable dialogPopup1 = new Runnable() {
                        @Override
                        public void run() {
                            String StackTrace = "";
                            StackTraceElement[] Stack = E.getStackTrace();
                            for (int i = 0; i < Stack.length; i++) {
                                StackTrace += Stack[i].toString() + "\n";
                            }
                            new AlertDialog.Builder(mOwner)
                                    .setMessage(StackTrace)
                                    .setTitle(E.toString())
                                    .show();
                        }
                    };
                    mOwner.runOnUiThread(dialogPopup1);
                } catch (FileNotFoundException e) {
                    final FileNotFoundException E = e;
                    Runnable dialogPopup1 = new Runnable() {
                        @Override
                        public void run() {
                            String StackTrace = "";
                            StackTraceElement[] Stack = E.getStackTrace();
                            for (int i = 0; i < Stack.length; i++) {
                                StackTrace += Stack[i].toString() + "\n";
                            }
                            new AlertDialog.Builder(mOwner)
                                    .setMessage(StackTrace)
                                    .setTitle(E.toString())
                                    .show();
                        }
                    };
                    mOwner.runOnUiThread(dialogPopup1);
                } catch (IOException e) {
                    final IOException E = e;
                    Runnable dialogPopup1 = new Runnable() {
                        @Override
                        public void run() {
                            String StackTrace = "";
                            StackTraceElement[] Stack = E.getStackTrace();
                            for (int i = 0; i < Stack.length; i++) {
                                StackTrace += Stack[i].toString() + "\n";
                            }
                            new AlertDialog.Builder(mOwner)
                                    .setMessage(StackTrace)
                                    .setTitle(E.toString())
                                    .show();
                        }
                    };
                    mOwner.runOnUiThread(dialogPopup1);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }


                DownloadService.retryCount = 0;
            }


            stopSelf();


        }


    });

    @Override
    public void onCreate() {
        super.onCreate();

        mDownloadThread.start();

    }

    @Override
    public void onDestroy() {
        DownloadService.serviceStarted = false;
        mDownloadThread.stop();
    }

    public static void showNotification(String text) {
        NotificationManager nm = (NotificationManager) mOwner.getSystemService(Service.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(mOwner, 0,
                new Intent(mOwner, DownloadQueue.class), 0);

        // construct the Notification object.
        Notification notif = new Notification();

        notif.tickerText = text;
        notif.icon = R.drawable.ic_refresh;

        RemoteViews nmView = new RemoteViews(mOwner.getPackageName(), R.layout.notify);
        nmView.setTextViewText(R.id.textView, text);

        //     nmView.setProgressBar(R.id.ProgressBar01, max, tot, false);
        //     nmView.setProgressBar(R.id.ProgressBar02, files, file, false);

        notif.contentView = nmView;

        notif.contentIntent = contentIntent;
        nm.notify(R.layout.notify, notif);
    }

    public static String GetDownloadQueue(int i) {
        return DownloadQueue[i];
    }
}
