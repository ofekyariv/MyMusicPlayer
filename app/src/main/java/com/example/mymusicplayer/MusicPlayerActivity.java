package com.example.mymusicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MusicPlayerActivity extends AppCompatActivity {
    MusicService mService;
    boolean mBound = false;
    private ArrayList<Song> songs;
    private ImageView album_cover;
    private TextView song_name;
    private TextView author_name;
    private TextView song_total_duration;
    private TextView song_current_duration;
    private ImageButton back_btn;
    private ImageButton next_btn;
    private ImageButton prev_btn;
    private ImageButton play_pause_btn;
    private SeekBar seekBarDuration;

//    private int position;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_player_layout);

        Log.d("bdika", "on create now");

        play_pause_btn = findViewById(R.id.play_pause_btn);
        play_pause_btn.setEnabled(false);
        back_btn = findViewById(R.id.back_btn);
        next_btn = findViewById(R.id.next_btn);
        prev_btn = findViewById(R.id.prev_btn);
        album_cover = findViewById(R.id.song_cover_iv);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            album_cover.setClipToOutline(true);
        }
        song_name = findViewById(R.id.song_name_tv);
        author_name = findViewById(R.id.song_author_tv);
        song_total_duration = findViewById(R.id.total_duration_tv);
        song_current_duration = findViewById(R.id.current_duration);
        seekBarDuration = findViewById(R.id.song_progress);

//        position = getIntent().getIntExtra("position", 0);
        songs = getIntent().getParcelableArrayListExtra("songs_list");

        final Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }


    @Override
    protected void onStart() {

        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartService("next", MusicService.sPosition, 1);
            }
        });

        prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartService("prev", MusicService.sPosition, -1);
            }
        });

        play_pause_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartService("play", MusicService.sPosition, 0);
            }
        });
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d("state", "on stop now");
        mBound = false;
        super.onStop();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService != null) {
                mService.setContext(MusicPlayerActivity.this);
                mService.setPlayPauseBtn(play_pause_btn);
                mService.setSongName(song_name);
                mService.setSongAuthor(author_name);
                mService.setSongCover(album_cover);
                mService.setSongTotalDuration_tv(song_total_duration);
                mService.setCurrentDuration_tv(song_current_duration);
                mService.setSeekBar(seekBarDuration);
                mService.setSongView(song_name, author_name, album_cover);

                unbindService(connection);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    public void restartService(String command, int position, int next_prev) {
        Intent service = new Intent(MusicPlayerActivity.this, MusicService.class);
        if (MusicService.isRunnig) {
            service.putExtra("command", command);
        } else {
            if (position + next_prev == songs.size()) {
                position = 0;
            } else if (position + next_prev < 0)
                position = songs.size() - 1;
            else
                position = position + next_prev;
            service.putExtra("position", position);
            service.putExtra("songs_list", songs);
            service.putExtra("command", "new_instance");
            bindService(service, connection, Context.BIND_AUTO_CREATE);
        }
        startService(service);
    }

}
