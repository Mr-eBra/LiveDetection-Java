package com.LiveDetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 200;

    private Button startCamera;
    private Button stopCamera;
    private PreviewView mPreviewView;
    private TextView textView;

    private FaceDetector detector;
    private FaceDetectorOptions option;

    private ProcessCameraProvider cameraProvider;
    private Executor AnalysisBackgroundExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startCamera = findViewById(R.id.startCamera);
        stopCamera = findViewById(R.id.stopCamera);
        mPreviewView = findViewById(R.id.previewView);
        textView = findViewById(R.id.text);

        option = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.2f)
                .build();

        detector = FaceDetection.getClient(option);




        if (checkPermission()) {

            Log.i(TAG, ":: Permission Approved :: ");

            startCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    startCamera.setVisibility(View.INVISIBLE);
                    stopCamera.setVisibility(View.VISIBLE);
                    mPreviewView.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.VISIBLE);

                    startCamera();

                }
            });

            stopCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    startCamera.setVisibility(View.VISIBLE);
                    stopCamera.setVisibility(View.INVISIBLE);
                    mPreviewView.setVisibility(View.INVISIBLE);
                    textView.setVisibility(View.INVISIBLE);

                    StopCamera();
                }
            });


        } else {
            Log.i(TAG, ":: Permission Not Approved :: ");
            requestPermission();
        }

    }



    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }


    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    cameraProvider = cameraProviderFuture.get();

                    Log.i(TAG, ":: Start Camera :: ");
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) { }

            }
        }, ContextCompat.getMainExecutor(this));
    }



    private void bindPreview( ProcessCameraProvider cameraProvider) {


        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Log.i(TAG, ":: init CameraSelector  :: ");


        Preview preview = new Preview.Builder()
                .build();

        Log.i(TAG, ":: init Preview  :: ");


        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        Log.i(TAG, ":: init ImageAnalysis  :: ");

        imageAnalysis.setAnalyzer(AnalysisBackgroundExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull final ImageProxy imageProxy) {

                @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();

                if (mediaImage != null) {

                    Log.i(TAG, ":: Media Image Not NULL :: ");

                    InputImage imageBitmap = InputImage.fromMediaImage(mediaImage,imageProxy.getImageInfo().getRotationDegrees());
                    detector.process(imageBitmap)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<Face>>() {
                                        @Override
                                        public void onSuccess(@NonNull List<Face> faces) {

                                            Log.i(TAG, ":: Detection Success :: ");
                                            Log.i(TAG, "Size Faces :: " + faces.size());
                                            textView.setText(faces.size() + " Faces");

                                            imageProxy.close();
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Log.i(TAG, ":: Detection Failed :: " + "Image :: " +imageBitmap+ " :: Error :: " + e.getMessage());
                                            imageProxy.close();
                                        }
                                    });
                }else {

                    Log.i(TAG, ":: Media Image NULL :: ");

                }

            }
        });


        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview,imageAnalysis);
    }


    private void StopCamera(){

        cameraProvider.unbindAll();
        Log.i(TAG, ":: Stop Camera  :: ");
    }

}