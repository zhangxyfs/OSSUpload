# OSSUpload
### 本项目是基于阿里云OSS文件上传的二次封装(我仅仅提供一种解决方案)
#### 原理如下：
![](https://github.com/zhangxyfs/OSSUpload/blob/master/example/upload_tactics.jpg)

#### 如何调用？

MainActivity.class
```java
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
```
##### 仅仅初始化一次即可。
##### 由于上传功能和业务逻辑关系比较紧密，需要大家根据自己的业务需求修改UploadManager.class，OSSUpload.class
