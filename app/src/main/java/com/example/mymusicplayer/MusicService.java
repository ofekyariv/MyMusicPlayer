package com.example.mymusicplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.NotificationTarget;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final IBinder binder = new LocalBinder();
    public static boolean isRunnig = false;
    public static int sPosition = RecyclerView.NO_POSITION;
    private Activity context;

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private NotificationManager manager;
    private ArrayList<Song> songs;
    private int duration;

    private ImageButton playPauseBtn;
    private TextView songName;
    private TextView songAuthor;
    private ImageView songCover;
    private SeekBar seekBar;
//    private int tempSongIndex = RecyclerView.NO_POSITION;

    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    private TextView songTotalDuration_tv;
    private TextView songCurrentDuration_tv;

    private RemoteViews remoteViews;
    private Notification notification;

    final int NOTIF_ID = 1;

    public class LocalBinder extends Binder {
        MusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        isRunnig = true;
        Log.d("index","service isRunning=true");

        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                    songCurrentDuration_tv.setText(createTimeLabel(mediaPlayer.getCurrentPosition()));
                }
                mHandler.postDelayed(this, 1000);
            }
        };


        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelId = "channel_id";
        String channelName = "channel_name";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);

        remoteViews = new RemoteViews(getPackageName(), R.layout.music_notification);

        Intent playIntent = new Intent(this, MusicService.class);
        playIntent.putExtra("command", "play");
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.play_pause_notif, playPendingIntent);


        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.putExtra("command", "next");
        nextIntent.putExtra("songs_list", songs);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.next_notif, nextPendingIntent);

        Intent prevIntent = new Intent(this, MusicService.class);
        prevIntent.putExtra("command", "prev");
        prevIntent.putExtra("songs_list", songs);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 2, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.prev_notif, prevPendingIntent);

        Intent closeIntent = new Intent(this, MusicService.class);
        closeIntent.putExtra("command", "close");
        PendingIntent closePendingIntent = PendingIntent.getService(this, 3, closeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.close_notification, closePendingIntent);

        Intent musicPlayerIntent = new Intent(this, MusicService.class);
        musicPlayerIntent.putExtra("command", "musicPlayer");
        PendingIntent musicPlayerPendingIntent = PendingIntent.getService(this, 4, musicPlayerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.content_layout, musicPlayerPendingIntent);

        builder.setCustomContentView(remoteViews);
        builder.setCustomBigContentView(remoteViews);
        builder.setSmallIcon(R.drawable.logo);
        builder.setOnlyAlertOnce(true);
        builder.setContentIntent(musicPlayerPendingIntent);
        notification = builder.build();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String command = intent.getStringExtra("command");
        switch (command) {
            case "new_instance":
                setUpSong(intent, intent.getBooleanExtra("same_song", false));
                startForeground(NOTIF_ID, notification);
                break;
            case "songs_update":
                songs = intent.getParcelableArrayListExtra("songs_list");
                break;
            case "play":
                if (!mediaPlayer.isPlaying()) {
                    if (playPauseBtn.isEnabled()) {
                        mediaPlayer.start();
                        playPauseBtn.setImageResource(R.drawable.pause_btn_selector);
                        remoteViews.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_notif_pause);
                    }
                } else {
                    if (playPauseBtn.isEnabled()) {
                        mediaPlayer.pause();
                        playPauseBtn.setImageResource(R.drawable.play_btn_selector);
                        remoteViews.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_notif_play);
                    }
                }
                manager.notify(NOTIF_ID, notification);
                break;
            case "next":
                playSong(true);
                break;
            case "prev":
                playSong(false);
                break;
            case "close":
                playPauseBtn.setImageResource(R.drawable.play_btn_selector);
                isRunnig = false;
//                sPosition = RecyclerView.NO_POSITION;
                MainActivity.songsAdapter.notifyDataSetChanged();
                stopForeground(true);
                stopSelf();
                break;
            case "musicPlayer":
                Intent intent1 = new Intent(MusicService.this, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent1);
                break;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null)
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
        mHandler.removeCallbacks(mRunnable);
        mediaPlayer.reset();
        mediaPlayer.release();
        isRunnig = false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playSong(true);
        setUpNotificationData(remoteViews, songs.get(sPosition), notification);
        setSongView(songName, songAuthor, songCover);
        MainActivity.songsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        startPlaying();
    }

    public void setUpSong(Intent intent, boolean isSameSong) {
        songs = intent.getParcelableArrayListExtra("songs_list");
        sPosition = intent.getIntExtra("position", 0);

        if (MusicService.isRunnig && isSameSong) {
            startPlaying();
        } else {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            setUpNotificationData(remoteViews, songs.get(sPosition), notification);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(songs.get(sPosition).getSong_link());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void playSong(boolean is_next) {
        if (is_next) {
            sPosition++;
            if (sPosition == songs.size())
                sPosition = 0;
        } else {
            sPosition--;
            if (sPosition < 0)
                sPosition = songs.size() - 1;
        }
        Log.d("index","inside next/prev : "+sPosition);
        try {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            else {
                remoteViews.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_notif_play);
                manager.notify(NOTIF_ID, notification);
            }
            playPauseBtn.setEnabled(false);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(songs.get(sPosition).getSong_link());
            mediaPlayer.prepareAsync();
            setUpNotificationData(remoteViews, songs.get(sPosition), notification);
            setSongView(songName, songAuthor, songCover);
            MainActivity.songsAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUpNotificationData(RemoteViews remoteViews, Song song, Notification notification) {
        remoteViews.setTextViewText(R.id.song_name_notif, song.getName() + " - " + song.getAuthor_name() );
        NotificationTarget notificationTarget = new NotificationTarget(
                MusicService.this,
                R.id.cover_iv,
                remoteViews,
                notification,
                NOTIF_ID);
        Glide.with(MusicService.this).asBitmap()
                .load(song.getAlbum_cover())
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(20)))
                .into(notificationTarget);
    }


    public void updateSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }


    public void setSongView(final TextView nameSong, final TextView authorSong, final ImageView songCover) {
        Animation fadeOut = AnimationUtils.loadAnimation(MusicService.this, R.anim.fade_out);
        nameSong.startAnimation(fadeOut);
        authorSong.startAnimation(fadeOut);
        songCover.startAnimation(fadeOut);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Glide.with(MusicService.this).load(songs.get(sPosition).getAlbum_cover()).into(songCover);
                nameSong.setText(songs.get(sPosition).getName());
                authorSong.setText(songs.get(sPosition).getAuthor_name());
                Animation fadeIn = AnimationUtils.loadAnimation(MusicService.this, R.anim.fade_in);
                songCover.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void setPlayPauseBtn(ImageButton ppBtn) {
        this.playPauseBtn = ppBtn;
    }

    public void setSongCover(ImageView imageCover) {
        this.songCover = imageCover;
    }

    public void setSongName(TextView songName) {
        this.songName = songName;
    }

    public void setSongAuthor(TextView songAuthor) {
        this.songAuthor = songAuthor;
    }

    public void setSongTotalDuration_tv(TextView songTotalDuration_tv) {
        this.songTotalDuration_tv = songTotalDuration_tv;
    }

    public void setCurrentDuration_tv(TextView songCurrentDuration) {
        this.songCurrentDuration_tv = songCurrentDuration;
    }

    public void setSeekBar(SeekBar seekBar) {
        this.seekBar = seekBar;
    }

    public void setContext(Activity activity) {
        this.context = activity;
    }

    public String createTimeLabel(int duration) {
        String timeLabel = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;
        if (min < 10)
            timeLabel += "0" + min + ":";
        else timeLabel += min + ":";
        if (sec < 10)
            timeLabel += "0" + sec;
        else timeLabel += sec;
        return timeLabel;

    }

    public void startPlaying(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mediaPlayer.isPlaying())
                    mediaPlayer.start();
                duration = mediaPlayer.getDuration();
                seekBar.setMax(duration / 1000);
                playPauseBtn.setImageResource(R.drawable.pause_btn_selector);
                playPauseBtn.setEnabled(true);
                remoteViews.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_notif_pause);
                songTotalDuration_tv.setText(createTimeLabel(duration));
                manager.notify(NOTIF_ID, notification);
                runDurationOnSeekBar();
            }
        }, 250);
    }


    public void runDurationOnSeekBar() {
        context.runOnUiThread(mRunnable);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isRunnig && mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress * 1000);
                    songCurrentDuration_tv.setText(createTimeLabel(mediaPlayer.getCurrentPosition()));
                }
            }
        });
    }
}
