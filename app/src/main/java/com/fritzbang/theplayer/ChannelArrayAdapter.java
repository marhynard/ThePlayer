package com.fritzbang.theplayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ChannelArrayAdapter extends ArrayAdapter<String> {
    private static final String DEBUG_TAG = "ChannelArrayAdapter";
    private final Activity context;
    private final String[] names;
    private final String[] images;
    private final String[] rssIDs;

    public ChannelArrayAdapter(Activity context, String[] names,
                               String images[], String[] rssIDs) {
        super(context, R.layout.rss_list_entry, names);
        this.context = context;
        this.names = names;
        this.images = images;
        this.rssIDs = rssIDs;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.rss_list_entry, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = (TextView) rowView
                    .findViewById(R.id.podcast_title);
            viewHolder.image = (ImageView) rowView
                    .findViewById(R.id.podcast_image);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        String name = names[position];
        String image = images[position];
        Log.d(DEBUG_TAG, "names: " + name);
        Log.d(DEBUG_TAG, "image: " + image);
        holder.text.setText(name);
        holder.text.setTag(rssIDs[position]);

        if (image == null) {
            holder.image.setImageResource(R.drawable.ic_launcher);
        } else {
            File imgFile = new File(image);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile
                        .getAbsolutePath());
                holder.image.setImageBitmap(myBitmap);

            } else {
                holder.image.setImageResource(R.drawable.ic_launcher);
            }
        }
        return rowView;
    }

    static class ViewHolder {
        public TextView text;
        public ImageView image;
    }
}
