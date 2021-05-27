package com.example.mymusicplayer;


import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {

    private Context context;
    private ArrayList<Song> songs;
    private MySongListener listener;
    // variable to track event time
    private long mLastClickTime = 0;
//    private int mSelectedItem = MusicService.sPosition;

    //-----------View Holder-----------//
    public class SongViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        RelativeLayout relativeLayout;
        ImageView album_cover;
        TextView song_name;
        TextView author_name;
        ImageView disk_iv;

        public SongViewHolder(@NonNull final View itemView) {
            super(itemView);
            itemView.setClickable(true);
            relativeLayout = itemView.findViewById(R.id.content_layout);
            cardView = itemView.findViewById(R.id.song_cardview);
            album_cover = itemView.findViewById(R.id.song_cover_iv);
            album_cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                album_cover.setClipToOutline(true);
            }
            song_name = itemView.findViewById(R.id.song_name_tv);
            author_name = itemView.findViewById(R.id.song_author_tv);
            disk_iv = itemView.findViewById(R.id.disk_image);
            relativeLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        // Preventing multiple clicks, using threshold of 1 second
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
//                        mSelectedItem = getAdapterPosition();
//                        Log.d("position","mSelected : "+mSelectedItem);
//                        notifyDataSetChanged();
                        mLastClickTime = SystemClock.elapsedRealtime();
                        listener.OnSongClicked(v, getAdapterPosition());
                    }
                }
            });

        }
    }
//-----------View Holder-----------//

    //-----------MySongInterface interface-----------//
    public interface MySongListener {
        public void OnSongClicked(View view, int position);
    }

    public void setListener(MySongListener listener) {
        this.listener = listener;
    }
//-----------MySongInterface interface-----------//


    public SongsAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_layout, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final SongViewHolder holder, final int position) {
        Song song = songs.get(holder.getAdapterPosition());
        Glide.with(context).load(song.getAlbum_cover()).into(holder.album_cover);
        holder.author_name.setText(song.getAuthor_name());
        holder.song_name.setText(song.getName());

        if(MusicService.sPosition == position) {
            holder.song_name.setTextColor(context.getResources().getColor(R.color.colorAccent));
            holder.disk_iv.setVisibility(View.VISIBLE);
            Animation rotation = AnimationUtils.loadAnimation(context,R.anim.rotation);
            holder.disk_iv.startAnimation(rotation);

        }
        else {
            holder.song_name.setTextColor(context.getResources().getColor(R.color.colorPrimary));
            holder.disk_iv.clearAnimation();
            holder.disk_iv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

//    public int getmSelectedItem() {
//        return mSelectedItem;
//    }

//    public void setmSelectedItem(int mSelectedItem) {
//        this.mSelectedItem = mSelectedItem;
//    }
}
