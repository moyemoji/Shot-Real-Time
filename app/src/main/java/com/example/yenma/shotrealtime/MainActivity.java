package com.example.yenma.shotrealtime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "moye";
    private TextureView textureView;
    private Button btnTakePic;
    private Camera mCamera;
    private static int cacheNum = 5;
    private static int cacheCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        detectCamera();
        initListener();
    }


    /**
     * 初始化私有变量
     * textureView：摄像头预览窗口
     * banTackPic：按钮
     */
    private void initView() {
        textureView = (TextureView) findViewById(R.id.texture_view);
        btnTakePic = (Button) findViewById(R.id.btn_takePic);
    }


    /**
     * 摄像头检测
     * 不存在摄像头时finish退出
     */
    private void detectCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();
        if (numberOfCameras < 1) {
            Toast toast = Toast.makeText(this, "No Camera!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 100);
            toast.show();
            finish();
            return;
        }
    }


    /**
     * 为按钮绑定点击事件
     */
    private void initListener() {
        textureView.setSurfaceTextureListener(this);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCallBack();
            }
        });
    }


    /**
     * 摄像头预览回调事件
     */
    private void addCallBack() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override  // 每更新一帧图像时，执行以下程序
                public void onPreviewFrame(byte[] data, Camera camera) {
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    try {
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if (image != null) {
                            // 预览二进制数据解码为为Bitmap
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            stream.close();

                            // 将Bitmap保存为JPG
                            saveImageToGallery(getApplicationContext(), bmp);
                            Log.i(TAG, "Saving Image...");

                            // TODO 调用分类模型，分类成功则：1.解绑预览图回调函数 2.将此图保存至图像采集专用文件夹
//                            mCamera.setPreviewCallback(null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /**
     * 保存帧图像到相册
     *
     * @param context 应用上下文
     * @param bitmap  位图文件
     */
    public static void saveImageToGallery(Context context, Bitmap bitmap) {
        // 创建应用目录
        File appDir = new File(Environment.getExternalStorageDirectory(), "ShotRealTime");
        // 目录不存在则创建
        if (!appDir.exists()) {
            appDir.mkdir();
            boolean isFilemaked1 = appDir.isDirectory();
            boolean isFilemaked2 = appDir.mkdirs();
            if (isFilemaked1 || isFilemaked2) {
                Log.i(TAG, "Directory Created");
            } else {
                Log.i(TAG, "Create Directory Failed");
            }
        }

        // 保存图片
        String fileName = (cacheCount % cacheNum) + ".jpg";
        cacheCount++;
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast toast = Toast.makeText(context, "Keep Moving...", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 100);
        toast.show();
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable...");
        // 打开相机 0后置 1前置
        mCamera = Camera.open(0);
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            // 设置相机预览宽高，此处设置为TextureView宽高
            params.setPreviewSize(width, height);
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                mCamera.setParameters(params);
            }
            try {
                // 设置预览角度，并不改变获取到的原始数据方向
                mCamera.setDisplayOrientation(90);
                // 绑定相机和预览的View
                mCamera.setPreviewTexture(surface);
                // 开始预览
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
