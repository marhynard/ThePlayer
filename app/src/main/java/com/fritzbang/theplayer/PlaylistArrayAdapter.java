package com.fritzbang.theplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistArrayAdapter extends ArrayAdapter<TrackBean> {

    public static final int TITLE_SORT = 0;
    public static final  int ALBUM_SORT = 1;
    public static final  int ARTIST_SORT = 2;
    public static final  int TRACK_SORT = 3;
	 private static final String DEBUG_TAG = "PlaylistArrayAdapter";
	private final Activity context;
	private ArrayList<TrackBean> trackBeans = new ArrayList<TrackBean>();
	public TrackBean currentTrackInfo;
    boolean isAscending = false;
    int sortType = -1;
    private SparseBooleanArray mSelectedItemsIds;

    public void updateStatus(int trackToPlay,int status,int position) {
        if(!trackBeans.isEmpty()) {
            TrackBean tmp = trackBeans.get(trackToPlay);
            tmp.status = status;
            tmp.position = position;
            trackBeans.set(trackToPlay, tmp);
            Log.d(DEBUG_TAG, "AdapterPosition: " + trackToPlay + " " + position);
            this.notifyDataSetChanged();
        }
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
    }

    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        Log.d(DEBUG_TAG,"selections removed");
        notifyDataSetChanged();
    }

    public void toggleSelection(int position) {
        Log.d(DEBUG_TAG,"toggleSelection");
        selectView(position, !mSelectedItemsIds.get(position));
    }

    private void selectView(int position, boolean value) {
        if(value){
            mSelectedItemsIds.put(position,value);
        }else{
            Log.d(DEBUG_TAG,"selection deleted");
            mSelectedItemsIds.delete(position);
        }
        notifyDataSetChanged();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }


    static class ViewHolder {
		public TextView songTitle;
		public TextView albumTitle;
		public TextView artist;
        public ImageView albumImage;
	}

	public PlaylistArrayAdapter(Activity context,
			ArrayList<TrackBean> trackBeans) {
		super(context, R.layout.playlist_list_entry, trackBeans);
		this.context = context;
		this.trackBeans = trackBeans;
        mSelectedItemsIds = new SparseBooleanArray();
	}
    
    public void addTrack(String songTitle, String fileLocation,
			String albumTitle, String artist) {
		trackBeans.add(new TrackBean(songTitle, fileLocation, artist,
				albumTitle,0,0));
	}
    public void sortTracks(int newSortType){
        if(this.sortType == newSortType){
            this.isAscending = !this.isAscending;
        }else{
            this.sortType = newSortType;
            this.isAscending = true;
        }
        switch(sortType) {
            case TITLE_SORT:
                if(isAscending) {
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return lhs.trackTitle.compareToIgnoreCase(rhs.trackTitle);
                        }
                    });
                }else{
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return rhs.trackTitle.compareToIgnoreCase(lhs.trackTitle);
                        }
                    });
                }
                break;
            case ARTIST_SORT:
                if(isAscending) {
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return lhs.artist.compareToIgnoreCase(rhs.artist);
                        }
                    });
                }else{
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return rhs.artist.compareToIgnoreCase(lhs.artist);
                        }
                    });
                }
                break;
            case ALBUM_SORT:
                if(isAscending) {
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return lhs.album.compareToIgnoreCase(rhs.album);
                        }
                    });
                }else{
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return rhs.album.compareToIgnoreCase(lhs.album);
                        }
                    });
                }
                break;
            case TRACK_SORT:
                if(isAscending) {
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return lhs.location.compareToIgnoreCase(rhs.location);
                        }
                    });
                }else{
                    Collections.sort(trackBeans, new Comparator<TrackBean>() {
                        @Override
                        public int compare(TrackBean lhs, TrackBean rhs) {
                            return rhs.location.compareToIgnoreCase(lhs.location);
                        }
                    });
                }
                break;
        }
    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.playlist_list_entry, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.songTitle = (TextView) rowView.findViewById(R.id.song_title);
			viewHolder.albumTitle = (TextView) rowView.findViewById(R.id.album_title);
			viewHolder.artist = (TextView) rowView.findViewById(R.id.artist);
            viewHolder.albumImage = (ImageView)rowView.findViewById(R.id.album_image);
			rowView.setTag(viewHolder);
		}

		// rowView.setBackgroundColor(color.white);
		ViewHolder holder = (ViewHolder) rowView.getTag();
        if(mSelectedItemsIds.get(position,false)){
            rowView.setBackgroundColor(Color.MAGENTA);
             holder.albumImage.setImageResource(R.drawable.ic_checked);
        }else{
            switch(trackBeans.get(position).status) {
                case TrackBean.TRACK_STATUS_CURRENT:
                    rowView.setBackgroundColor(Color.BLUE);
                    break;
                case TrackBean.TRACK_STATUS_FINISHED:
                    rowView.setBackgroundColor(Color.DKGRAY);
                    break;
                case TrackBean.TRACK_STATUS_PARTIAL:
                    rowView.setBackgroundColor(Color.GRAY);
                    break;
                case TrackBean.TRACK_STATUS_UNPLAYED:
                    rowView.setBackgroundColor(Color.WHITE);
                     break;
            }
            holder.albumImage.setImageResource(R.drawable.list_item_selector);
        }


		holder.songTitle.setText(trackBeans.get(position).trackTitle);
		holder.songTitle.setTag(R.id.TAG_FILE_LOCATION,trackBeans.get(position).location);
		holder.albumTitle.setText(trackBeans.get(position).album);
		holder.artist.setText(trackBeans.get(position).artist);



		return rowView;
	}

	public void delete(int position) {
		trackBeans.remove(position);
	}

    public int getItem(String currentFile) {
        int index = 0;
        for(TrackBean bean: trackBeans){
            if(bean.location.equals(currentFile)){
                return index;
            }
            index++;
        }
        return 0;
    }
}
