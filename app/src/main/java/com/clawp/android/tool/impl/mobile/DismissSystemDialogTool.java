package com.clawp.android.tool.impl.mobile;

import com.clawp.android.service.ClawAccessibilityService;
import com.clawp.android.tool.BaseTool;
import com.clawp.android.tool.ToolParameter;
import com.clawp.android.tool.ToolResult;
import com.clawp.android.utils.XLog;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 检测并关闭系统弹窗
 */
public class DismissSystemDialogTool extends BaseTool {

    private static final String TAG = "DismissSystemDialogTool";

    @Override
    public String getName() {
        return "dismiss_system_dialog";
    }

    @Override
    public String getDisplayName() {
        return "关闭系统弹窗";
    }

    @Override
    public String getDescriptionEN() {
        return "Detect and dismiss system dialogs (permission requests, update prompts, ads, etc.). Returns success if a dialog was dismissed.";
    }

    @Override
    public String getDescriptionCN() {
        return "检测并关闭系统弹窗（权限请求、更新提示、广告等）。如果关闭了弹窗则返回成功。";
    }

    @Override
    public List<ToolParameter> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        ClawAccessibilityService service = ClawAccessibilityService.getInstance();
        if (service == null) {
            return ToolResult.error("Accessibility service is not running");
        }

        if (!service.hasSystemDialog()) {
            XLog.i(TAG, "No system dialog detected");
            return ToolResult.success("No system dialog detected");
        }

        XLog.i(TAG, "System dialog detected, attempting to dismiss");
        boolean dismissed = service.dismissSystemDialog();

        if (dismissed) {
            XLog.i(TAG, "System dialog dismissed successfully");
            return ToolResult.success("System dialog dismissed successfully");
        } else {
            XLog.w(TAG, "Failed to dismiss system dialog");
            return ToolResult.error("Failed to dismiss system dialog");
        }
    }
}
