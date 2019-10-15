
package com.giszone.filepicker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.giszone.filepicker.filepicker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;


/**
 * Adapter Class that extends {@link BaseAdapter} that is
 * used to populate {@link ListView} with file info.
 */
public class FileListAdapter extends BaseAdapter {
    private ArrayList<FileListItem> listItem;
    private Context context;
    private FileItemSelectedListener fileItemSelectedListener;
    private int selectType;
    private int selectMode;

    public FileListAdapter(Context context, ArrayList<FileListItem> listItem, int selectType, int selectMode) {
        this.context = context;
        this.listItem = listItem;
        this.selectType = selectType;
        this.selectMode = selectMode;
    }

    @Override
    public int getCount() {
        return listItem.size();
    }

    @Override
    public FileListItem getItem(int i) {
        return listItem.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        final ViewHolder holder;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.dialog_file_list_item, viewGroup, false);
            holder = new ViewHolder();
            holder.tvName = view.findViewById(R.id.tv_file_name);
            holder.tvInfo = view.findViewById(R.id.tv_file_info);
            holder.ivType = view.findViewById(R.id.iv_file_type);
            holder.cbMark = view.findViewById(R.id.cb_file_mark);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final FileListItem item = listItem.get(position);

        if (MarkedItemList.hasItem(item.getPath())) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.marked_item_animation);
            view.setAnimation(animation);
        } else {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.unmarked_item_animation);
            view.setAnimation(animation);
        }

        String name = item.getName();
        boolean isDirectory = item.isDirectory();

        holder.ivType.setImageResource(isDirectory ? R.mipmap.ic_type_folder : R.mipmap.ic_type_file);
        holder.ivType.setColorFilter(context.getResources().getColor(isDirectory ? R.color.colorPrimary : R.color.colorAccent, context.getTheme()));
        holder.ivType.setContentDescription(name);

        holder.tvName.setText(name);

        if (position == 0 && name.startsWith(context.getString(R.string.label_parent_dir))) {
            holder.tvInfo.setText(R.string.label_parent_directory);
            holder.cbMark.setVisibility(View.INVISIBLE);
        } else {
            holder.tvInfo.setText(new SimpleDateFormat("yyyy/dd/MM HH:mm", Locale.getDefault()).format(item.getTime()));
            switch (selectType) {
                case FilePickerDialog.TYPE_FILE:
                    holder.cbMark.setVisibility(item.isDirectory() ? View.INVISIBLE : View.VISIBLE);
                    break;
                case FilePickerDialog.TYPE_DIR:
                    holder.cbMark.setVisibility(item.isDirectory() ? View.VISIBLE : View.INVISIBLE);
                    break;
                case FilePickerDialog.TYPE_ALL:
                default:
                    holder.cbMark.setVisibility(View.VISIBLE);
                    break;
            }
        }


        holder.cbMark.setOnCheckedChangeListener(null);
        if (MarkedItemList.hasItem(item.getPath())) {
            holder.cbMark.setChecked(true);
        } else {
            holder.cbMark.setChecked(false);
        }

        holder.cbMark.setOnCheckedChangeListener((checkbox, isChecked) -> {
            if (isChecked) {
                if (selectMode == FilePickerDialog.MODE_MULTI) {
                    String[] paths = MarkedItemList.getSelectedPaths();
                    for (String path : paths) {
                        if (path != null && item.getPath().startsWith(path + "/")) {
                            // 已经勾选了item的父目录，则勾选无效
                            Toast.makeText(context, "已勾选上层文件夹", Toast.LENGTH_SHORT).show();
                            checkbox.setChecked(false);
                            return;
                        } else if (path != null && path.startsWith(item.getPath() + "/")) {
                            // item包含已勾选的其他子路径，子路径移除
                            MarkedItemList.removeSelectedItem(path);
                        }
                    }
                    MarkedItemList.addMultiItem(item);
                } else {
                    MarkedItemList.addSingleFile(item);
                }
            } else {
                MarkedItemList.removeSelectedItem(item.getPath());
            }
            item.setMarked(isChecked);
            fileItemSelectedListener.onFileItemSelected();
            notifyDataSetChanged();
        });
        return view;
    }

    private class ViewHolder {
        ImageView ivType;
        TextView tvName, tvInfo;
        CheckBox cbMark;
    }

    public void setFileItemSelectedListener(FileItemSelectedListener notifyItemChecked) {
        this.fileItemSelectedListener = notifyItemChecked;
    }


    public interface FileItemSelectedListener {

        /**
         * Called when a checkbox is checked.
         */
        void onFileItemSelected();
    }
}
