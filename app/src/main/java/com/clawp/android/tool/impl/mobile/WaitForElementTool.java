package com.clawp.android.tool.impl.mobile;

import android.view.accessibility.AccessibilityNodeInfo;
import com.clawp.android.service.ClawAccessibilityService;
import com.clawp.android.tool.BaseTool;
import com.clawp.android.tool.ToolParameter;
import com.clawp.android.tool.ToolResult;
import com.clawp.android.utils.XLog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 等待指定文字的元素出现
 */
public class WaitForElementTool extends BaseTool {

    private static final String TAG = "WaitForElementTool";

    @Override
    public String getName() {
        return "wait_for_element";
    }

    @Override
    public String getDisplayName() {
        return "等待元素出现";
    }

    @Override
    public String getDescriptionEN() {
        return "Wait for an element with specified text to appear on screen. Returns success when found or timeout.";
    }

    @Override
    public String getDescriptionCN() {
        return "等待指定文字的元素出现在屏幕上。找到元素或超时后返回。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
                new ToolParameter("text", "string", "Text to wait for", true),
                new ToolParameter("timeout_ms", "integer", "Maximum time to wait in milliseconds. Default 5000.", false),
                new ToolParameter("check_interval_ms", "integer", "Interval between checks in milliseconds. Default 500.", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }

        String text = requireString(params, "text");
        long timeoutMs = optionalLong(params, "timeout_ms", 5000);
        long checkIntervalMs = optionalLong(params, "check_interval_ms", 500);

        XLog.i(TAG, "Waiting for element with text: " + text + ", timeout=" + timeoutMs + "ms");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;

        while (System.currentTimeMillis() < endTime) {
            List<AccessibilityNodeInfo> nodes = service.findNodesByText(text);
            if (!nodes.isEmpty()) {
                XLog.i(TAG, "Element found with text: " + text);
                return ToolResult.success("Element found with text: " + text);
            }

            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ToolResult.error("Wait interrupted");
            }
        }

        XLog.w(TAG, "Timeout waiting for element with text: " + text);
        return ToolResult.error("Timeout waiting for element with text: " + text);
    }
}
