
package com.nassar.accounting;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 32, 24, 24);
        page.setBackgroundColor(Color.rgb(248, 247, 252));
        page.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("نصار للحسابات");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(92, 45, 145));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 16, 0, 12);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("نظام متكامل لإدارة الحسابات والمخازن");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        LinearLayout sections = new LinearLayout(this);
        sections.setOrientation(LinearLayout.VERTICAL);
        sections.setPadding(0, 24, 0, 0);

        String[] items = {
            "المبيعات", "المشتريات", "المرتجعات",
            "الأصناف والمخازن", "العملاء والموردون",
            "الديون والتحصيلات", "الخزينة والبنوك",
            "التسعير", "المصروفات", "التقارير",
            "المستخدمون والصلاحيات", "الإعدادات"
        };

        for (String item : items) {
            TextView card = new TextView(this);
            card.setText(item);
            card.setTextSize(19);
            card.setTextColor(Color.rgb(70, 35, 110));
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(24, 22, 24, 22);

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
            params.setMargins(0, 0, 0, 12);
            sections.addView(card, params);
        }

        scroll.addView(sections);
        page.addView(scroll);
        setContentView(page);
    }
}
