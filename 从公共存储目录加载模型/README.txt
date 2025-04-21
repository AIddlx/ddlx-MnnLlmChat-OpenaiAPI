代码为简化思路，测试模为Qwen2-0.5B-Instruct-MNN，模型文件放在

/storage/emulated/0/modelscope/Qwen2-0.5B-Instruct-MNN

模型文件不要用git clone获取到的，必须是用魔搭、魔乐sdk获取到的，或者从之前从MNN下载缓存中拷贝过来的
测试时需要先获取管理所有文件的权限,然后清退并重新打开MNN，等待5秒后跳转ChatActivity.java并自动加载模型

以下是app\src\main\AndroidManifest.xml需要新增的权限
       <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />


我的b站
https://space.bilibili.com/3493283904883140
