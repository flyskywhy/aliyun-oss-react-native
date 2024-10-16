package com.aliyunOssReactNative;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AliyunDownloadManager {
    private OSS mOSS;

    /**
     * AliyunDownloadManager
     * @param oss
     */
    public AliyunDownloadManager(OSS oss) {
        mOSS = oss;
    }

    public void asyncDownload(final ReactContext context, String bucketName, String objectKey, final String filepath, ReadableMap options, final Promise promise) {
        GetObjectRequest get = new GetObjectRequest(bucketName, objectKey);

        String xOssPositon = options.getString("x-oss-process");
        //process image
        get.setxOssProcess(xOssPositon);

        OSSAsyncTask task = mOSS.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {

                Log.d("Content-Length", "" + result.getContentLength());

                InputStream inputStream = result.getObjectContent();
                long resultLength = result.getContentLength();

                byte[] buffer = new byte[2048];
                int len;

                FileOutputStream outputStream = null;
                String downloadToFileURL;
                if (!TextUtils.isEmpty(filepath)) {
                    downloadToFileURL = filepath + File.separator + objectKey;
                } else {
                    downloadToFileURL = context.getFilesDir().getAbsolutePath() + File.separator + objectKey;
                }

                Log.d("downloadToFileURL", downloadToFileURL);
                File cacheFile = new File(downloadToFileURL);
                if (!cacheFile.exists()) {
                    cacheFile.getParentFile().mkdirs();
                    try {
                        cacheFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("DownloadFaile", e);
                    }
                }
                long readSize = 0;
                try {
                    outputStream = new FileOutputStream(cacheFile, false);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    promise.reject("DownloadFaile", e);
                }
                if (resultLength == -1) {
                    promise.reject("DownloadFaile", "message:lengtherror");
                }

                try {
                    while ((len = inputStream.read(buffer)) != -1) {
                       // resove download data
                        try {
                            outputStream.write(buffer, 0, len);
                            readSize += len;

                            String str_currentSize = Long.toString(readSize);
                            String str_totalSize = Long.toString(resultLength);
                            WritableMap onProgressValueData = Arguments.createMap();
                            onProgressValueData.putString("currentSize", str_currentSize);
                            onProgressValueData.putString("totalSize", str_totalSize);
                            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit("downloadProgress", onProgressValueData);

                        } catch (IOException e) {
                            e.printStackTrace();
                            promise.reject("DownloadFaile", e);
                        }
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    promise.reject("DownloadFaile", e);
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            promise.reject("DownloadFaile", e);
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            promise.reject("DownloadFaile", e);
                        }
                    }
                    promise.resolve(downloadToFileURL);
                }
            }

            @Override
            public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                PromiseExceptionManager.resolvePromiseException(clientExcepion,serviceException,promise);
            }
        });
    }
}
