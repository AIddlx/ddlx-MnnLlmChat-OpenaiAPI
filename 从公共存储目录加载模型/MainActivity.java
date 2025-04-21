// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.

package com.alibaba.mnnllm.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.alibaba.mls.api.download.ModelDownloadManager;
import com.alibaba.mnnllm.android.chat.ChatActivity;
import com.alibaba.mnnllm.android.history.ChatHistoryFragment;
import com.alibaba.mnnllm.android.modelist.ModelListFragment;
import com.alibaba.mnnllm.android.settings.MainSettings;
import com.alibaba.mnnllm.android.update.UpdateChecker;
import com.alibaba.mnnllm.android.utils.GithubUtils;
import com.alibaba.mnnllm.android.utils.ModelUtils;
import com.techiness.progressdialoglibrary.ProgressDialog;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private ProgressDialog progressDialog;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private ModelListFragment modelListFragment;
    private ChatHistoryFragment chatHistoryFragment;
    private UpdateChecker updateChecker;


    /**
     * 用于启动权限请求的 ActivityResultLauncher
     * 实现日期：2025年4月21日 创作者：ddlx
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        updateChecker = new UpdateChecker(this);
        updateChecker.checkForUpdates(this, false);

        // Set up ActionBar toggle
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                toolbar,
                R.string.nav_open,
                R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container,
                        getModelListFragment())
                .commit();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.history_fragment_container,
                        getChatHistoryFragment())
                .commit();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
 
        // 初始化权限请求启动器
        //实现日期：2025年4月21日 创作者：ddlx
        initializePermissionLauncher();

        // 如果需要，请求存储权限
        //实现日期：2025年4月21日 创作者：ddlx
        requestStoragePermissionIfNeeded();

        // 延迟并运行模型
        // 实现日期：2025年4月21日 创作者：ddlx
        delayAndRunModel();
    }


    private Fragment getModelListFragment() {
        if (modelListFragment == null) {
            modelListFragment = new ModelListFragment();
        }
        return modelListFragment;
    }

    private Fragment getChatHistoryFragment() {
        if (chatHistoryFragment == null) {
            chatHistoryFragment = new ChatHistoryFragment();
        }
        return chatHistoryFragment;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    /**
     * 初始化权限请求发射器
     *
     * 使用 registerForActivityResult 方法注册一个新的 ActivityResultContracts.RequestPermission 类型的发射器。
     * 当权限请求的结果返回时，会调用回调函数。如果权限未被授予，则显示一个 Toast 提示用户需要存储权限。
     * 实现日期：2025年4月21日 创作者：ddlx
     */
    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "需要存储权限", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * 根据系统版本请求存储权限
     *
     * @return 无返回值
     * 实现日期：2025年4月21日 创作者：ddlx
     */
    private void requestStoragePermissionIfNeeded() {
        // 检查是否已经拥有管理外部存储的权限
        if (Environment.isExternalStorageManager()) {
            // 如果已经有权限，则记录日志并返回
            Log.d(TAG, "已拥有 MANAGE_EXTERNAL_STORAGE 权限。");
            return;
        }

        // 检查 Android 版本是否为 R (API 30) 或更高
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android R 及以上版本，请求 MANAGE_EXTERNAL_STORAGE 权限
            Log.d(TAG, "为 Android R+ 请求 MANAGE_EXTERNAL_STORAGE 权限");
            try {
                // 创建意图以请求所有文件访问权限
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                // 如果启动 Activity 出错，记录错误日志
                Log.e(TAG, "启动 MANAGE_ALL_FILES_ACCESS_PERMISSION activity 时出错", e);
                // 尝试再次启动，不指定特定包
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent); // Try again without specific package
            }
        } else {
            // Android R 以下版本，请求传统的存储权限
            // 检查 Android 版本是否为 Android 14 (API 34) 或更高
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14 及以上版本，请求 READ_MEDIA_IMAGES 权限
                Log.d(TAG, "为 Android 14+ 请求 READ_MEDIA_IMAGES 权限");
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                // Android 14 以下版本，请求 READ_EXTERNAL_STORAGE 权限
                Log.d(TAG, "为低于 Android 14 的版本请求 READ_EXTERNAL_STORAGE 权限");
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }
    /**
     * 延迟5秒后运行模型
     *
     * 该方法会创建一个 Handler 实例，并设置一个延迟5秒的 Runnable，当延迟时间到达后，将执行 runModel 方法。
     * 实现日期：2025年4月21日 创作者：ddlx
     */
    private void delayAndRunModel() {
        // 创建一个 Handler 实例
        Handler handler = new Handler(Looper.getMainLooper());
        // 延迟5秒后执行 runModel 方法
        handler.postDelayed(() -> runModel(null, null, null), 5000); // 5000 毫秒 = 5 秒
    }

    /**
     * 运行模型
     *
     * @param destModelDir 目标模型目录
     * @param modelId        模型ID
     * @param sessionId    会话ID
     * 实现日期：2025年4月21日 创作者：ddlx
     */
    public void runModel(String destModelDir, String modelId, String sessionId) {
        // 固定模型路径和 modelId
        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/modelscope/Qwen2-0.5B-Instruct-MNN";
        modelId = "taobao-mnn/Qwen2-0.5B-Instruct-MNN";
        destModelDir = absolutePath; // 使用固定路径


        Log.d(TAG, "运行模型 destModelDir: " + destModelDir);
        if (MainSettings.INSTANCE.isStopDownloadOnChatEnabled(this)) {
            ModelDownloadManager.getInstance(this).pauseAllDownloads();
        }
        drawerLayout.close();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.model_loading));
        progressDialog.show();

        // 移除扩散模型判断逻辑
        String configFileName = "config.json";
        String configFilePath = destModelDir + "/" + configFileName;
        boolean configFileExists = new File(configFilePath).exists();
        if (!configFileExists) {
            Toast.makeText(this, getString(R.string.config_file_not_found, configFilePath), Toast.LENGTH_LONG).show();
            progressDialog.dismiss();
            return;
        }

        progressDialog.dismiss();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatSessionId", sessionId);
        // 移除扩散模型相关的额外参数设置
        intent.putExtra("configFilePath", configFilePath);
        intent.putExtra("modelId", modelId);
        intent.putExtra("modelName", "Qwen2-0.5B-Instruct-MNN");


        startActivity(intent);
    }

    public void onStarProject(View view) {
        GithubUtils.starProject(this);
    }

    public void onReportIssue(View view) {
        GithubUtils.reportIssue(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ModelDownloadManager.REQUEST_CODE_POST_NOTIFICATIONS) {
            ModelDownloadManager.getInstance(this).startForegroundService();
        }
    }

    public void checkForUpdate() {
        updateChecker.checkForUpdates(this, true);
    }
}