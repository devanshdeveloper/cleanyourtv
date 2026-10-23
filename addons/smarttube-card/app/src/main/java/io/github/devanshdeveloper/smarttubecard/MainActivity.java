package io.github.devanshdeveloper.smarttubecard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName target = SmartTubeFinder.findSmartTube(this);
        if (target != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .setComponent(target)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return;
            } catch (Exception e) {
                Toast.makeText(this, "Failed to launch SmartTube: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Target not found: display informative D-pad friendly screen
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF121212);
        int pad = (int) (32 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(R.string.not_installed_title);
        tvTitle.setTextSize(22);
        tvTitle.setTextColor(0xFFE53935);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, (int) (16 * getResources().getDisplayMetrics().density));
        root.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(R.string.not_installed_body);
        tvBody.setTextSize(15);
        tvBody.setTextColor(0xFFDDDDDD);
        tvBody.setGravity(Gravity.CENTER);
        tvBody.setLineSpacing(4f, 1.2f);
        tvBody.setPadding(0, 0, 0, (int) (32 * getResources().getDisplayMetrics().density));
        root.addView(tvBody);

        Button btnClose = new Button(this);
        btnClose.setText(R.string.btn_close);
        btnClose.setTextSize(14);
        btnClose.setTextColor(Color.WHITE);
        btnClose.setBackgroundResource(R.drawable.btn_pill);
        btnClose.setFocusable(true);
        btnClose.requestFocus();
        btnClose.setOnClickListener(v -> finish());

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                (int) (180 * getResources().getDisplayMetrics().density),
                (int) (48 * getResources().getDisplayMetrics().density)
        );
        root.addView(btnClose, btnLp);

        setContentView(root);
    }
}
