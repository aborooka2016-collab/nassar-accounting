// ألوان واجهة نصار للحسابات
private int themeColor = 0xFF592B93;
private final int goldColor = 0xFFDFAD3D;

private android.graphics.drawable.GradientDrawable
makeBackground(int color, int radius) {
    android.graphics.drawable.GradientDrawable shape =
        new android.graphics.drawable.GradientDrawable();
    shape.setColor(color);
    shape.setCornerRadius(radius);
    return shape;
}

private void showNassarDashboard() {
    android.widget.ScrollView scroll =
        new android.widget.ScrollView(this);

    android.widget.LinearLayout page =
        new android.widget.LinearLayout(this);
    page.setOrientation(1);
    page.setPadding(18, 18, 18, 18);
    page.setLayoutDirection(
        android.view.View.LAYOUT_DIRECTION_RTL
    );
    page.setBackgroundColor(0xFFF7F3FC);

    scroll.addView(page);

    android.widget.TextView header =
        new android.widget.TextView(this);
    header.setText("نصار للحسابات\nNASSAR Accounting");
    header.setTextSize(25);
    header.setTextColor(-1);
    header.setGravity(android.view.Gravity.CENTER);
    header.setPadding(15, 30, 15, 30);
    header.setBackground(makeBackground(themeColor, 25));
    page.addView(header);

    String[] sections = {
        "🛒 فاتورة بيع",
        "🛍 فاتورة شراء",
        "↩ مرتجع بيع",
        "🔄 مرتجع شراء",
        "💵 سند قبض",
        "💸 سند صرف",
        "👥 العملاء",
        "🚚 الموردون",
        "📦 الأصناف",
        "🏬 المخازن",
        "💰 الخزينة",
        "📊 التقارير"
    };

    for (int i = 0; i < sections.length; i += 2) {
        android.widget.LinearLayout row =
            new android.widget.LinearLayout(this);
        row.setOrientation(0);

        page.addView(row);

        for (int j = i; j < Math.min(i + 2, sections.length); j++) {
            final int index = j;

            android.widget.Button button =
                new android.widget.Button(this);
            button.setText(sections[j]);
            button.setTextSize(17);
            button.setAllCaps(false);
            button.setTextColor(themeColor);
            button.setBackground(
                makeBackground(0xFFFFFFFF, 18)
            );

            android.widget.LinearLayout.LayoutParams params =
                new android.widget.LinearLayout.LayoutParams(
                    0, 145, 1
                );
            params.setMargins(5, 7, 5, 7);
            row.addView(button, params);

            button.setOnClickListener(v -> {
                switch (index) {
                    case 0: invoice(true); break;
                    case 1: invoice(false); break;
                    case 2:
                    case 3: returns(); break;
                    case 4:
                    case 5: debts(); break;
                    case 6:
                    case 7: parties(); break;
                    case 8:
                    case 9: products(); break;
                    case 10: cash(); break;
                    case 11: reports(); break;
                }
            });
        }
    }

    android.widget.TextView statistics =
        new android.widget.TextView(this);
    statistics.setText("إحصائيات الحسابات");
    statistics.setTextSize(22);
    statistics.setTextColor(themeColor);
    statistics.setPadding(10, 20, 10, 10);
    page.addView(statistics);

    android.widget.TextView balance =
        new android.widget.TextView(this);
    balance.setText(
        "رصيد الخزينة: " + fmt(cashBalance()) + " ج"
    );
    balance.setTextSize(19);
    balance.setPadding(15, 20, 15, 20);
    balance.setBackground(
        makeBackground(0xFFFFFFFF, 15)
    );
    page.addView(balance);

    setContentView(scroll);
}
