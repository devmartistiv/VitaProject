package com.martist.vitamove.analytics;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.martist.vitamove.R;

import io.noties.markwon.Markwon;

public class ReportDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "report_title";
    public static final String EXTRA_CONTENT = "report_content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_VitaMove);
        setContentView(R.layout.activity_report_detail);

        Toolbar toolbar = findViewById(R.id.reportToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription(R.string.back_button_description);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView titleView = findViewById(R.id.reportTitle);
        TextView contentView = findViewById(R.id.reportContent);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String content = getIntent().getStringExtra(EXTRA_CONTENT);

        titleView.setText(title != null ? title : getString(R.string.app_name));

        if (content != null) {
            Markwon markwon = Markwon.builder(this).build();
            markwon.setMarkdown(contentView, content);
        }
    }
}
