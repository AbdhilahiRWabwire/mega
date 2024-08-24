/*
 *  Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package mega.privacy.android.app.meeting;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only

import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.VideoCaptureUtils.isFrontCameraInUse;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.DisplayMetrics;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MegaSurfaceRenderer implements TextureView.SurfaceTextureListener {

    private Paint paint;
    private final PorterDuffXfermode modesrcover;
    private final PorterDuffXfermode modesrcin;
    private int surfaceWidth;
    private int surfaceHeight;
    private static final int CORNER_RADIUS = 8;
    private static final int VISIBLE = 255;

    // the bitmap used for drawing.
    private Bitmap bitmap;
    // Rect of the source bitmap to draw
    private final Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private final Rect dstRect = new Rect();
    private RectF dstRectf = new RectF();
    private boolean isSmallCamera;
    private DisplayMetrics outMetrics;
    private long peerId = MEGACHAT_INVALID_HANDLE;
    private long clientId = MEGACHAT_INVALID_HANDLE;
    private boolean isScreenShared = false;
    private final TextureView myTexture;
    protected List<MegaSurfaceRendererListener> listeners;

    private int alpha = VISIBLE;

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public MegaSurfaceRenderer(TextureView view, boolean isSmallCamera, DisplayMetrics outMetrics) {
        this.myTexture = view;
        myTexture.setSurfaceTextureListener(this);
        bitmap = myTexture.getBitmap();
        surfaceHeight = myTexture.getHeight();
        surfaceWidth = myTexture.getWidth();
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.isSmallCamera = isSmallCamera;
        this.outMetrics = outMetrics;
        listeners = new ArrayList<>();
    }

    public MegaSurfaceRenderer(TextureView view, long peerId, long clientId, boolean isScreenShared) {
        this.myTexture = view;
        myTexture.setSurfaceTextureListener(this);
        bitmap = myTexture.getBitmap();
        surfaceHeight = myTexture.getHeight();
        surfaceWidth = myTexture.getWidth();
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.peerId = peerId;
        this.clientId = clientId;
        this.isScreenShared = isScreenShared;
        listeners = new ArrayList<>();
    }

    // surfaceChanged and surfaceCreated share this function
    private void changeDestRect(int dstWidth, int dstHeight) {
        surfaceWidth = dstWidth;
        surfaceHeight = dstHeight;
        dstRect.top = 0;
        dstRect.left = 0;
        dstRect.right = dstWidth;
        dstRect.bottom = dstHeight;
        dstRectf = new RectF(dstRect);

        adjustAspectRatio();
    }

    private void adjustAspectRatio() {
        if (bitmap != null) {
            dstRect.top = 0;
            dstRect.left = 0;
            dstRect.right = surfaceWidth;
            dstRect.bottom = surfaceHeight;

            dstRectf = new RectF(dstRect);
            float srcaspectratio = (float) bitmap.getWidth() / bitmap.getHeight();
            float dstaspectratio = (float) dstRect.width() / dstRect.height();
            if (srcaspectratio != 0 && dstaspectratio != 0) {

                if (isSmallCamera) {
                    if (srcaspectratio > dstaspectratio) {
                        float newHeight = dstRect.width() / srcaspectratio;
                        float decrease = dstRect.height() - newHeight;
                        dstRect.top += decrease / 2;
                        dstRect.bottom -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    } else {
                        float newWidth = dstRect.height() * srcaspectratio;
                        float decrease = dstRect.width() - newWidth;
                        dstRect.left += decrease / 2;
                        dstRect.right -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    }
                } else {
                    if (srcaspectratio > dstaspectratio) {
                        float newWidth = dstRect.height() * srcaspectratio;
                        float decrease = dstRect.width() - newWidth;
                        dstRect.left += decrease / 2;
                        dstRect.right -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    } else {
                        float newHeight = dstRect.width() / srcaspectratio;
                        float decrease = dstRect.height() - newHeight;
                        dstRect.top += decrease / 2;
                        dstRect.bottom -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    }
                }
            }
        }
    }

    public Bitmap createBitmap(int width, int height) {
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        srcRect.left = 0;
        srcRect.top = 0;
        srcRect.bottom = height;
        srcRect.right = width;
        adjustAspectRatio();
        return bitmap;
    }

    /**
     * Draw video frames.
     *
     * @param isLocal Indicates if the frames are from the local camera.
     */
    public void drawBitmap(boolean isLocal) {
        if (bitmap == null || myTexture == null)
            return;

        Canvas canvas = myTexture.lockCanvas();

        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);
        boolean isFrontCameraInUse = true;
        try {
            isFrontCameraInUse = isFrontCameraInUse();
        } catch (Exception e) {
            Timber.e(e);
        }

        if (isLocal && isFrontCameraInUse) {
            canvas.scale(-1, 1);
            canvas.translate(-canvas.getWidth(), 0);
        }
        if (isSmallCamera) {
            paint.reset();
            paint.setAlpha(alpha);
            paint.setXfermode(modesrcover);
            canvas.drawRoundRect(dstRectf, dp2px(CORNER_RADIUS, outMetrics), dp2px(CORNER_RADIUS, outMetrics), paint);
            paint.setXfermode(modesrcin);
        } else {
            paint = null;
        }
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        myTexture.unlockCanvasAndPost(canvas);
    }

    private void notifyStateToAll() {
        if (listeners == null) return;
        for (MegaSurfaceRendererListener listener : listeners) {
            notifyState(listener);
        }
    }

    public void addListener(MegaSurfaceRendererListener l) {
        listeners.add(l);
    }

    private void notifyState(MegaSurfaceRendererListener listener) {
        if (listener == null)
            return;

        listener.resetSize(peerId, clientId, isScreenShared);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        Bitmap textureViewBitmap = myTexture.getBitmap();
        if (textureViewBitmap == null) return;

        Timber.d("TextureView Available");
        notifyStateToAll();
        changeDestRect(in_width, in_height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        changeDestRect(in_width, in_height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Timber.d("TextureView destroyed");
        bitmap = null;
        surfaceWidth = 0;
        surfaceHeight = 0;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    public interface MegaSurfaceRendererListener {
        void resetSize(long peerId, long clientId, boolean isScreenShared);
    }
}
