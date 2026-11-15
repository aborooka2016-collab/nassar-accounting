
package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private AccountingDb helper;
    private LinearLayout page;

    private int themeColor = 0xFF592B93;
    private static final int WHITE = Color.WHITE;
    private static final int BG = 0xFFF7F4FB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper = new AccountingDb(this);

        themeColor = getPreferences(MODE_PRIVATE)
                .getInt("theme", themeColor);

        home();
    }

    private SQLiteDatabase db() {
        return helper.getWritableDatabase();
    }

    private GradientDrawable background(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(20);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(14, 15, 14, 15);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private void screen(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setPadding(14, 14, 14, 25);

        scroll.addView(page);
        setContentView(scroll);

        TextView header = text(
                "نصار للحسابات\nNASSAR Accounting",
                24,
                WHITE
        );

        header.setTypeface(null, Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        header.setBackground(background(themeColor));
        header.setPadding(15, 30, 15, 30);

        page.addView(header);

        if (!title.equals("الرئيسية")) {
            TextView heading = text(title, 22, themeColor);
            heading.setTypeface(null, Typeface.BOLD);
            page.addView(heading);

            addButton("الرجوع للرئيسية", () -> home());
        }
    }

    private void heading(String title) {
        TextView t = text(title, 19, themeColor);
        t.setTypeface(null, Typeface.BOLD);
        page.addView(t);
    }

    private void addButton(String title, Runnable action) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTextColor(themeColor);
        button.setBackground(background(WHITE));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, 65);

        p.setMargins(3, 6, 3, 6);
        page.addView(button, p);

        button.setOnClickListener(v -> action.run());
    }

    private void addTile(
            LinearLayout row,
            String title,
            Runnable action
    ) {
        TextView tile = text(title, 16, themeColor);
        tile.setTypeface(null, Typeface.BOLD);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(background(WHITE));
        tile.setElevation(3);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(0, 90, 1);

        p.setMargins(5, 5, 5, 5);
        row.addView(tile, p);

        tile.setOnClickListener(v -> action.run());
    }

    private void row(
            String first,
            Runnable firstAction,
            String second,
            Runnable secondAction
    ) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(r);

        addTile(r, first, firstAction);
        addTile(r, second, secondAction);
    }

    private void card(String title, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 8, 10, 8);
        box.setBackground(background(WHITE));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(4, 6, 4, 6);
        page.addView(box, p);

        box.addView(text(title, 15, Color.DKGRAY));

        TextView number = text(value, 22, themeColor);
        number.setTypeface(null, Typeface.BOLD);
        box.addView(number);
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private double query(String sql) {
        try (Cursor c = db().rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                return c.getDouble(0);
            }
        }
        return 0;
    }

    private void notice(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void home() {
        screen("الرئيسية");

        TextView welcome = text(
                "مرحباً بك في نظام إدارة الحسابات والمخازن",
                16,
                themeColor
        );

        welcome.setGravity(Gravity.CENTER);
        page.addView(welcome);

        heading("العمليات الرئيسية");

        row(
                "🛒 فاتورة بيع",
                () -> invoice(true),
                "🛍 فاتورة شراء",
                () -> invoice(false)
        );

        row(
                "↩ مرتجع بيع",
                () -> unavailable("مرتجع بيع"),
                "↪ مرتجع شراء",
                () -> unavailable("مرتجع شراء")
        );

        row(
                "💵 سند قبض",
                () -> unavailable("سند قبض"),
                "💸 سند صرف",
                () -> unavailable("سند صرف")
        );

        heading("إدارة الحسابات");

        row(
                "👥 العملاء",
                () -> customers(),
                "🚚 الموردون",
                () -> customers()
        );

        row(
                "📦 الأصناف",
                () -> products(),
                "🏬 المخازن",
                () -> products()
        );

        row(
                "💰 الخزينة",
                () -> cash(),
                "📊 التقارير",
                () -> reports()
        );

        row(
                "💳 المصروفات",
                () -> expenses(),
                "⚙ الإعدادات",
                () -> settings()
        );

        heading("ملخص الحسابات");

        card(
                "رصيد الخزينة",
                money(cashBalance()) + " جنيه"
        );

        card(
                "عدد الأصناف",
                String.valueOf((long) query(
                        "SELECT COUNT(*) FROM products"
                ))
        );

        card(
                "عدد الفواتير",
                String.valueOf((long) query(
                        "SELECT COUNT(*) FROM invoices"
                ))
        );

        heading("أدوات إضافية");

        row(
                "▥ مسح باركود",
                () -> unavailable("مسح الباركود"),
                "⌕ استرجاع عملية",
                () -> unavailable("استرجاع عملية")
        );

        row(
                "🖨 طباعة",
                () -> unavailable("الطباعة"),
                "⚙ الإعدادات",
                () -> settings()
        );
    }

    private void unavailable(String feature) {
        new AlertDialog.Builder(this)
                .setTitle(feature)
                .setMessage("هذه الوظيفة قيد التطوير.")
                .setPositiveButton("حسناً", null)
                .show();
    }

    private void invoice(boolean sale) {
        Intent intent = new Intent(
                this,
                InvoicesActivity.class
        );

        intent.putExtra(
                "type",
                sale ? "sale" : "purchase"
        );

        intent.putExtra(
                "invoice_type",
                sale ? "sale" : "purchase"
        );

        intent.putExtra("isSale", sale);
        startActivity(intent);
    }

    private void customers() {
        startActivity(new Intent(
                this,
                CustomersActivity.class
        ));
    }

    private void products() {
        screen("الأصناف والمخازن");

        addButton("＋ إضافة صنف جديد", () ->
                productDialog(-1, "", "", "", "")
        );

        try (Cursor c = db().rawQuery(
                "SELECT id,name,quantity,buy_price,sell_price " +
                "FROM products ORDER BY name",
                null
        )) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String name = c.getString(1);
                String qty = money(c.getDouble(2));
                String buy = money(c.getDouble(3));
                String sell = money(c.getDouble(4));

                addButton(
                        name + " | الكمية: " + qty,
                        () -> productDialog(
                                id, name, qty, buy, sell
                        )
                );
            }
        }
    }

    private EditText input(
            LinearLayout parent,
            String title,
            String initial,
            boolean numeric
    ) {
        TextView label = text(title, 15, themeColor);
        label.setTypeface(null, Typeface.BOLD);
        parent.addView(label);

        EditText field = new EditText(this);
        field.setHint("أدخل " + title);
        field.setText(initial);
        field.setSingleLine(true);
        field.setSelectAllOnFocus(true);

        if (numeric) {
            field.setInputType(
                    InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
        }

        parent.addView(field);
        return field;
    }

    private double number(EditText field) {
        String s = field.getText().toString().trim();

        if (s.isEmpty()) return 0;

        double n = Double.parseDouble(s.replace(",", ""));

        if (!Double.isFinite(n) || n < 0) {
            throw new IllegalArgumentException(
                    "أدخل قيمة رقمية صحيحة"
            );
        }

        return n;
    }

    private void productDialog(
            long id,
            String oldName,
            String oldQty,
            String oldBuy,
            String oldSell
    ) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(20, 10, 20, 10);
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText name = input(
                form, "اسم الصنف", oldName, false
        );

        EditText qty = input(
                form, "الكمية", oldQty, true
        );

        EditText buy = input(
                form, "سعر الشراء", oldBuy, true
        );

        EditText sell = input(
                form, "سعر البيع", oldSell, true
        );

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(
                        id < 0 ? "إضافة صنف" : "تعديل صنف"
                )
                .setView(scroll)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();

        dialog.setOnShowListener(v -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(button -> {
                try {
                    String title =
                            name.getText().toString().trim();

                    if (title.isEmpty()) {
                        notice("اسم الصنف مطلوب");
                        return;
                    }

                    ContentValues cv = new ContentValues();
                    cv.put("name", title);
                    cv.put("quantity", number(qty));
                    cv.put("buy_price", number(buy));
                    cv.put("sell_price", number(sell));

                    if (id < 0) {
                        db().insertOrThrow(
                                "products", null, cv
                        );
                    } else {
                        db().update(
                                "products",
                                cv,
                                "id=?",
                                new String[]{
                                        String.valueOf(id)
                                }
                        );
                    }

                    dialog.dismiss();
                    products();

                } catch (Exception e) {
                    notice("خطأ: " + e.getMessage());
                }
            });
        });

        dialog.show();
    }

    private double cashBalance() {
        return query(
                "SELECT COALESCE(SUM(" +
                "CASE WHEN movement_type IN " +
                "('receipt','capital') " +
                "THEN amount ELSE -amount END" +
                "),0) FROM cash_movements"
        );
    }

    private void cash() {
        screen("الخزينة");

        card(
                "رصيد الخزينة",
                money(cashBalance()) + " جنيه"
        );

        addButton("＋ إضافة رأس مال", () -> addCapital());

        heading("آخر الحركات");

        try (Cursor c = db().rawQuery(
                "SELECT movement_type,amount " +
                "FROM cash_movements " +
                "ORDER BY id DESC LIMIT 30",
                null
        )) {
            while (c.moveToNext()) {
                card(
                        c.getString(0),
                        money(c.getDouble(1)) + " جنيه"
                );
            }
        }
    }

    private void addCapital() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(20, 10, 20, 10);

        EditText amount = input(
                form, "المبلغ", "", true
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("إضافة رأس مال")
                .setView(form)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();

        dialog.setOnShowListener(v ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(button -> {
                    try {
                        double n = number(amount);

                        if (n <= 0) {
                            notice("أدخل مبلغاً أكبر من صفر");
                            return;
                        }

                        ContentValues cv = new ContentValues();
                        cv.put("movement_type", "capital");
                        cv.put("amount", n);
                        cv.put(
                                "movement_date",
                                System.currentTimeMillis()
                        );

                        db().insertOrThrow(
                                "cash_movements", null, cv
                        );

                        dialog.dismiss();
                        cash();

                    } catch (Exception e) {
                        notice(e.getMessage());
                    }
                })
        );

        dialog.show();
    }

    private void expenses() {
        screen("المصروفات");

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(10, 10, 10, 10);
        page.addView(form);

        EditText category = input(
                form, "بند المصروف", "", false
        );

        EditText amount = input(
                form, "المبلغ", "", true
        );

        addButton("حفظ المصروف", () -> {
            try {
                String title =
                        category.getText().toString().trim();

                double n = number(amount);

                if (title.isEmpty() || n <= 0) {
                    notice("أدخل البند والمبلغ");
                    return;
                }

                SQLiteDatabase database = db();
                database.beginTransaction();

                try {
                    long now = System.currentTimeMillis();

                    ContentValues expense =
                            new ContentValues();

                    expense.put("category", title);
                    expense.put("amount", n);
                    expense.put("expense_date", now);

                    database.insertOrThrow(
                            "expenses", null, expense
                    );

                    ContentValues cash =
                            new ContentValues();

                    cash.put("movement_type", "expense");
                    cash.put("amount", n);
                    cash.put("movement_date", now);
                    cash.put("notes", title);

                    database.insertOrThrow(
                            "cash_movements", null, cash
                    );

                    database.setTransactionSuccessful();

                } finally {
                    database.endTransaction();
                }

                notice("تم حفظ المصروف");
                expenses();

            } catch (Exception e) {
                notice("خطأ: " + e.getMessage());
            }
        });

        heading("آخر المصروفات");

        try (Cursor c = db().rawQuery(
                "SELECT category,amount FROM expenses " +
                "ORDER BY id DESC LIMIT 30",
                null
        )) {
            while (c.moveToNext()) {
                card(
                        c.getString(0),
                        money(c.getDouble(1)) + " جنيه"
                );
            }
        }
    }

    private void reports() {
        screen("التقارير");

        card(
                "إجمالي المبيعات",
                money(query(
                        "SELECT COALESCE(" +
                        "SUM(total-discount),0) " +
                        "FROM invoices " +
                        "WHERE invoice_type='sale'"
                )) + " جنيه"
        );

        card(
                "إجمالي المشتريات",
                money(query(
                        "SELECT COALESCE(" +
                        "SUM(total-discount),0) " +
                        "FROM invoices " +
                        "WHERE invoice_type='purchase'"
                )) + " جنيه"
        );

        card(
                "إجمالي المصروفات",
                money(query(
                        "SELECT COALESCE(SUM(amount),0) " +
                        "FROM expenses"
                )) + " جنيه"
        );

        card(
                "رصيد الخزينة",
                money(cashBalance()) + " جنيه"
        );

        card(
                "عدد الأصناف",
                String.valueOf((long) query(
                        "SELECT COUNT(*) FROM products"
                ))
        );

        card(
                "عدد العملاء والموردين",
                String.valueOf((long) query(
                        "SELECT COUNT(*) FROM parties"
                ))
        );
    }

    private void settings() {
        screen("الإعدادات");

        heading("ألوان البرنامج");

        addButton("🟣 البنفسجي", () ->
                changeTheme(0xFF592B93)
        );

        addButton("🔵 الأزرق", () ->
                changeTheme(0xFF2465B0)
        );

        addButton("🟢 الأخضر", () ->
                changeTheme(0xFF23835A)
        );

        addButton("⚫ الرمادي الداكن", () ->
                changeTheme(0xFF454955)
        );

        heading("معلومات التطبيق");

        page.addView(text(
                "نصار للحسابات\n" +
                "Android Java\n" +
                "SQLite Offline",
                16,
                Color.DKGRAY
        ));
    }

    private void changeTheme(int color) {
        themeColor = color;

        getPreferences(MODE_PRIVATE)
                .edit()
                .putInt("theme", color)
                .apply();

        settings();
        notice("تم تغيير لون البرنامج");
    }

    @Override
    public void onBackPressed() {
        home();
    }

    @Override
    protected void onDestroy() {
        if (helper != null) {
            helper.close();
        }
        super.onDestroy();
    }
}
