package com.example.mymusicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Song implements Parcelable, Serializable {
    private String name;
    private String author_name;
    private int song_duration;
    private String album_cover;
    private String song_link;

    public Song(String name, String author_name, String album_cover, String song_link) {
        this.name = name;
        this.author_name = author_name;
        this.song_duration = 0;
        this.album_cover = album_cover;
        this.song_link = song_link;
    }

    protected Song(Parcel in) {
        name = in.readString();
        author_name = in.readString();
        song_duration = in.readInt();
        album_cover = in.readString();
        song_link = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(author_name);
        dest.writeInt(song_duration);
        dest.writeString(album_cover);
        dest.writeString(song_link);


    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor_name() {
        return author_name;
    }

    public void setAuthor_name(String author_name) {
        this.author_name = author_name;
    }

    public int getSong_duration() {
        return song_duration;
    }

    public void setSong_duration(int song_duration) {
        this.song_duration = song_duration;
    }

    public String getAlbum_cover() {
        return album_cover;
    }

    public void setAlbum_cover(String album_cover) {
        this.album_cover = album_cover;
    }

    public String getSong_link() {
        return song_link;
    }

    public void setSong_link(String song_link) {
        this.song_link = song_link;
    }

}
