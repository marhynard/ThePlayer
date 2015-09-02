package com.fritzbang.theplayer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

@SuppressLint("NewApi")
public class ThePlayerMediaService extends Service implements
		MediaPlayer.OnErrorListener {

    public static final int COMPLETE_MESSAGE = 1;
    public static final int SHUTDOWN_MESSAGE = 2;
    public static final int PAUSE_MESSAGE = 3;
    public static final int PLAY_MESSAGE = 4;

	private final IBinder mBinder = new MyBinder();
    private Handler idleHandler = new Handler();
	private MediaPlayer mediaPlayer = null;
	private boolean isPlaying = false;
	private static int classID = 579; // just a number
	private Handler aHandler;
	Notification notification;
	private String currentTrackTitle = "";
    private long currentTime = -1;
    private long startIdleTime = -1;
    private long idleDelay = 600000; //1000mill * 60sec * 10min = 600000
	private int flag = -1;
	private static final String DEBUG_TAG = "ThePlayerMediaService";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		flag = flags;
		Log.d(DEBUG_TAG,
				"The flag: " + flags + " " + startId + " " + intent.getAction());

			if (intent.getAction() != null) {
				if (intent.getAction().equalsIgnoreCase("forward")) {
					forward(5000);
				}
				if (intent.getAction().equalsIgnoreCase("rewind")) {
					rewind(5000);
				}
				if (intent.getAction().equalsIgnoreCase("pause")) {
					pause();
                    sendMessage(PAUSE_MESSAGE);

				}
				if (intent.getAction().equalsIgnoreCase("play")) {
					play(null);
                    sendMessage(PLAY_MESSAGE);
				}
				if (intent.getAction().equalsIgnoreCase("shutdown")) {
                    shutdownThePlayerMediaService();

				}
			}

		return Service.START_NOT_STICKY;
	}

    private boolean shutdownThePlayerMediaService() {
		Log.d(DEBUG_TAG,"shutting down the service");
		if(flag != SHUTDOWN_MESSAGE) {
			sendMessage(SHUTDOWN_MESSAGE);
		}
        idleHandler.removeCallbacks(IdleApplication);
        this.stopForeground(true);
        stopSelf();
        return false;
    }

    public void seek(int position) {
		if (mediaPlayer != null) {
			mediaPlayer.seekTo(position);
		}
	}

	public void forward(int forwardTime) {
		if (mediaPlayer != null) {
			int startTime = mediaPlayer.getCurrentPosition();
			int finalTime = mediaPlayer.getDuration();
			int temp = startTime;
			if ((temp + forwardTime) <= finalTime) {
				startTime = startTime + forwardTime;
				mediaPlayer.seekTo(startTime);
			} else {
				Toast.makeText(getApplicationContext(),
						"Cannot jump forward 5 seconds", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	public void rewind(int backwardTime) {
		if (mediaPlayer != null) {
			int startTime = mediaPlayer.getCurrentPosition();
			int temp = startTime;
			if ((temp - backwardTime) > 0) {
				startTime = startTime - backwardTime;
				mediaPlayer.seekTo(startTime);
			} else {
				Toast.makeText(getApplicationContext(),
						"Cannot jump backward 5 seconds", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	public void pause() {
		if (isPlaying) {
			if (mediaPlayer != null) {
				Log.d(DEBUG_TAG, "Media Player is pausing");
				mediaPlayer.pause();
                notification = createPauseNotification(currentTrackTitle);
                startForeground(classID, notification);
                startIdleTime = -1;
                idleHandler.postDelayed(IdleApplication, 500);
			}
			isPlaying = false;
		}

	}

	public void play(TrackBean selectedTrackInfo) {
		if (selectedTrackInfo != null) {
			currentTrackTitle = selectedTrackInfo.trackTitle;
		}

		if (!isPlaying) {
			Log.d(DEBUG_TAG, "Starting player");


			if (mediaPlayer == null) {
				Log.d(DEBUG_TAG, "Media Player is being created");
                mediaPlayer = MediaPlayer.create(this,Uri.parse(selectedTrackInfo.location));
                if(mediaPlayer == null){
                    isPlaying = false;
                    sendMessage(COMPLETE_MESSAGE);
                 }else{
                    mediaPlayer.start();
                    isPlaying = true;
                }
			} else {
				Log.d(DEBUG_TAG, "Media Player is starting");
				mediaPlayer.start();
                isPlaying = true;
			}

		} else {
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
				mediaPlayer = MediaPlayer.create(this,
						Uri.parse(selectedTrackInfo.location));
				mediaPlayer.start();

				isPlaying = true;
			} else
				Log.d(DEBUG_TAG, "Not sure why I am getting this right now");
		}
        if(isPlaying) {
            notification = createPlayNotification(currentTrackTitle);
            startForeground(classID, notification);
            mediaPlayer.setOnCompletionListener(completionListener);
        }
	}

	public void setHandler(Handler aHandler) {
		this.aHandler = aHandler;
	}

	OnCompletionListener completionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
            sendMessage(COMPLETE_MESSAGE);
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();//stop();
	}

	private void stop() {
		if (isPlaying) {
			isPlaying = false;
			if (mediaPlayer != null) {
				mediaPlayer.release();
				mediaPlayer = null;
			}
			stopForeground(true);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d(DEBUG_TAG, "There is an Error in the media player");
		return false;
	}

	public class MyBinder extends Binder {
		ThePlayerMediaService getService() {
			return ThePlayerMediaService.this;
		}
	}

	public int getCurrentPosition() {
        if(mediaPlayer != null)
		    return mediaPlayer.getCurrentPosition();
        return 0;
	}

	public int getFinishTime() {
		return mediaPlayer.getDuration();
	}

	public boolean isPlaying() {
		if (mediaPlayer == null)
			return false;
		try {
            return mediaPlayer.isPlaying();
		} catch (IllegalStateException ex) {
			return false;
		}
	}

	public void restore(TrackBean selectedTrackInfo, int startTime) {
		currentTrackTitle = selectedTrackInfo.trackTitle;

		notification = createPlayNotification(selectedTrackInfo.trackTitle);
		if (mediaPlayer == null) {
			Log.d(DEBUG_TAG, "Starting player");
			isPlaying = true;

			if (mediaPlayer == null) {
				Log.d(DEBUG_TAG, "Media Player is being created");
				mediaPlayer = MediaPlayer.create(this,
						Uri.parse(selectedTrackInfo.location));
				mediaPlayer.seekTo(startTime);
				mediaPlayer.start();
			} else {
				Log.d(DEBUG_TAG, "Media Player is starting");
				mediaPlayer.seekTo(startTime);
				mediaPlayer.start();
			}

		}
		startForeground(classID, notification);
		mediaPlayer.setOnCompletionListener(completionListener);
	}

	Notification createPlayNotification(String trackTitle) {

        Intent intent = new Intent(this, ThePlayerActivity.class);

		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);


		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
				0, intent, 0);

		PendingIntent rewindPendingIntent = PendingIntent.getService(this, 0,
				new Intent(this,ThePlayerMediaService.class).setAction("rewind"), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent pausePendingIntent = PendingIntent.getService(this, 0,
                new Intent(this,ThePlayerMediaService.class).setAction("pause"), PendingIntent.FLAG_UPDATE_CURRENT);

		PendingIntent forwardPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this,ThePlayerMediaService.class).setAction("forward"), PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification.Builder(
				getApplicationContext())
				//.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setContentTitle("The Player")
				.setContentText("Now Playing: " + trackTitle)
				.setSmallIcon(android.R.drawable.ic_media_play)
				.addAction(android.R.drawable.ic_media_rew, "Rewind",
						rewindPendingIntent)
				.addAction(android.R.drawable.ic_media_pause, "Pause",
						pausePendingIntent)
				.addAction(android.R.drawable.ic_media_ff, "Forward",
						forwardPendingIntent)
                .setContentIntent(pi).build();
		return notification;
	}

	Notification createPauseNotification(String trackTitle) {
		Intent intent = new Intent(this, ThePlayerActivity.class);
		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),0, intent, 0);

		PendingIntent playPendingIntent = PendingIntent.getService(this, 3,
                new Intent(this, ThePlayerMediaService.class).setAction("play"), PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent shutdownPendingIntent = PendingIntent.getService(this, 4,
                new Intent(this, ThePlayerMediaService.class).setAction("shutdown"), PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification.Builder(
				getApplicationContext())
                //.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setContentTitle("The Player")
				.setContentText("Now Playing: " + trackTitle)
				.setSmallIcon(android.R.drawable.ic_media_pause)
				.addAction(android.R.drawable.ic_media_play, "Play",
						playPendingIntent)
				.addAction(android.R.drawable.ic_menu_delete, "Shutdown",
						shutdownPendingIntent).setContentIntent(pi)
                //.setStyle(new Notification.MediaStyle()
                //        .setShowActionsInCompactView(1 /* #1: pause button */))
                        //.setMediaSession(mMediaSession.getSessionToken())

                        .build();
		return notification;
	}
    private void sendMessage(int messageID){
        if (aHandler != null) {
            Messenger messenger = new Messenger(aHandler);
            Message msg = Message.obtain();
            msg.arg1 = messageID;
            try {
                messenger.send(msg);
            } catch (android.os.RemoteException e1) {
                Log.w(getClass().getName(), "Exception sending message", e1);
            }
        }
    }
    private Runnable IdleApplication = new Runnable() {

        public void run() {
            if(isPlaying())
                startIdleTime = -1;
            else{
                Calendar c = Calendar.getInstance();
                currentTime = c.getTimeInMillis();
                if(startIdleTime == -1)
                    startIdleTime = currentTime;
                long totalIdleTime = currentTime - startIdleTime;
                //Log.d(DEBUG_TAG,"Time: " + currentTime + " " + startIdleTime + " " + totalIdleTime);
                boolean postDelay = true;
                if(totalIdleTime >= idleDelay)
                {
                   postDelay = shutdownThePlayerMediaService();
                }
                if(postDelay)
                    idleHandler.postDelayed(this, 500);
            }

          }
    };
}