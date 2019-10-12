
package com.giszone.filepicker;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.giszone.filepicker.filepicker.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class FilePickerDialog extends Dialog implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    public static final String DEFAULT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final int EXTERNAL_READ_PERMISSION_GRANT = 0x1a;
    public static final int MODE_SINGLE = 0;
    public static final int MODE_MULTI = 1;
    public static final int TYPE_ALL = 0;
    public static final int TYPE_FILE = 1;
    public static final int TYPE_DIR = 2;

    private Context context;
    private ListView listView;
    private TextView tvDirPath, tvTitle;

    private Button btnCancel;
    private Button btnSelect;

    private ArrayList<FileListItem> internalList;
    private FileListAdapter mFileListAdapter;
    private ExtensionFilter filter;
    private FileDialogListener listener;

    private String titleStr, selectBtnText, cancelBtnText;

    private int selectMode;
    private int selectType;
    private File rootDir;
    private File primaryDir;
    private String[] extensions;

    private String curDirPath;

    private HashMap<String, Integer> positionMap;

    public FilePickerDialog(Context context) {
        super(context);
        this.context = context;
        internalList = new ArrayList<>();
        selectMode = MODE_SINGLE;
        selectType = TYPE_FILE;
        rootDir = new File(DEFAULT_DIR);
        primaryDir = new File(DEFAULT_DIR);
        extensions = new String[]{""};
        positionMap = new HashMap<>();
        this.setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_main);

        listView = findViewById(R.id.lv_files);
        btnSelect = findViewById(R.id.btn_select);
        btnCancel = findViewById(R.id.btn_cancel);
        tvTitle = findViewById(R.id.tv_dialog_title);
        tvDirPath = findViewById(R.id.tv_dir_path);

        filter = new ExtensionFilter(selectType, extensions);

        int size = MarkedItemList.getFileCount();
        if (size == 0) {
            btnSelect.setEnabled(false);
            int color = context.getResources().getColor(R.color.colorAccent);
            btnSelect.setTextColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)));
        }

        if (cancelBtnText != null) {
            btnCancel.setText(cancelBtnText);
        }

        btnSelect.setOnClickListener(v -> {
            String[] paths = MarkedItemList.getSelectedPaths();
            if (listener != null) {
                listener.onSelectedFilePaths(paths);
            }
            dismiss();
        });
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCanceled();
            }
            dismiss();
        });

        mFileListAdapter = new FileListAdapter(context, internalList, selectType, selectMode);
        mFileListAdapter.setFileItemSelectedListener(() -> {
            selectBtnText = selectBtnText == null ? context.getResources().getString(R.string.choose_button_label) : selectBtnText;
            int size1 = MarkedItemList.getFileCount();
            int color = context.getResources().getColor(R.color.colorAccent, context.getTheme());
            if (size1 == 0) {
                btnSelect.setEnabled(false);
                btnSelect.setTextColor(Color.argb(128, Color.red(color), Color.green(color), Color.blue(color)));
                btnSelect.setText(selectBtnText);
            } else {
                btnSelect.setEnabled(true);
                btnSelect.setTextColor(color);
                btnSelect.setText(String.format(Locale.getDefault(), "%s (%d) ", selectBtnText, size1));
            }
        });
        listView.setAdapter(mFileListAdapter);
        setTitle();
    }

    private void setTitle() {
        if (!TextUtils.isEmpty(titleStr)) {
            tvTitle.setText(titleStr);
        } else {
            tvTitle.setText("当前路径：");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        selectBtnText = (selectBtnText == null ? context.getResources().getString(R.string.choose_button_label) : selectBtnText);
        btnSelect.setText(selectBtnText);
        if (Utility.checkStorageAccessPermissions(context)) {
            File currLoc;
            internalList.clear();
            if (primaryDir.isDirectory() && validateOffsetPath()) {
                currLoc = new File(primaryDir.getAbsolutePath());
                internalList.add(new FileListItem(
                        context.getString(R.string.label_parent_dir),
                        currLoc.getParentFile().getAbsolutePath(),
                        currLoc.lastModified(),
                        true));
            } else if (rootDir.exists() && rootDir.isDirectory()) {
                currLoc = new File(rootDir.getAbsolutePath());
            } else {
                currLoc = new File(DEFAULT_DIR);
            }
            setCurDirPath(currLoc.getAbsolutePath());
            setTitle();
            internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
            mFileListAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            listView.setOnItemClickListener(this);
            listView.setOnScrollListener(this);
        }
    }

    private boolean validateOffsetPath() {
        String offset_path = primaryDir.getAbsolutePath();
        String root_path = rootDir.getAbsolutePath();
        return !offset_path.equals(root_path) && offset_path.contains(root_path);
    }

    private void setCurDirPath(String dirPath) {
        this.curDirPath = dirPath;
        tvDirPath.setText(curDirPath);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (internalList.size() > i) {
            FileListItem fItem = internalList.get(i);
            if (fItem.isDirectory()) {
                if (new File(fItem.getPath()).canRead()) {
                    File currLoc = new File(fItem.getPath());
                    Integer position = 0;

                    if (i == 0) {
                        positionMap.remove(curDirPath);
                        position = positionMap.get(currLoc.getAbsolutePath());
                        if (position == null || position < 0) {
                            position = 0;
                        }
                    }
                    String tempCurDirPath = curDirPath;

                    setCurDirPath(currLoc.getAbsolutePath());
                    internalList.clear();
                    if (!currLoc.getName().equals(rootDir.getName())) {
                        internalList.add(new FileListItem(
                                context.getString(R.string.label_parent_dir),
                                currLoc.getParentFile().getAbsolutePath(),
                                currLoc.lastModified(),
                                true));
                    }
                    internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
                    mFileListAdapter.notifyDataSetChanged();

                    if (position == 0) {
                        for (int j = 0; j < internalList.size(); j++) {
                            FileListItem item = internalList.get(j);
                            if (item.getPath().equals(tempCurDirPath)) {
                                position = j;
                            }
                        }
                    }

                    listView.setSelection(position);
                } else {
                    Toast.makeText(context, R.string.error_dir_access, Toast.LENGTH_SHORT).show();
                }
            } else {
                CheckBox fmark = view.findViewById(R.id.cb_file_mark);
                fmark.performClick();
            }
        }
    }


    public FilePickerDialog setDialogListener(CharSequence selectBtnText, CharSequence cancelBtnText, FileDialogListener listener) {
        if (selectBtnText != null) {
            this.selectBtnText = selectBtnText.toString();
        } else {
            this.selectBtnText = null;
        }
        if (cancelBtnText != null) {
            this.cancelBtnText = cancelBtnText.toString();
        } else {
            this.cancelBtnText = null;
        }
        this.listener = listener;
        return this;
    }

    public FilePickerDialog setTitleText(CharSequence titleStr) {
        if (titleStr != null) {
            this.titleStr = titleStr.toString();
        } else {
            this.titleStr = null;
        }
        if (tvTitle != null) {
            setTitle();
        }
        return this;
    }

    public FilePickerDialog setSelectMode(int selectMode) {
        this.selectMode = selectMode;
        return this;
    }

    public FilePickerDialog setSelectType(int selectType) {
        this.selectType = selectType;
        return this;
    }

    public FilePickerDialog setRootDir(String rootDir) {
        this.rootDir = new File(rootDir);
        return this;
    }

    public FilePickerDialog setPrimaryDir(String primaryDir) {
        this.primaryDir = new File(primaryDir);
        return this;
    }

    public FilePickerDialog setExtensions(String[] extensions) {
        this.extensions = extensions;
        return this;
    }

    public FilePickerDialog setBackCancelable(boolean cancelable) {
        this.setCancelable(cancelable);
        return this;
    }

    public FilePickerDialog setOutsideCancelable(boolean cancelable) {
        this.setCanceledOnTouchOutside(cancelable);
        return this;
    }

    public void markFiles(List<String> paths) {
        if (paths != null && paths.size() > 0) {
            int count = (selectMode == MODE_SINGLE) ? 1 : paths.size();
            for (int i = 0; i < count; i++) {
                String path = paths.get(i);
                File temp = new File(path);
                if (temp.exists()) {
                    switch (selectType) {
                        case TYPE_DIR:
                            if (temp.isDirectory()) {
                                MarkedItemList.addMultiItem(new FileListItem(
                                        temp.getName(),
                                        temp.getAbsolutePath(),
                                        temp.lastModified(),
                                        true,
                                        true
                                ));
                            }
                            break;
                        case TYPE_FILE:
                            if (temp.isFile()) {
                                MarkedItemList.addMultiItem(new FileListItem(
                                        temp.getName(),
                                        temp.getAbsolutePath(),
                                        temp.lastModified(),
                                        false,
                                        true
                                ));
                            }
                            break;
                        case TYPE_ALL:
                            MarkedItemList.addMultiItem(new FileListItem(
                                    temp.getName(),
                                    temp.getAbsolutePath(),
                                    temp.lastModified(),
                                    temp.isDirectory(),
                                    true
                            ));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void show() {
        if (Utility.checkStorageAccessPermissions(context)) {
            super.show();
            selectBtnText = selectBtnText == null ? context.getResources().getString(R.string.choose_button_label) : selectBtnText;
            btnSelect.setText(selectBtnText);
            int size = MarkedItemList.getFileCount();
            if (size == 0) {
                btnSelect.setText(selectBtnText);
            } else {
                String button_label = selectBtnText + " (" + size + ") ";
                btnSelect.setText(button_label);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //currentDirName is dependent on dirName
        String currentDirName = new File(curDirPath).getName();
        if (internalList.size() > 0) {
            FileListItem fItem = internalList.get(0);
            File currLoc = new File(fItem.getPath());
            if (currentDirName.equals(rootDir.getName()) || !currLoc.canRead()) {
                super.onBackPressed();
            } else {
                positionMap.remove(curDirPath);
                Integer position = positionMap.get(currLoc.getAbsolutePath());
                if (position == null || position < 0) {
                    position = 0;
                }

                setCurDirPath(currLoc.getAbsolutePath());

                internalList.clear();
                if (!currLoc.getName().equals(rootDir.getName())) {
                    internalList.add(new FileListItem(
                            context.getString(R.string.label_parent_dir),
                            currLoc.getParentFile().getAbsolutePath(),
                            currLoc.lastModified(),
                            true));
                }
                internalList = Utility.prepareFileListEntries(internalList, currLoc, filter);
                mFileListAdapter.notifyDataSetChanged();

                if (position == 0) {
                    for (int i = 0; i < internalList.size(); i++) {
                        FileListItem item = internalList.get(i);
                        if (item.getName().equals(currentDirName)) {
                            position = i;
                        }
                    }
                }
                listView.setSelection(position);
            }
            setTitle();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void dismiss() {
        MarkedItemList.clearSelectionList();
        internalList.clear();
        super.dismiss();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Integer position = positionMap.get(curDirPath);
        if (position == null || position != firstVisibleItem) {
            positionMap.put(curDirPath, firstVisibleItem);
            Log.d("MainTAG", "FilePickerDialog.onScroll: " + positionMap.toString());
        }
    }

    public interface FileDialogListener {

        void onSelectedFilePaths(String[] filePaths);

        void onCanceled();
    }
}
