package me.nereo.multi_image_selector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.nereo.multi_image_selector.R;
import me.nereo.multi_image_selector.bean.Folder;

/**
 * Folder Adapter
 * Created by Nereo on 2015/4/7.
 */
public class FolderAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;

    private List<Folder> mFolders = new ArrayList<>();

    int mImageSize;

    Folder lastSelectedFolder = new Folder();

    public FolderAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mImageSize = mContext.getResources().getDimensionPixelOffset(R.dimen.folder_cover_size);
    }

    /**
     * Set data
     *
     * @param folders
     */
    public void setData(List<Folder> folders) {
        if (folders != null && folders.size() > 0) {
            mFolders = folders;
        } else {
            mFolders.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mFolders.size() + 1;
    }

    @Override
    public Folder getItem(int i) {
        return i == 0 ? new Folder() : mFolders.get(i - 1);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_folder, viewGroup, false);
            holder = new ViewHolder(view);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (holder != null) {
            if (i == 0) {
                holder.name.setText(mContext.getString(R.string.folder_all));
//                holder.size.setText(getTotalImageSize() + "张");
                holder.size.setText(String.valueOf(getTotalImageSize()));

                if (mFolders.size() > 0) {
                    Folder f = mFolders.get(0);
                    Picasso.with(mContext)
                            .load(new File(f.cover.path))
                            .error(R.drawable.default_error)
                            .resize(mImageSize, mImageSize)
                            .centerCrop()
                            .into(holder.cover);
                }
            } else {
                holder.bindData(getItem(i));
            }
            boolean visibility = lastSelectedFolder.equals(getItem(i));
            holder.indicator.setVisibility(visibility ? View.VISIBLE : View.INVISIBLE);
        }
        return view;
    }

    private int getTotalImageSize() {
        int result = 0;
        if (mFolders != null && mFolders.size() > 0) {
            for (Folder f : mFolders) {
                result += f.images.size();
            }
        }
        return result;
    }

    public void setSelectIndex(int i) {
        if(lastSelectedFolder.equals(getItem(i))) return;

        lastSelectedFolder = getItem(i);
        notifyDataSetChanged();
    }

    public int getSelectIndex() {
        for (int i = 1; i < getCount(); i++) {
            if(getItem(i).equals(lastSelectedFolder))
                return i;
        }
        return 0;
    }

    class ViewHolder {
        ImageView cover;
        TextView name;
        TextView size;
        ImageView indicator;

        ViewHolder(View view) {
            cover = (ImageView) view.findViewById(R.id.cover);
            name = (TextView) view.findViewById(R.id.name);
            size = (TextView) view.findViewById(R.id.size);
            indicator = (ImageView) view.findViewById(R.id.indicator);
            view.setTag(this);
        }

        void bindData(Folder data) {
            name.setText(data.name);
//            size.setText(data.images.size() + "张");
            size.setText(String.valueOf(data.images.size()));
            // Display pictures
            Picasso.with(mContext)
                    .load(new File(data.cover.path))
                    .placeholder(R.drawable.default_error)
                    .resize(mImageSize, mImageSize)
                    .centerCrop()
                    .into(cover);
            // TODO Select Identity
        }
    }

}
