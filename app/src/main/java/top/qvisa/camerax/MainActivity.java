package top.qvisa.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.camera.core.ImageCapture.CaptureMode.MIN_LATENCY;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    String currentPhotoPath;
    private TextureView textureView;
    private Button button;
    private Executor executor = Executors.newSingleThreadExecutor();
    String base64 = null;
    String path = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);
        button = findViewById(R.id.button);
        Button button2 = findViewById(R.id.button2);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
        } else {
            startCamera();
        }

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(base64!=null){
                    try{
                        Pattern pattern = Pattern.compile("\t|\r|\n|\\s*");
                        Matcher matcher = pattern.matcher(base64);
                        String dest = matcher.replaceAll("");

                        FileOutputStream outputStream = openFileOutput("hello.text", Context.MODE_PRIVATE);
                        outputStream.write( dest.getBytes());
                        outputStream.close();
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    String[] files = fileList();
                    for (String file : files) {
                        Log.d("file--",file);
                    }
                }
            }
        });


    }

    public void startCamera() {

        int width = 2448;
        int height = 2448;

        //Preview
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(width,height))
//               .setLensFacing(CameraX.LensFacing.FRONT)
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(
                previewOutput -> {
                    ViewGroup parent = (ViewGroup) textureView.getParent();
                    parent.removeView(textureView);
                    parent.addView(textureView, 0);
                    SurfaceTexture surfaceTexture = previewOutput.getSurfaceTexture();
                    textureView.setSurfaceTexture(surfaceTexture);

                });

        //ImageCapture
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetResolution(new Size(width,height))
//                .setLensFacing(CameraX.LensFacing.FRONT)
                .build();
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = null;
                try {
                    file = createImageFile();
                } catch (Exception e) {
                }
                imageCapture.takePicture(file, executor,
                        new ImageCapture.OnImageSavedListener() {
                            @Override
                            public void onImageSaved(File file) {
                                path = file.getAbsolutePath();
                                base64 =imageToBase64(path);
                            }

                            @Override
                            public void onError(@NonNull ImageCapture.ImageCaptureError imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                Log.d("Main", message);
                            }
                        });
            }
        });

        CameraX.bindToLifecycle((LifecycleOwner) this, preview, imageCapture);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.CHINA).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,   //prefix
                ".jpeg",         // suffix
                storageDir       //directory
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //动态权限请求回调方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static String imageToBase64(String path) {
        //decode to bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        Log.d("Main", "bitmap width: " + bitmap.getWidth() + " height: " + bitmap.getHeight());

        // 定义矩阵对象
        Matrix matrix = new Matrix();

        // 向左旋转（逆时针旋转）45度，参数为正则向右旋转（顺时针旋转）
        matrix.postRotate(90);

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                matrix, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30,baos);
        byte[] bytes = baos.toByteArray();
        //base64 encode
        byte[] encode = Base64.encode(bytes,Base64.DEFAULT);
        return new String(encode);
    }
}
