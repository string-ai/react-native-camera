package ai.string.ocrreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.util.Log;
import android.view.GestureDetector;
import android.view.ScaleGestureDetector;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

import ai.string.ocrreader.ui.camera.CameraSource;
import ai.string.ocrreader.ui.camera.CameraSourcePreview;
import ai.string.ocrreader.ui.camera.GraphicOverlay;

public class OcrReaderViewManager  extends ViewGroupManager<CameraSourcePreview> implements ProcessorObserver {
    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private static final String REACT_CLASS = "RCTOcrView";
    private static final String LOG_TAG = OcrReaderViewManager.class.getName();

    private Boolean mActive = false;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private CameraSource mCameraSource;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;
    private CameraSourcePreview mPreview;
    private ThemedReactContext mContext;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public CameraSourcePreview createViewInstance(ThemedReactContext context) {
        mContext = context;
        mPreview = new CameraSourcePreview(mContext);
        start(mPreview);
        return mPreview;
    }

    @ReactProp(name = "active", defaultBoolean = false)
    public void setActive(CameraSourcePreview view, boolean active) {
        mActive = active;
        if(mActive) {
            start(view);
        }
        else {
            stop(view);
        }
    }

    ThemedReactContext getContext() {
        return mContext;
    }

    private void start(CameraSourcePreview view){
        mGraphicOverlay = new GraphicOverlay<OcrGraphic>(view.getContext());

        boolean autoFocus = true;
        boolean useFlash = false;

        createCameraSource(view.getContext(), autoFocus, useFlash);

        startCameraSource(view.getContext());
    }

    private void stop(CameraSourcePreview view){


    }

    @Override
    public void notifyDetections(String text) {
        WritableMap event = Arguments.createMap();
        event.putString("text", text);
        ReactContext reactContext = getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                mPreview.getId(),
                "topChange",
                event);
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource(Context context) throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (code != ConnectionResult.SUCCESS) {
            //Dialog dlg =
            //       GoogleApiAvailability
            //                .getInstance().getErrorDialog(context, code, RC_HANDLE_GMS);
            //dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(Context context, boolean autoFocus, boolean useFlash) {

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay, this));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(LOG_TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(context,
                        "Ocr dependencies cannot be downloaded due to low device storage",
                        Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG, "Ocr dependencies cannot be downloaded due to low device storage");
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(context, textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        //.setRequestedPreviewSize(1280, 1024)
                        .setRequestedPreviewSize(412, 302)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

}
