package de.nulide.findmydevice.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import de.nulide.findmydevice.R;
import de.nulide.findmydevice.data.Settings;
import de.nulide.findmydevice.data.io.IO;
import de.nulide.findmydevice.data.io.JSONFactory;
import de.nulide.findmydevice.data.io.json.JSONMap;
import de.nulide.findmydevice.services.FMDServerService;
import de.nulide.findmydevice.utils.CypherUtils;

public class DummyCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static String CAMERA = "camera";

    private SurfaceView surfaceView;

    private ImageReader imgReader;
    private List<Surface> targets;

    private CameraManager camManager;
    private String camIdToUse;
    private CameraDevice camDevice;
    private CameraCaptureSession camSession;

    private Settings settings;
    private Context context;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        settings = JSONFactory.convertJSONSettings(IO.read(JSONMap.class, IO.settingsFileName));
        context = this;
        final int camera;
        Bundle bundle = getIntent().getExtras();
        if (!bundle.isEmpty()) {
            camera = bundle.getInt(CAMERA);
        } else {
            camera = 0;
        }

        //Find the right camera
        camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        camIdToUse = null;
        try {
            String[] cameraIdList = camManager.getCameraIdList();
            camIdToUse = cameraIdList[0];
            for (String camId : cameraIdList) {
                CameraCharacteristics characteristics = camManager.getCameraCharacteristics(camId);
                int cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT && camera == 1) {
                    camIdToUse = camId;
                    break;
                }
                if (cameraLensFacing == CameraMetadata.LENS_FACING_BACK && camera == 0) {
                    camIdToUse = camId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


        //Create a preview
        surfaceView = findViewById(R.id.preview);
        surfaceView.getHolder().addCallback(this);
        imgReader = ImageReader.newInstance(720, 1280, ImageFormat.JPEG, 1);
        Surface previewSurface = surfaceView.getHolder().getSurface();

        //Create an imagegrabber
        Surface imgSurface = imgReader.getSurface();
        targets = Arrays.asList(previewSurface, imgSurface);


    }

    public void createCapture() {
        try {
            camDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    camSession = cameraCaptureSession;
                    CaptureRequest.Builder reqBuilder = null;
                    try {
                        reqBuilder = camSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    reqBuilder.addTarget(imgReader.getSurface());
                    CaptureRequest req = reqBuilder.build();
                    try {
                        camSession.capture(req, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Image img = imgReader.acquireLatestImage();
                                if(img != null) {
                                    byte[] imgData = jpegImageToJpegByteArray(img);
                                    String picture = CypherUtils.encodeBase64(imgData);
                                    FMDServerService.sendPicture(context, picture, (String) settings.get(Settings.SET_FMDSERVER_URL), (String) settings.get(Settings.SET_FMDSERVER_ID));
                                }
                                imgReader.close();
                                camDevice.close();
                                finish();
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    finish();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
//Open Camera
        try {
            camManager.openCamera(camIdToUse, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    camDevice = cameraDevice;
                    createCapture();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    finish();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    finish();
                }
            }, new CamHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }


    public class CamHandler extends Handler {

    }

    private static byte[] jpegImageToJpegByteArray(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] data = new byte[buffer.capacity()];
        buffer.get(data);
        return data;
    }
}