package com.giszone.app;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.giszone.filepicker.FilePickerDialog;

import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv).setOnClickListener(v -> {
            new FilePickerDialog(this)
                    .setTitleText("选择影像路径：")
                    .setSelectMode(FilePickerDialog.MODE_MULTI)
                    .setSelectType(FilePickerDialog.TYPE_ALL)
                    .setRootDir(Environment.getExternalStorageDirectory().getAbsolutePath())
                    .setPrimaryDir(Environment.getExternalStorageDirectory().getAbsolutePath() + "/StitchTest")
                    .setExtensions(null)
                    .setBackCancelable(false)
                    .setOutsideCancelable(false)
                    .setDialogListener("确定", "取消", new FilePickerDialog.FileDialogListener() {
                        @Override
                        public void onSelectedFilePaths(String[] filePaths) {
                            Log.d("MainTAG", "MainActivity.onSelectedFilePaths: " + Arrays.toString(filePaths));
                        }

                        @Override
                        public void onCanceled() {

                        }
                    })
                    .show();
        });

    }

}
