package com.alibaba.mnnllm.android.api;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.alibaba.mnnllm.android.R;

public class ApiSettingsFragment extends Fragment implements ApiStatusView {
    private Switch apiServiceSwitch;
    private EditText portEditText;
    private TextView statusTextView;
    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,@Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_api_settings, container, false);
        apiServiceSwitch = view.findViewById(R.id.switch_api_service);
        portEditText = view.findViewById(R.id.edit_port);
        statusTextView = view.findViewById(R.id.text_api_status);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        
        setupViews();
        return view;
    }

    private void setupViews() {
        // 获取保存的端口号，默认8080
        int savedPort = prefs.getInt("api_port", 8080);
        portEditText.setText(String.valueOf(savedPort));
        // 获取服务状态
        boolean isServiceRunning = ApiManager.getInstance().isServiceBound();
        apiServiceSwitch.setChecked(isServiceRunning);
        
        // 确保初始状态下 text_api_status 是隐藏的
        statusTextView.setVisibility(View.GONE);
        
        apiServiceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ApiManager.getInstance().startApiService();
            } else {
                ApiManager.getInstance().stopApiService();
            }
        });
        portEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int port = Integer.parseInt(s.toString());
                    if (port >= 1024 && port <= 65535) {
                        prefs.edit().putInt("api_port", port).apply();
                        ApiManager.getInstance().updatePort(port);
                        Toast.makeText(requireContext(), getString(R.string.port_changed, port), 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                            R.string.invalid_port,
                            Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(),
                        R.string.invalid_port,
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        updateStatus(isServiceRunning);
    }

    @Override
    public void updateStatus(boolean isRunning) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            if (isRunning) {
                int port = prefs.getInt("api_port", 8080);
                statusTextView.setText(getString(R.string.api_status_running, port));
                statusTextView.setVisibility(View.VISIBLE); // 显示状态文本
            } else {
                statusTextView.setText(R.string.api_status_stopped);
                statusTextView.setVisibility(View.GONE); // 隐藏状态文本
            }
        });
        apiServiceSwitch.setChecked(isRunning); // 确保开关状态与API服务状态一致
    }

    @Override
    public void onResume() {
        super.onResume();
        ApiManager.getInstance().setStatusView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        ApiManager.getInstance().setStatusView(null);
    }
}