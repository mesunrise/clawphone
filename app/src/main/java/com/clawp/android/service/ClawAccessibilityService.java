package com.clawp.android.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import com.clawp.android.utils.XLog;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core accessibility service that provides all device interaction capabilities.
 * Singleton-pattern: the running instance is accessible via {@link #getInstance()}.
 */
public class ClawAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClawA11yService";
    private static volatile ClawAccessibilityService instance;

    public static ClawAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        XLog.i(TAG, "Accessibility service connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Events can be processed here if needed in the future
    }

    @Override
    public void onInterrupt() {
        XLog.w(TAG, "Accessibility service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        XLog.i(TAG, "Accessibility service destroyed");
    }

    // ======================== Gesture Operations ========================

    /**
     * Performs a tap at the given screen coordinates.
     */
    public boolean performTap(int x, int y) {
        return performTap(x, y, 100);
    }

    public boolean performTap(int x, int y, long durationMs) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Performs a long press at the given screen coordinates.
     */
    public boolean performLongPress(int x, int y, long durationMs) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Performs a swipe gesture from (startX, startY) to (endX, endY).
     */
    public boolean performSwipe(int startX, int startY, int endX, int endY, long durationMs) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return dispatchGestureSync(gesture);
    }

    /**
     * Dispatches a gesture and waits for it to complete synchronously.
     */
    private boolean dispatchGestureSync(GestureDescription gesture) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);

        boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                result.set(true);
                latch.countDown();
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                result.set(false);
                latch.countDown();
            }
        }, null);

        if (!dispatched) {
            return false;
        }

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return result.get();
    }

    // ======================== Node Operations ========================

    /**
     * Finds all nodes matching the given text.
     */
    public List<AccessibilityNodeInfo> findNodesByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        return nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Finds all nodes matching the given view ID (e.g. "com.example:id/button").
     */
    public List<AccessibilityNodeInfo> findNodesById(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        return nodes != null ? nodes : new ArrayList<>();
    }

    /**
     * Performs a click action on the given node.
     */
    public boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    /**
     * Performs a long click action on the given node.
     */
    public boolean performLongClick(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
    }

    /**
     * Sets text on the given node (must be editable).
     */
    public boolean performSetText(AccessibilityNodeInfo node, String text) {
        if (node == null) {
            return false;
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    /**
     * Scrolls forward on the given node.
     */
    public boolean performScrollForward(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    /**
     * Scrolls backward on the given node.
     */
    public boolean performScrollBackward(AccessibilityNodeInfo node) {
        if (node == null) {
            return false;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    /**
     * Performs the global back action.
     */
    public boolean performGlobalBack() {
        return performGlobalAction(GLOBAL_ACTION_BACK);
    }

    /**
     * Performs the global home action.
     */
    public boolean performGlobalHome() {
        return performGlobalAction(GLOBAL_ACTION_HOME);
    }

    /**
     * Performs the global recents action.
     */
    public boolean performGlobalRecents() {
        return performGlobalAction(GLOBAL_ACTION_RECENTS);
    }

    // ======================== UI Tree Extraction ========================

    /**
     * Builds a simplified UI tree representation for LLM consumption.
     * Returns JSON string with node hierarchy, text, bounds, and actions.
     */
    public String getScreenInfo() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return "{\"error\": \"No root node available\"}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"nodes\": [");
        buildNodeTree(root, sb, 0, true);
        sb.append("]}");
        return sb.toString();
    }

    private void buildNodeTree(AccessibilityNodeInfo node, StringBuilder sb, int depth, boolean isFirst) {
        if (node == null) {
            return;
        }

        if (!isFirst) {
            sb.append(",");
        }

        sb.append("{");
        sb.append("\"depth\":").append(depth).append(",");
        sb.append("\"class\":\"").append(escapeJson(node.getClassName())).append("\",");
        sb.append("\"text\":\"").append(escapeJson(node.getText())).append("\",");
        sb.append("\"contentDesc\":\"").append(escapeJson(node.getContentDescription())).append("\",");
        sb.append("\"viewId\":\"").append(escapeJson(node.getViewIdResourceName())).append("\",");

        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        sb.append("\"bounds\":{\"left\":").append(bounds.left)
                .append(",\"top\":").append(bounds.top)
                .append(",\"right\":").append(bounds.right)
                .append(",\"bottom\":").append(bounds.bottom).append("},");

        sb.append("\"clickable\":").append(node.isClickable()).append(",");
        sb.append("\"longClickable\":").append(node.isLongClickable()).append(",");
        sb.append("\"scrollable\":").append(node.isScrollable()).append(",");
        sb.append("\"editable\":").append(node.isEditable()).append(",");
        sb.append("\"checkable\":").append(node.isCheckable()).append(",");
        sb.append("\"checked\":").append(node.isChecked()).append(",");
        sb.append("\"focusable\":").append(node.isFocusable()).append(",");
        sb.append("\"focused\":").append(node.isFocused()).append(",");
        sb.append("\"selected\":").append(node.isSelected()).append(",");
        sb.append("\"enabled\":").append(node.isEnabled());

        int childCount = node.getChildCount();
        if (childCount > 0) {
            sb.append(",\"children\":[");
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    buildNodeTree(child, sb, depth + 1, i == 0);
                    child.recycle();
                }
            }
            sb.append("]");
        }

        sb.append("}");
    }

    private String escapeJson(CharSequence cs) {
        if (cs == null) {
            return "";
        }
        String s = cs.toString();
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ======================== Screen Capture ========================

    /**
     * Takes a screenshot and returns it as a Bitmap.
     * Requires Android 9+ (API 28+).
     */
    public Bitmap takeScreenshot() {
        return takeScreenshot(3000);
    }

    public Bitmap takeScreenshot(long timeoutMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            XLog.e(TAG, "Screenshot API requires Android 11+");
            return null;
        }

        Display display = getSystemService(android.hardware.display.DisplayManager.class)
                .getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            XLog.e(TAG, "Cannot get default display");
            return null;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Bitmap> bitmapRef = new AtomicReference<>();

        takeScreenshot(display.getDisplayId(),
                getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult screenshotResult) {
                        bitmapRef.set(Bitmap.wrapHardwareBuffer(
                                screenshotResult.getHardwareBuffer(),
                                screenshotResult.getColorSpace()));
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        XLog.e(TAG, "Screenshot failed with error code: " + errorCode);
                        latch.countDown();
                    }
                });

        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return bitmapRef.get();
    }

    // ======================== Key Event Injection (TV Remote) ========================

    /**
     * Sends a key event via shell command. Works reliably on Android TV boxes.
     *
     * @param keyCode Android KeyEvent keycode (e.g. KeyEvent.KEYCODE_DPAD_UP = 19)
     * @return true if the command executed without error
     */
    public boolean sendKeyEvent(int keyCode) {
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"input", "keyevent", String.valueOf(keyCode)});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to send key event: " + keyCode, e);
            return false;
        }
    }

    // ======================== App Launch ========================

    /**
     * Opens an app by its package name.
     */
    public boolean openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent == null) {
                XLog.e(TAG, "Cannot resolve launch intent for " + packageName);
                return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to open app: " + packageName, e);
            return false;
        }
    }
}
