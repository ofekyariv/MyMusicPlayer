package com.example.mymusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    MusicService mService;
    boolean mBound = false;

    public static SongsAdapter songsAdapter;
    private ArrayList<Song> songs;
    private RecyclerView recyclerView;
    private FloatingActionButton addBtn;
    private FloatingActionButton playAllBtn;

    private ImageView dialog_cover_iv;
    private File photoFile;
    private Uri photoUri;

    public static final int CAMERA_REQUEST = 1;
    public static final int CAMERA_PICK_FROM_GALLERY = 2;
    public static final int WRITE_REQUEST = 3;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(getString(R.string.app_name));

        addBtn = findViewById(R.id.add_Btn);
        playAllBtn = findViewById(R.id.play_all_btn);


        recyclerView = findViewById(R.id.recycler_layout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        File fileSongs = getFileStreamPath("songs.dat");
        if (fileSongs.exists()) {
            readSongsFromFile();
        } else {
            initFirstRunSongs();
        }

        songsAdapter = new SongsAdapter(this, songs);
        recyclerView.setAdapter(songsAdapter);


        playAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songs.size() > 0) {
                    Intent service = new Intent(MainActivity.this, MusicService.class);
                    service.putExtra("position", 0);
                    service.putExtra("songs_list", songs);
                    service.putExtra("command", "new_instance");
                    startService(service);
                    bindService(service, connection, BIND_AUTO_CREATE);

                    Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                    intent.putExtra("position", 0);
                    intent.putExtra("songs_list", songs);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
            }
        });

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.DOWN | ItemTouchHelper.UP, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull final RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromTarget = viewHolder.getAdapterPosition();
                int toTarget = target.getAdapterPosition();
                Song tempSong = songs.get(fromTarget);
                songs.remove(fromTarget);
                songs.add(toTarget, tempSong);

                if (MusicService.isRunnig) {
                    if (fromTarget < MusicService.sPosition && toTarget >= MusicService.sPosition) {
                        MusicService.sPosition -= 1;
                    } else if (fromTarget > MusicService.sPosition && toTarget <= MusicService.sPosition) {
                        MusicService.sPosition += 1;
                    } else if (fromTarget == MusicService.sPosition) {
                        MusicService.sPosition = toTarget;
                    }

                }
                songsAdapter.notifyItemMoved(fromTarget, toTarget);

                saveSongsToFile();

                if (MusicService.isRunnig) {
                    Intent service = new Intent(MainActivity.this, MusicService.class);
                    service.putExtra("songs_list", songs);
                    service.putExtra("command", "songs_update");
                    startService(service);
                }
                return true;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, int direction) {
                showSongDeleteDialog(viewHolder);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        songsAdapter.setListener(new SongsAdapter.MySongListener() {
            @Override
            public void OnSongClicked(View view, int position) {
                Intent service = new Intent(MainActivity.this, MusicService.class);
                if (MusicService.isRunnig && position == MusicService.sPosition) {
                    service.putExtra("same_song", true);
                }
                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                intent.putExtra("songs_list", songs);
                intent.putExtra("position",position);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                service.putExtra("position", position);
                service.putExtra("songs_list", songs);
                service.putExtra("command", "new_instance");
                bindService(service, connection, BIND_AUTO_CREATE);
                startService(service);
            }
        });
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.updateSongs(songs);
            songsAdapter.notifyDataSetChanged();

            unbindService(connection);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onStop() {
        mBound = false;
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Cant take picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            dialog_cover_iv.setVisibility(View.VISIBLE);
            Glide.with(MainActivity.this).load(photoUri).into(dialog_cover_iv);
            dialog_cover_iv.requestLayout();

        } else if (requestCode == CAMERA_PICK_FROM_GALLERY && resultCode == RESULT_OK) {
            dialog_cover_iv.setVisibility(View.VISIBLE);
            photoUri = data.getData();
            Glide.with(MainActivity.this).load(data.getData()).into((dialog_cover_iv));
        }
    }

    private void readSongsFromFile() {
        try {
            FileInputStream fileInputStream = openFileInput("songs.dat");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            songs = (ArrayList<Song>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSongsToFile() {
        try {
            FileOutputStream fileOutputStream = openFileOutput("songs.dat", MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(songs);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initFirstRunSongs() {
        songs = new ArrayList<>();
        songs.add(new Song("Blinding Lights",
                "The Weekend",
                "https://upload.wikimedia.org/wikipedia/he/e/e6/The_Weeknd_-_Blinding_Lights.png",
                "https://www.mboxdrive.com/The%20Weeknd%20-%20Blinding%20Lights.mp3"));
        songs.add(new Song("Stressed Out",
                "Twenty One Pilots",
                "https://upload.wikimedia.org/wikipedia/he/4/48/TOP_Stressed_Out.jpg",
                "https://www.mboxdrive.com/Stressed%20Out%20by%20twenty%20one%20pilots%20lyrics.mp3"));
        songs.add(new Song("The Man In Me",
                "Bob Dylan",
                "https://images.genius.com/7f81fe234452ddc42d1ba5fd56d1f8ea.500x500x1.jpg",
                "http://www.syntax.org.il/xtra/bob2.mp3"));
        songs.add(new Song("Sara",
                "Bob Dylan",
                "https://southerncrossreview.org/125/sara-and-bob.jpg",
                "http://www.syntax.org.il/xtra/bob1.m4a"));
        songs.add(new Song("One More Cup Of Coffee",
                "Bob Dylan",
                "https://m.media-amazon.com/images/I/71fYldgAi0L._SS500_.jpg",
                "http://www.syntax.org.il/xtra/bob.m4a"));
        songs.add(new Song("Boulevard Of Broken Dreams",
                "Green Day",
                "https://upload.wikimedia.org/wikipedia/he/c/c5/Green_Day_-_Boulevard_Of_Broken_Dreams.jpg",
                "https://www.mboxdrive.com/Green%20Day%20-%20Boulevard%20of%20Broken%20Dreams.mp3"));
        songs.add(new Song("SOS",
                "Avicii",
                "https://m.media-amazon.com/images/I/71vfRyK6lGL._SS500_.jpg",
                "https://www.mboxdrive.com/Avicii%20-%20SOS%20ft.%20Aloe%20Blacc.mp3"));
        songs.add(new Song("Let You Down",
                "NF",
                "https://upload.wikimedia.org/wikipedia/en/4/42/NF_let_you_down_single_cover.jpg",
                "https://www.mboxdrive.com/NF%20%E2%80%93%20Let%20You%20Down%20%F0%9F%8E%B5.mp3"));
        songs.add(new Song("Sara",
                "Bob Dylan",
                "https://southerncrossreview.org/125/sara-and-bob.jpg",
                "http://www.syntax.org.il/xtra/bob1.m4a"));
        songs.add(new Song("Blinding Lights",
                "The Weekend",
                "https://upload.wikimedia.org/wikipedia/he/e/e6/The_Weeknd_-_Blinding_Lights.png",
                "https://www.mboxdrive.com/The%20Weeknd%20-%20Blinding%20Lights.mp3"));
        songs.add(new Song("Stressed Out",
                "Twenty One Pilots",
                "https://upload.wikimedia.org/wikipedia/he/4/48/TOP_Stressed_Out.jpg",
                "https://www.mboxdrive.com/Stressed%20Out%20by%20twenty%20one%20pilots%20lyrics.mp3"));
        songs.add(new Song("Boulevard Of Broken Dreams",
                "Green Day",
                "https://upload.wikimedia.org/wikipedia/he/c/c5/Green_Day_-_Boulevard_Of_Broken_Dreams.jpg",
                "https://www.mboxdrive.com/Green%20Day%20-%20Boulevard%20of%20Broken%20Dreams.mp3"));
        songs.add(new Song("SOS",
                "Avicii",
                "https://m.media-amazon.com/images/I/71vfRyK6lGL._SS500_.jpg",
                "https://www.mboxdrive.com/Avicii%20-%20SOS%20ft.%20Aloe%20Blacc.mp3"));
        songs.add(new Song("Let You Down",
                "NF",
                "https://upload.wikimedia.org/wikipedia/en/4/42/NF_let_you_down_single_cover.jpg",
                "https://www.mboxdrive.com/NF%20%E2%80%93%20Let%20You%20Down%20%F0%9F%8E%B5.mp3"));
        songs.add(new Song("Blinding Lights",
                "The Weekend",
                "https://upload.wikimedia.org/wikipedia/he/e/e6/The_Weeknd_-_Blinding_Lights.png",
                "https://www.mboxdrive.com/The%20Weeknd%20-%20Blinding%20Lights.mp3"));
        songs.add(new Song("Stressed Out",
                "Twenty One Pilots",
                "https://upload.wikimedia.org/wikipedia/he/4/48/TOP_Stressed_Out.jpg",
                "https://www.mboxdrive.com/Stressed%20Out%20by%20twenty%20one%20pilots%20lyrics.mp3"));
        songs.add(new Song("Boulevard Of Broken Dreams",
                "Green Day",
                "https://upload.wikimedia.org/wikipedia/he/c/c5/Green_Day_-_Boulevard_Of_Broken_Dreams.jpg",
                "https://www.mboxdrive.com/Green%20Day%20-%20Boulevard%20of%20Broken%20Dreams.mp3"));
        songs.add(new Song("SOS",
                "Avicii",
                "https://m.media-amazon.com/images/I/71vfRyK6lGL._SS500_.jpg",
                "https://www.mboxdrive.com/Avicii%20-%20SOS%20ft.%20Aloe%20Blacc.mp3"));
        songs.add(new Song("One More Cup Of Coffee",
                "Bob Dylan",
                "https://m.media-amazon.com/images/I/71fYldgAi0L._SS500_.jpg",
                "http://www.syntax.org.il/xtra/bob.m4a"));
        songs.add(new Song("Let You Down",
                "NF",
                "https://upload.wikimedia.org/wikipedia/en/4/42/NF_let_you_down_single_cover.jpg",
                "https://www.mboxdrive.com/NF%20%E2%80%93%20Let%20You%20Down%20%F0%9F%8E%B5.mp3"));
        songs.add(new Song("Blinding Lights",
                "The Weekend",
                "https://upload.wikimedia.org/wikipedia/he/e/e6/The_Weeknd_-_Blinding_Lights.png",
                "https://www.mboxdrive.com/The%20Weeknd%20-%20Blinding%20Lights.mp3"));
        songs.add(new Song("Stressed Out",
                "Twenty One Pilots",
                "https://upload.wikimedia.org/wikipedia/he/4/48/TOP_Stressed_Out.jpg",
                "https://www.mboxdrive.com/Stressed%20Out%20by%20twenty%20one%20pilots%20lyrics.mp3"));
        songs.add(new Song("One More Cup Of Coffee",
                "Bob Dylan",
                "https://m.media-amazon.com/images/I/71fYldgAi0L._SS500_.jpg",
                "http://www.syntax.org.il/xtra/bob.m4a"));
        songs.add(new Song("Boulevard Of Broken Dreams",
                "Green Day",
                "https://upload.wikimedia.org/wikipedia/he/c/c5/Green_Day_-_Boulevard_Of_Broken_Dreams.jpg",
                "https://www.mboxdrive.com/Green%20Day%20-%20Boulevard%20of%20Broken%20Dreams.mp3"));
        songs.add(new Song("SOS",
                "Avicii",
                "https://m.media-amazon.com/images/I/71vfRyK6lGL._SS500_.jpg",
                "https://www.mboxdrive.com/Avicii%20-%20SOS%20ft.%20Aloe%20Blacc.mp3"));
        songs.add(new Song("Let You Down",
                "NF",
                "https://upload.wikimedia.org/wikipedia/en/4/42/NF_let_you_down_single_cover.jpg",
                "https://www.mboxdrive.com/NF%20%E2%80%93%20Let%20You%20Down%20%F0%9F%8E%B5.mp3"));
        saveSongsToFile();
    }


    public void showAlertDialog() {

        if (Build.VERSION.SDK_INT >= 23) {
            int hasWritePremission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWritePremission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST);
            }
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.Theme_MaterialComponents_Dialog_Alert);
        View view = getLayoutInflater().inflate(R.layout.add_song_dialog, null);

        Button cancel = view.findViewById(R.id.cancel_button_dialog);
        Button add = view.findViewById(R.id.add_btn_dialog);
        ImageButton addPhoto = view.findViewById(R.id.add_photo_dialog);
        ImageButton pickPhoto = view.findViewById(R.id.add_photo_alternate_dialog);
        final TextInputEditText songName_et = view.findViewById(R.id.song_name_dialog);
        final TextInputEditText songAuthor_et = view.findViewById(R.id.song_author_dialog);
        final TextInputEditText songLink_et = view.findViewById(R.id.song_link_dialog);
        dialog_cover_iv = view.findViewById(R.id.cover_iv_dialog);

        photoFile = null;

        final AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.setView(view);
        dialog.setCancelable(false);
        dialog.show();
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        dialog.getWindow().setAttributes(params);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (songName_et.length() == 0) {
                    songName_et.setError(getResources().getString(R.string.dialog_name_error));
                } else if (songAuthor_et.length() == 0) {
                    songAuthor_et.setError(getResources().getString(R.string.dialog_author_error));
                } else if (songLink_et.length() == 0) {
                    songLink_et.setError(getResources().getString(R.string.dialog_link_error));
                } else {
                    String songName = songName_et.getText().toString();
                    String songAuthor = songAuthor_et.getText().toString();
                    String songLink = songLink_et.getText().toString();
                    songs.add(new Song(songName, songAuthor, photoUri.toString(), songLink));
                    songsAdapter.notifyItemInserted(songs.size() - 1);

                    Intent service = new Intent(MainActivity.this, MusicService.class);
                    service.putExtra("songs_list", songs);
                    service.putExtra("command", "songs_update");
                    startService(service);

                    saveSongsToFile();
                    dialog.dismiss();
                }
            }
        });

        addPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoFile = new File(Environment.getExternalStorageDirectory(), "song" + (songs != null ? songs.size() : 0) + ".jpg");
                photoUri = FileProvider.getUriForFile(MainActivity.this
                        , "com.example.mymusicplayer.provider",
                        photoFile);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            }

        });

        pickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPicture = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPicture, CAMERA_PICK_FROM_GALLERY);
            }
        });
    }

    public void showSongDeleteDialog(final RecyclerView.ViewHolder viewHolder){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.Theme_MaterialComponents_Dialog_Alert);
        final View view = getLayoutInflater().inflate(R.layout.delete_song_dialog, null);

        Button cancel = view.findViewById(R.id.cancel_button_dialog);
        Button delete = view.findViewById(R.id.delete_song_button);

        final AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.setView(view);
        dialog.setCancelable(false);
        dialog.show();
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        dialog.getWindow().setAttributes(params);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                songsAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                dialog.dismiss();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Song song = (Song) songs.get(viewHolder.getAdapterPosition());
                final int lastPos = viewHolder.getAdapterPosition();
                songs.remove(viewHolder.getAdapterPosition());
                songsAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                final Intent service = new Intent(MainActivity.this, MusicService.class);

                Snackbar.make(recyclerView, R.string.song_delete_snackbar, Snackbar.LENGTH_LONG).setActionTextColor(getResources().getColor(R.color.colorAccent))
                        .setAction(R.string.song_delete_cancel, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                songs.add(lastPos, song);
                                songsAdapter.notifyItemInserted(lastPos);
                                if (MusicService.sPosition >= lastPos) {
                                    MusicService.sPosition += 1;;
                                }
                                saveSongsToFile();
                                service.putExtra("songs_list", songs);
                                service.putExtra("command", "songs_update");
                                startService(service);
                            }
                        }).show();

                if (MusicService.sPosition > lastPos && MusicService.isRunnig) {
                    MusicService.sPosition -= 1;
                } else if (MusicService.sPosition == lastPos && MusicService.isRunnig) {
                    MusicService.sPosition -= 1;
                    service.putExtra("songs_list", songs);
                    service.putExtra("command", "songs_update");
                    startService(service);
                    service.putExtra("command", "next");
                    startService(service);
                }
                saveSongsToFile();
                service.putExtra("songs_list", songs);
                service.putExtra("command", "songs_update");
                startService(service);
                dialog.dismiss();
            }
        });

    }

}