package com.z7dream.upload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.z7dream.upload.lib.OSSUpload;
import com.z7dream.upload.lib.db.bean.TaskInfo;
import com.z7dream.upload.lib.listener.UploadListener;
import com.z7dream.upload.lib.manager.UploadManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OSSUpload.init(Appli.getContext(), Appli.getBoxStore());

        OSSUpload.upload("xxx/xxx.png", null, 1L, UploadManager.PU_IM, null, null, null, new UploadListener() {
            @Override
            public void onSuccess(TaskInfo info) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OSSUpload.destory(Appli.getContext());
    }
}
