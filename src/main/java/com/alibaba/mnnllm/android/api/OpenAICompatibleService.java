package com.alibaba.mnnllm.android.api;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;

import com.alibaba.mnnllm.android.ChatSession;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response; // 显式导入Response类，否则会报错

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenAICompatibleService extends Service {
    private static final String TAG = "OpenAICompatibleService";
   private int port = 8080;
    private ApiServer server;
    private final ApiServiceBinder binder = new ApiServiceBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        try {
           port = PreferenceManager.getDefaultSharedPreferences(this)
               .getInt("api_port", 8080);
            server = new ApiServer();
            server.start();
           Log.i(TAG, "API server started on port " + port);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start API server: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    public class ApiServiceBinder extends Binder {
        private boolean isServerRunning = false;

        public boolean isServerRunning() {
            return isServerRunning;
        }

        public OpenAICompatibleService getService() {
            return OpenAICompatibleService.this;
        }

        public void setServerRunning(boolean running) {
            isServerRunning = running;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service bound");
        if (server != null) {
            binder.setServerRunning(true);
        }
        return binder;
    }

    private class ApiServer extends NanoHTTPD {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final Object chatLock = new Object();

    public ApiServer() throws IOException {
           super("0.0.0.0", port);
    }

    @Override
    protected boolean useGzipWhenAccepted(Response r) {
        return false;
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server: " + e.getMessage());
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Log.i(TAG, String.format("Incoming request: %s %s", method, uri));
        Map<String, String> headers = session.getHeaders();
        Log.d(TAG, "Request Headers:");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            Log.d(TAG, String.format("%s: %s", header.getKey(), header.getValue()));
        }

        totalRequests.incrementAndGet();

        // 处理 OPTIONS 预检请求
        if (Method.OPTIONS.equals(method)) {
            Response response = super.newFixedLengthResponse(Response.Status.OK, "application/json", "");
            addCorsHeaders(response);
            response.addHeader("Connection", "keep-alive");
            response.addHeader("Keep-Alive", "timeout=5, max=100");
            successRequests.incrementAndGet();
            return response;
        }

        try {
            Response response = handleRequest(session, uri);
            addCorsHeaders(response);

            if (uri.equals("/v1/chat/completions")) {
                response.addHeader("Content-Type", "text/event-stream");
                response.addHeader("Cache-Control", "no-cache");
                response.addHeader("Connection", "keep-alive");
            }
            return response;
        } catch (JSONException e) {
            failedRequests.incrementAndGet();
            return super.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private void addCorsHeaders(Response response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "*");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.addHeader("Access-Control-Max-Age", "86400");
        response.addHeader("Access-Control-Allow-Credentials", "true");
    }

    private Response handleRequest(IHTTPSession session, String uri) throws JSONException {
        try {
            switch (uri) {
                case "/v1/chat/completions":
                    return handleChatCompletions(session);
                case "/v1/models":
                    return handleModels(session);
                case "/v1/status":
                    return handleStatus(session);
                default:
                    failedRequests.incrementAndGet();
                    return super.newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                            new JSONObject().put("error", "Not found").toString());
            }
        } catch (JSONException e) {
            failedRequests.incrementAndGet();
            throw e;
        }
    }

private Response handleStatus(IHTTPSession session) throws JSONException {
    if (!Method.GET.equals(session.getMethod())) {
        if (!Method.GET.equals(session.getMethod())) {
            failedRequests.incrementAndGet();
            return super.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json",
                    new JSONObject().put("error", "Method not allowed").toString());
        }

        JSONObject status = new JSONObject()
                .put("model_loaded", ApiManager.getInstance().isModelLoaded());
        successRequests.incrementAndGet();
        return super.newFixedLengthResponse(Response.Status.OK, "application/json", status.toString());
    }
    return null;
}
    private Response handleModels(IHTTPSession session) throws JSONException {
        if (!Method.GET.equals(session.getMethod())) {
            failedRequests.incrementAndGet();
            return super.newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json",
                    new JSONObject().put("error", "Method not allowed").toString());
        }

        JSONObject model = new JSONObject()
                .put("id", "mnn-local")
                .put("object", "model")
                .put("owned_by", "alibaba")
                .put("permission", new JSONArray());

        JSONObject response = new JSONObject()
                .put("object", "list")
                .put("data", new JSONArray().put(model));

        successRequests.incrementAndGet();
        return super.newFixedLengthResponse(Response.Status.OK, "application/json", response.toString());
    }

    private Response handleChatCompletions(IHTTPSession session) throws JSONException {
    if (!Method.POST.equals(session.getMethod())) {
        failedRequests.incrementAndGet();
        return createErrorResponse(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed",
                "invalid_request_error", null);
    }

    if (!ApiManager.getInstance().isModelLoaded()) {
        failedRequests.incrementAndGet();
        return createErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Model not loaded yet",
                "server_error", "service_unavailable");
    }

    synchronized (chatLock) {
        String authHeader = session.getHeaders().get("authorization");
        if (authHeader == null || authHeader.isEmpty()) {
            failedRequests.incrementAndGet();
            return createErrorResponse(Response.Status.UNAUTHORIZED, "You didn't provide an API key.",
                    "invalid_request_error", "invalid_api_key");
        }

        try {
            // 直接读取原始字节数据并解码为UTF-8字符串
            byte[] bodyBytes = new byte[session.getInputStream().available()];
            session.getInputStream().read(bodyBytes);
            String requestBody = new String(bodyBytes, StandardCharsets.UTF_8);

            Log.i(TAG, "Chat completion request received: " + requestBody);

            JSONObject request = new JSONObject(requestBody);
            if (!request.has("model")) {
                failedRequests.incrementAndGet();
                return createErrorResponse(Response.Status.BAD_REQUEST, "Required parameter 'model' is missing",
                        "invalid_request_error", "model");
            }

            JSONArray messages = request.getJSONArray("messages");
            if (messages.length() == 0) {
                failedRequests.incrementAndGet();
                return createErrorResponse(Response.Status.BAD_REQUEST, "Messages array is empty",
                        "invalid_request_error", "messages");
            }

            // 构建完整的上下文字符串
            StringBuilder contextBuilder = new StringBuilder();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                contextBuilder.append(message.getString("role")).append(": ").append(message.getString("content")).append("\n");
            }
            String context = contextBuilder.toString();

            ChatSession chatSession = ApiManager.getInstance().getCurrentSession();
            if (chatSession == null) {
                failedRequests.incrementAndGet();
                return createErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "No active chat session available",
                        "server_error", "service_unavailable");
            }

            chatSession.reset(); // 确保清空之前的对话状态
            Response response = handleStreamingResponse(chatSession, context);
            chatSession.reset(); // 请求处理完成后再次调用reset()确保状态清除
            return response;

        } catch (IOException e) {
            failedRequests.incrementAndGet();
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Failed to process request: " + e.getMessage(),
                    "server_error", null);
        }
    }
}


    private Response createErrorResponse(Response.Status status, String message, String type, String code) throws JSONException {
        JSONObject error = new JSONObject()
                .put("message", message)
                .put("type", type);
        if (code != null) {
            error.put("code", code);
        }
        return super.newFixedLengthResponse(status, "application/json",
                new JSONObject().put("error", error).toString());
    }

    private Response handleStreamingResponse(ChatSession chatSession, String userMessage) {
    try {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream();
        in.connect(out);

        Response streamResponse = super.newChunkedResponse(Response.Status.OK, "text/event-stream", in);
        streamResponse.addHeader("X-Accel-Buffering", "no");
        streamResponse.addHeader("Content-Type", "text/event-stream; charset=utf-8");

        Thread generationThread = new Thread(() -> {
            try {
                String responseId = "chatcmpl-" + System.currentTimeMillis();
                long created = System.currentTimeMillis() / 1000;

                chatSession.generate(userMessage, progress -> {
                    try {
                        if (progress != null) {
                            JSONObject delta = createDeltaResponse(responseId, created, progress);
                            String sseMessage = "data: " + delta.toString() + "\n\n";
                            out.write(sseMessage.getBytes(StandardCharsets.UTF_8));
                            out.flush();
                            Log.d(TAG, "Sent chunk: " + progress);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error writing to stream: " + e.getMessage());
                        return true; // 停止生成
                    }
                    return false; // 继续生成
                });

                // 发送完成标记
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception e) {
                Log.e(TAG, "Error in generation process: " + e.getMessage());
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing output stream: " + e.getMessage());
                }
            }
        });
        generationThread.start();

        successRequests.incrementAndGet();
        return streamResponse;
    } catch (IOException e) {
        failedRequests.incrementAndGet();
        try {
            return createErrorResponse(Response.Status.INTERNAL_ERROR, "Failed to initialize streaming response",
                    "server_error", null);
        } catch (JSONException je) {
            return super.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\": \"Internal server error\"}");
        }
    }
}

    private JSONObject createDeltaResponse(String responseId, long created, String content) throws JSONException {
        String safeContent = content;
        try {
            // 确保内容是有效的UTF-8文本
            if (content != null) {
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                safeContent = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error encoding content: " + e.getMessage());
            // 如果编码失败，尝试移除不可打印字符
            safeContent = content != null ? content.replaceAll("[^\\p{Print}\\s]", "") : "";
        }
        return new JSONObject()
                .put("id", responseId)
                .put("object", "chat.completion.chunk")
                .put("created", created)
                .put("model", "mnn-local")
                .put("choices", new JSONArray()
                        .put(new JSONObject()
                                .put("delta", new JSONObject()
                                        .put("content", safeContent))
                                .put("finish_reason", null)
                                .put("index", 0)));
    }
}}