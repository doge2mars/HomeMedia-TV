package org.moontechlab.shutdown;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/**
 * 关机：点开即关机（无需确认，老人一键用）。
 * 靠 root 执行 reboot -p；没有 root 时显示失败原因，不闪退、不乱关。
 */
public class MainActivity extends Activity {
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tv = new TextView(this);
        tv.setText("正在关机…");
        tv.setTextSize(36);
        tv.setTextColor(Color.WHITE);
        tv.setBackgroundColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        setContentView(tv);
        new Thread(() -> {
            boolean ok = shutdown();
            if (!ok) {
                runOnUiThread(() -> tv.setText("关机失败：需要 root 权限"));
            }
        }).start();
    }

    private boolean shutdown() {
        // 179 是安卓 7.1（API 25），没有 waitFor(timeout, unit)（API 26 才有），
        // 用 sleep + exitValue() 判断，各 API 通用。
        String[] cmds = {
            "su -c reboot -p",
            "su 0 reboot -p",
            "su root reboot -p",
            "su -c svc power shutdown",
            "su 0 svc power shutdown",
            "reboot -p"
        };
        for (String c : cmds) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", c});
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                try {
                    int code = p.exitValue();
                    if (code == 0) return true;
                } catch (IllegalThreadStateException e) {
                    return true; // 还在跑 = 正在关机（或 su 在等授权）
                }
            } catch (Exception e) {
                // 换下一条命令
            }
        }
        return false;
    }
}
