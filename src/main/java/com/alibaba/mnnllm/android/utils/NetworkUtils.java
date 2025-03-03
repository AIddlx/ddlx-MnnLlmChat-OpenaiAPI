package com.alibaba.mnnllm.android.utils;

import android.util.Log;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.net.InetAddress;
import java.net.Inet4Address;
import com.alibaba.mnnllm.android.utils.NetworkUtils;


public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // 首选 WLAN 接口
            for (NetworkInterface ni : interfaces) {
                if (ni.getName().toLowerCase().contains("wlan") || ni.getName().toLowerCase().contains("eth")) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    for (InetAddress address : addresses) {
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            String ip = address.getHostAddress();
                            Log.i(TAG, "Using network interface: " + ni.getName() + ", IP: " + ip);
                            return ip;
                        }
                    }
                }
            }
            // 如果没有找到 WLAN，则使用任何可用的网络接口
            for (NetworkInterface ni : interfaces) {
                if (!ni.isLoopback() && ni.isUp()) {
                    List<InetAddress> addresses = Collections.list(ni.getInetAddresses());
                    for (InetAddress address : addresses) {
                        if (address instanceof Inet4Address) {
                            String ip = address.getHostAddress();
                            Log.i(TAG, "Fallback to network interface: " + ni.getName() + ", IP: " + ip);
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address: " + e.getMessage());
        }
        Log.w(TAG, "No suitable network interface found, using localhost");
        return "127.0.0.1";
    }
}