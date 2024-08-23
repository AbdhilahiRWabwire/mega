package mega.privacy.android.app.utils;

import android.content.Context;

import androidx.annotation.Keep;

import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener;
import nz.mega.sdk.MegaChatApiAndroid;
import timber.log.Timber;

/**
 * The class can call from JNI to manage the video capture devices.
 * Don't move package name and change class name
 */
@Keep
public class VideoCaptureUtils {

    static private VideoCapturer videoCapturer = null;

    /**
     * Indicates if show video is allowed. The default value is TRUE, but this value will change to
     * FALSE meanwhile swapping between front and back cameras. The value will be TRUE again once
     * the camera swapping is completed.
     */
    private static boolean isVideoAllowed = true;

    /**
     * Check if show video is allowed.
     *
     * @return TRUE if show video is allowed or FALSE in other case.
     * @see VideoCaptureUtils#isVideoAllowed
     */
    public static boolean isVideoAllowed() {
        return isVideoAllowed;
    }

    /**
     * Set if show video is allowed.
     *
     * @param isVideoAllowed Value to indicate if show video is allowed.
     * @see VideoCaptureUtils#isVideoAllowed
     */
    public static void setIsVideoAllowed(boolean isVideoAllowed) {
        VideoCaptureUtils.isVideoAllowed = isVideoAllowed;
    }

    static private VideoCapturer createCameraCapturer(CameraEnumerator enumerator, String deviceName) {
        Timber.d("createCameraCapturer: %s", deviceName);
        return enumerator.createCapturer(deviceName, null);
    }

    /**
     * Get the video capture devices list.
     *
     * @return The video capture devices list.
     */
    static private String[] deviceList() {
        Timber.d("DeviceList");
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.getDeviceNames();
    }

    /**
     * Swap the current camera device to the opposite camera device.
     *
     * @param listener Camera swap listener.
     */
    public static void swapCamera(ChatChangeVideoStreamListener listener) {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        String currentCamera = megaChatApi.getVideoDeviceSelected();
        String newCamera;
        if (isFrontCamera(currentCamera)) {
            newCamera = getBackCamera();
        } else {
            newCamera = getFrontCamera();
        }
        if (newCamera != null) {
            isVideoAllowed = false;
            megaChatApi.setChatVideoInDevice(newCamera, listener);
        }
    }

    /**
     * Get the front camera device.
     *
     * @return Front camera device.
     */
    static public String getFrontCamera() {
        return getCameraDevice(true);
    }

    /**
     * Get the back camera device.
     *
     * @return Back camera device.
     */
    static public String getBackCamera() {
        return getCameraDevice(false);
    }

    /**
     * Get a camera device (front or back).
     *
     * @param front Value to indicate the camera device to get (true: front / false: back).
     * @return The camera device (front or back) requested. NULL if the requested device does not exist.
     */
    static private String getCameraDevice(boolean front) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        String[] deviceList = deviceList();
        for (String device : deviceList) {
            if ((front && enumerator.isFrontFacing(device)) || (!front && enumerator.isBackFacing(device))) {
                return device;
            }
        }
        return null;
    }

    /**
     * Check if the camera device is the front camera.
     *
     * @param device Camera device to check.
     * @return True if device is front camera or false in other case.
     */
    static public boolean isFrontCamera(String device) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.isFrontFacing(device);
    }

    /**
     * Check if the camera device is the back camera.
     *
     * @param device Camera device to check.
     * @return True if device is back camera or false in other case.
     */
    static public boolean isBackCamera(String device) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.isBackFacing(device);
    }

    /**
     * Check if the front camera is the current video device in use.
     *
     * @return True if the front camera is in use or false in other case.
     */
    static public boolean isFrontCameraInUse() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        String deviceName = megaChatApi.getVideoDeviceSelected();

        if (deviceName == null) return false;

        return isFrontCamera(deviceName);
    }

    /**
     * Check if the back camera is the current video device in use.
     *
     * @return True if the back camera is in use or false in other case.
     */
    static public boolean isBackCameraInUse() {
        MegaChatApiAndroid megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        return isBackCamera(megaChatApi.getVideoDeviceSelected());
    }

    static public void stopVideoCapture() {
        Timber.d("stopVideoCapture");

        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer = null;
        }
    }

    static public void startVideoCapture(int videoWidth, int videoHeight, int videoFps, SurfaceTextureHelper surfaceTextureHelper, CapturerObserver nativeAndroidVideoTrackSource, String deviceName) {
        Timber.d("startVideoCapture: %s", deviceName);

        stopVideoCapture();
        Context context = MegaApplication.getInstance().getApplicationContext();

        videoCapturer = createCameraCapturer(new Camera1Enumerator(true), deviceName);

        if (videoCapturer == null) {
            Timber.e("Unable to create video capturer");
            return;
        }

        videoCapturer.initialize(surfaceTextureHelper, context, nativeAndroidVideoTrackSource);

        // Start the capture!
        videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
        Timber.d("Start Capture");
    }
}
