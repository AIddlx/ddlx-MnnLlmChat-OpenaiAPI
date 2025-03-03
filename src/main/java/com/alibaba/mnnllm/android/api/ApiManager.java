package com.alibaba.mnnllm.android.api;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.util.Log;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.alibaba.mnnllm.android.ChatSession;

public class ApiManager implements ServiceConnection {
    private static final String TAG = "ApiManager";
    private static ApiManager instance;
    private Context context;
    private boolean isModelLoaded = false;
    private ChatSession currentSession;
    private ApiStatusView statusView;
    private OpenAICompatibleService.ApiServiceBinder serviceBinder;
    private boolean isServiceBound = false;
   private int currentPort = 8080;

    // 添加 ChatSessionBinder 内部类
    public class ChatSessionBinder extends Binder {
        private final ChatSession session;
        
        public ChatSessionBinder(ChatSession session) {
            this.session = session;
        }
        
        public ChatSession getSession() {
            return session;
        }
    }

    private ApiManager() {}

    public static ApiManager getInstance() {
        if (instance == null) {
            instance = new ApiManager();
        }
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize with null context");
            return;
        }
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
       currentPort = prefs.getInt("api_port", 8080);Log.i(TAG, "ApiManager initialized with context");
        this.context = context.getApplicationContext(); // 使用 ApplicationContext 避免内存泄漏
        Log.i(TAG, "ApiManager initialized with context");
    }

    public void setStatusView(ApiStatusView view) {
        this.statusView = view;
    }

    public boolean isModelLoaded() {
        return isModelLoaded;
    }
    
    public void setCurrentSession(ChatSession session) {
        this.currentSession = session;
    }

    public ChatSession getCurrentSession() {
        return currentSession;
    }

    public void setModelLoaded(boolean loaded) {
        isModelLoaded = loaded;
        if (loaded) {
            Log.i(TAG, "Model loading completed, checking context...");
            if (context != null) {
                startApiService();
                Log.i(TAG, "Model loaded successfully, API service started");
            } else {
                Log.e(TAG, "Context not initialized, please call init() first");
            }            
        }
    }

    public void startApiService() {
        if (context == null) {
            Log.e(TAG, "Context not initialized");
            return;
        }

        if (!isModelLoaded) {
            Log.e(TAG, "Model not loaded yet");
            return;
        }
        try {
            bindApiService();

            Intent intent = new Intent(context, OpenAICompatibleService.class);
           intent.putExtra("port", currentPort);
            context.startService(intent);
        
            if (statusView != null) {
                statusView.updateStatus(true);
            }
           Log.i(TAG, "OpenAI compatible API service started on port " + currentPort);Log.i(TAG, "Available endpoints:");
            Log.i(TAG, "Available endpoints:");
            Log.i(TAG, "  GET /v1/status - Check server and model status");
            Log.i(TAG, "  GET /v1/models - List available models");
            Log.i(TAG, "  POST /v1/chat/completions - Chat completion endpoint");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start API service: " + e.getMessage());
            isModelLoaded = false;
        }
    }

   public void updatePort(int port) {
       if (port != currentPort) {
           currentPort = port;
           if (isServiceBound) {
               stopApiService();
               startApiService();
           }
       }
   }
    private void bindApiService() {
        Intent intent = new Intent(context, OpenAICompatibleService.class);
        // 设置包名以支持跨应用调用
        intent.setPackage(context.getPackageName());
        context.bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof OpenAICompatibleService.ApiServiceBinder) {
            serviceBinder = (OpenAICompatibleService.ApiServiceBinder) service;
            isServiceBound = true;
            Log.i(TAG, "API Service bound successfully");
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        serviceBinder = null;
        isServiceBound = false;
        Log.i(TAG, "API Service unbound");
    }

    public boolean isServiceBound() {
        return isServiceBound;
    }

    public OpenAICompatibleService.ApiServiceBinder getServiceBinder() {
        return serviceBinder;
    }

    public void stopApiService() {
        if (context == null) {
            Log.e(TAG, "Context not initialized");
            return;
        }
        if (isServiceBound) {
            context.unbindService(this);
            isServiceBound = false;
        }
        context.stopService(new Intent(context, OpenAICompatibleService.class));
        if (statusView != null) {
            statusView.updateStatus(false);
        }
        Log.i(TAG, "OpenAI compatible API service stopped");
    }
}