package com.clawp.android.tool.impl.mobile;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import com.clawp.android.ClawApplication;
import com.clawp.android.R;
import com.clawp.android.service.ClawAccessibilityService;
import com.clawp.android.tool.BaseTool;
import com.clawp.android.tool.ToolParameter;
import com.clawp.android.tool.ToolResult;
import com.clawp.android.utils.XLog;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 通过文字查找并点击元素
 */
public class ClickElementByTextTool extends BaseTool {

    private static final String TAG = "ClickElementByTextTool";

    @Override
    public String getName() {
        return "click_element_by_text";
    }

    @Override
    public String getDisplayName() {
        return "点击文字元素";
    }

    @Override
    public String getDescriptionEN() {
        return "Find and click an element by its text content. Supports partial matching.";
    }

    @Override
    public String getDescriptionCN() {
        return "通过文字内容查找并点击元素。支持部分匹配。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Arrays.asList(
                new ToolParameter("text", "string", "Text to search for (supports partial match)", true),
                new ToolParameter("exact_match", "boolean", "Whether to require exact text match. Default false.", false),
                new ToolParameter("index", "integer", "If multiple elements found, click the one at this index (0-based). Default 0.", false)
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }

        String text = requireString(params, "text");
        boolean exactMatch = optionalBoolean(params, "exact_match", false);
        int index = optionalInt(params, "index", 0);

        XLog.i(TAG, "Searching for element with text: " + text + ", exactMatch=" + exactMatch + ", index=" + index);

        // Find nodes by text
        List<AccessibilityNodeInfo> nodes = service.findNodesByText(text);

        if (nodes.isEmpty()) {
            return ToolResult.error("No element found with text: " + text);
        }

        // Filter by exact match if needed
        if (exactMatch) {
            nodes.removeIf(node -> {
                CharSequence nodeText = node.getText();
                return nodeText == null || !nodeText.toString().equals(text);
            });

            if (nodes.isEmpty()) {
                return ToolResult.error("No element found with exact text: " + text);
            }
        }

        // Check index
        if (index < 0 || index >= nodes.size()) {
            return ToolResult.error("Index " + index + " out of range. Found " + nodes.size() + " elements.");
        }

        AccessibilityNodeInfo targetNode = nodes.get(index);

        // Try to click the node
        boolean success = service.performClick(targetNode);

        if (success) {
            Rect bounds = new Rect();
            targetNode.getBoundsInScreen(bounds);
            XLog.i(TAG, "Clicked element at (" + bounds.centerX() + ", " + bounds.centerY() + ")");
            return ToolResult.success("Clicked element with text: " + text + " at position (" + bounds.centerX() + ", " + bounds.centerY() + ")");
        } else {
            return ToolResult.error("Failed to click element with text: " + text);
        }
    }
}
