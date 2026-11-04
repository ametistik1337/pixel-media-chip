package com.pixel.mediachip;

import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controller managing real-time audio FFT capture via android.media.audiofx.Visualizer
 * and smooth procedural fallback when DRM/silent streams are detected.
 */
public class AudioVisualizerController {
    private static final String TAG = "PixelMediaChip_VisCtrl";
    public static final int NUM_BINS = 24;

    public interface VisualizerListener {
        void onFftData(float[] magnitudes);
    }

    private static AudioVisualizerController sInstance;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<VisualizerListener> mListeners = new CopyOnWriteArrayList<>();

    private Visualizer mVisualizer;
    private boolean mIsPlaying = false;
    private boolean mVisualizerRunning = false;
    private float mSensitivity = 1.0f; // 0.5f to 2.0f

    // Smoothed values with attack/decay
    private final float[] mCurrentMagnitudes = new float[NUM_BINS];

    // Procedural fallback handling
    private boolean mUseProcedural = false;
    private int mZeroEnergyFrames = 0;
    private static final int ZERO_ENERGY_THRESHOLD_FRAMES = 15; // ~250ms of zeros triggers procedural
    private final Runnable mProceduralTicker = new Runnable() {
        @Override
        public void run() {
            if (!mIsPlaying || mListeners.isEmpty()) {
                return;
            }
            if (mUseProcedural || !mVisualizerRunning) {
                generateProceduralFrame();
                notifyListeners();
            }
            mMainHandler.postDelayed(this, 16); // ~60fps
        }
    };

    public static synchronized AudioVisualizerController get() {
        if (sInstance == null) {
            sInstance = new AudioVisualizerController();
        }
        return sInstance;
    }

    private AudioVisualizerController() {}

    public void registerListener(VisualizerListener listener) {
        if (listener == null) return;
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
        evaluateState();
    }

    public void unregisterListener(VisualizerListener listener) {
        if (listener == null) return;
        mListeners.remove(listener);
        evaluateState();
    }

    public void setPlaying(boolean playing) {
        if (mIsPlaying != playing) {
            mIsPlaying = playing;
            evaluateState();
        }
    }

    public void setSensitivity(float sensitivity) {
        mSensitivity = Math.max(0.4f, Math.min(2.5f, sensitivity));
    }

    private void evaluateState() {
        boolean shouldRun = mIsPlaying && !mListeners.isEmpty();
        if (shouldRun) {
            startVisualizer();
            mMainHandler.removeCallbacks(mProceduralTicker);
            mMainHandler.post(mProceduralTicker);
        } else {
            stopVisualizer();
            mMainHandler.removeCallbacks(mProceduralTicker);
            // Smoothly collapse bins to zero
            for (int i = 0; i < NUM_BINS; i++) {
                mCurrentMagnitudes[i] = 0f;
            }
            notifyListeners();
        }
    }

    private synchronized void startVisualizer() {
        if (mVisualizerRunning) return;

        try {
            int[] range = Visualizer.getCaptureSizeRange();
            int captureSize = Math.max(128, range[0]);
            if (captureSize > 256) captureSize = 256;

            mVisualizer = new Visualizer(0);
            mVisualizer.setEnabled(false);
            mVisualizer.setCaptureSize(captureSize);
            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    processFft(fft);
                }
            }, Math.min(Visualizer.getMaxCaptureRate(), 60000), false, true);

            mVisualizer.setEnabled(true);
            mVisualizerRunning = true;
            mZeroEnergyFrames = 0;
            Log.d(TAG, "AudioVisualizer started successfully on audio session 0");
        } catch (Throwable t) {
            Log.w(TAG, "Unable to initialize system Visualizer (falling back to procedural): " + t.getMessage());
            mVisualizerRunning = false;
            mUseProcedural = true;
            if (mVisualizer != null) {
                try {
                    mVisualizer.release();
                } catch (Throwable ignored) {}
                mVisualizer = null;
            }
        }
    }

    private synchronized void stopVisualizer() {
        if (mVisualizer != null) {
            try {
                mVisualizer.setEnabled(false);
                mVisualizer.release();
            } catch (Throwable t) {
                Log.w(TAG, "Error releasing Visualizer: " + t.getMessage());
            }
            mVisualizer = null;
        }
        mVisualizerRunning = false;
        mUseProcedural = false;
        mZeroEnergyFrames = 0;
    }

    private void processFft(byte[] fft) {
        if (fft == null || fft.length < 4) return;

        int n = fft.length;
        int numPairs = (n / 2) - 1; // frequencies from 1 to N/2 - 1
        float[] rawMags = new float[numPairs];
        float maxRaw = 0f;

        for (int k = 1; k <= numPairs; k++) {
            byte r = fft[2 * k];
            byte im = fft[2 * k + 1];
            float mag = (float) Math.hypot(r, im);
            rawMags[k - 1] = mag;
            if (mag > maxRaw) {
                maxRaw = mag;
            }
        }

        // Check if stream is silent or DRM-zeroed
        if (maxRaw < 1.0f) {
            mZeroEnergyFrames++;
            if (mZeroEnergyFrames > ZERO_ENERGY_THRESHOLD_FRAMES) {
                mUseProcedural = true;
            }
            return;
        } else {
            mZeroEnergyFrames = 0;
            mUseProcedural = false;
        }

        // Map raw frequencies to NUM_BINS using logarithmic scaling
        float[] targetBins = new float[NUM_BINS];
        for (int i = 0; i < NUM_BINS; i++) {
            // Logarithmic mapping: lower bins cover fewer raw bins, higher bins cover more
            double startRatio = Math.pow((double) i / NUM_BINS, 2.0);
            double endRatio = Math.pow((double) (i + 1) / NUM_BINS, 2.0);

            int startIdx = (int) Math.floor(startRatio * numPairs);
            int endIdx = (int) Math.ceil(endRatio * numPairs);
            if (endIdx <= startIdx) endIdx = startIdx + 1;
            if (endIdx > numPairs) endIdx = numPairs;

            float sum = 0f;
            int count = 0;
            for (int j = startIdx; j < endIdx; j++) {
                sum += rawMags[j];
                count++;
            }
            float avg = count > 0 ? (sum / count) : 0f;

            // Normalize with boost for mid and high frequencies which naturally have lower energy
            float freqWeight = 1.0f + (float) Math.pow((double) i / NUM_BINS, 1.3) * 2.2f;
            float normalized = (avg / 64.0f) * freqWeight * mSensitivity;
            if (normalized > 1.0f) normalized = 1.0f;
            if (normalized < 0.0f) normalized = 0.0f;

            targetBins[i] = normalized;
        }

        // Attack & decay smoothing
        mMainHandler.post(() -> {
            for (int i = 0; i < NUM_BINS; i++) {
                float target = targetBins[i];
                float current = mCurrentMagnitudes[i];
                if (target > current) {
                    // Snappy attack
                    mCurrentMagnitudes[i] = current + (target - current) * 0.70f;
                } else {
                    // Smooth organic decay
                    mCurrentMagnitudes[i] = current * 0.78f;
                }
            }
            notifyListeners();
        });
    }

    private void generateProceduralFrame() {
        long now = SystemClock.uptimeMillis();
        // 124 BPM rhythm simulation (period ~484ms)
        double beatPhase = (now % 484) / 484.0;
        double slowPhase = (now % 3870) / 3870.0 * 2.0 * Math.PI;

        // Bass kick pulse
        float kick = (float) Math.max(0.0, 1.0 - beatPhase * 3.2);

        // Hi-hat and snare pulsation
        float snare = (float) Math.max(0.0, 1.0 - Math.abs(beatPhase - 0.5) * 4.0);

        for (int i = 0; i < NUM_BINS; i++) {
            float binPos = (float) i / (NUM_BINS - 1);
            float target = 0.10f; // ambient idle floor

            if (binPos < 0.28f) {
                // Bass frequencies
                float bassWeight = 1.0f - (binPos / 0.28f);
                target += kick * 0.85f * bassWeight;
            } else if (binPos < 0.65f) {
                // Mid frequencies
                float midWave = (float) Math.sin(slowPhase + binPos * 5.0) * 0.5f + 0.5f;
                target += midWave * 0.45f + snare * 0.35f;
            } else {
                // High frequencies
                float highJitter = (float) ((Math.sin(now * 0.015 + i * 2.1) + 1.0) * 0.5);
                target += highJitter * 0.38f + snare * 0.50f;
            }

            target *= mSensitivity;
            if (target > 1.0f) target = 1.0f;
            if (target < 0.0f) target = 0.0f;

            float current = mCurrentMagnitudes[i];
            if (target > current) {
                mCurrentMagnitudes[i] = current + (target - current) * 0.60f;
            } else {
                mCurrentMagnitudes[i] = current * 0.82f;
            }
        }
    }

    private void notifyListeners() {
        for (VisualizerListener listener : mListeners) {
            try {
                listener.onFftData(mCurrentMagnitudes);
            } catch (Throwable t) {
                Log.w(TAG, "Error in VisualizerListener: ", t);
            }
        }
    }
}
