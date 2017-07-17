package com.z7dream.upload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.z7dream.upload.lib.OSSUpload;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OSSUpload.init(Appli.getContext(), Appli.getBoxStore());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OSSUpload.destory(Appli.getContext());
    }
}
