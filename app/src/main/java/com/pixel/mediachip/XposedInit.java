package com.pixel.mediachip;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {
    private static final String TAG = "PixelMediaChip";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("PixelMediaChip: Hooking SystemUI package with vanilla chip & original player...");

        // 1. Hook PhoneStatusBarView to insert chip
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                    lpparam.classLoader,
                    "onFinishInflate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            View statusBarView = (View) param.thisObject;
                            Context context = statusBarView.getContext();

                            int containerId = context.getResources().getIdentifier(
                                    "status_bar_start_side_except_heads_up", "id", "com.android.systemui");
                            ViewGroup startSideExceptHeadsUp = statusBarView.findViewById(containerId);

                            if (startSideExceptHeadsUp == null) {
                                containerId = context.getResources().getIdentifier(
                                        "status_bar_start_side_content", "id", "com.android.systemui");
                                startSideExceptHeadsUp = statusBarView.findViewById(containerId);
                            }

                            if (startSideExceptHeadsUp != null) {
                                int clockId = context.getResources().getIdentifier("clock", "id", "com.android.systemui");
                                View clockView = statusBarView.findViewById(clockId);

                                int index = 0;
                                if (clockView != null) {
                                    index = startSideExceptHeadsUp.indexOfChild(clockView) + 1;
                                }

                                new VanillaMediaChipController(context, lpparam.classLoader, startSideExceptHeadsUp, index);
                                Log.i(TAG, "Successfully initialized VanillaMediaChipController into status bar!");
                                XposedBridge.log("PixelMediaChip: Successfully initialized VanillaMediaChipController!");
                            } else {
                                Log.e(TAG, "Could not find start side container in PhoneStatusBarView");
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            Log.e(TAG, "Error hooking PhoneStatusBarView: ", t);
            XposedBridge.log("PixelMediaChip error: " + t.getMessage());
        }

        // 2. Direct hook into RecordingController to instantly detect Screen Recording start/stop
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.screenrecord.RecordingController",
                    lpparam.classLoader,
                    "updateState",
                    boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean isRecording = (boolean) param.args[0];
                            VanillaMediaChipController.setScreenRecordingActive(isRecording);
                            XposedBridge.log("PixelMediaChip: Screen recording state changed: " + isRecording);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("PixelMediaChip: Could not hook RecordingController.updateState: " + t.getMessage());
        }

        // 3. Hook RecordingService for backup screen record detection
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.screenrecord.RecordingService",
                    lpparam.classLoader,
                    "onStartCommand",
                    android.content.Intent.class,
                    int.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            android.content.Intent intent = (android.content.Intent) param.args[0];
                            if (intent != null && intent.getAction() != null) {
                                String action = intent.getAction();
                                if (action.endsWith("START")) {
                                    VanillaMediaChipController.setScreenRecordingActive(true);
                                } else if (action.endsWith("STOP")) {
                                    VanillaMediaChipController.setScreenRecordingActive(false);
                                }
                            }
                        }
                    }
            );
        } catch (Throwable ignored) {}

        // 4. Hook NotificationIconContainer to prevent notification icons from overlapping when chip is visible
        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.phone.NotificationIconContainer",
                    lpparam.classLoader,
                    "setVisibility",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (VanillaMediaChipController.isAnyChipShowing()) {
                                param.args[0] = View.GONE;
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("PixelMediaChip: Could not hook NotificationIconContainer.setVisibility: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "com.android.systemui.statusbar.AlphaOptimizedFrameLayout",
                    lpparam.classLoader,
                    "setVisibility",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            View view = (View) param.thisObject;
                            if (view != null && view.getContext() != null) {
                                int notifAreaId = view.getContext().getResources().getIdentifier(
                                        "notification_icon_area", "id", "com.android.systemui"
                                );
                                if (notifAreaId != 0 && view.getId() == notifAreaId) {
                                    if (VanillaMediaChipController.isAnyChipShowing()) {
                                        param.args[0] = View.GONE;
                                    }
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("PixelMediaChip: Could not hook AlphaOptimizedFrameLayout.setVisibility: " + t.getMessage());
        }
    }
}
