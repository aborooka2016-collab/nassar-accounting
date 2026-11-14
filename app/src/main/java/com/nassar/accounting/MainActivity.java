package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int purple = Color.rgb(92, 45, 145);

    private AccountingDb db;
    private LinearLayout page;
    private boolean stockOpen = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        db = new AccountingDb(this);

        try {
            migrateOldStock();
        } catch (Exception e) {
            Toast.makeText(
                this,
                "تعذر نقل المخزون القديم: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show();
        }

        showHome();
    }

    private void migrateOldStock() {

        SQLiteDatabase target = db.getWritableDatabase();

        target.execSQL(
            "CREATE TABLE IF NOT EXISTS migration_status (" +
            "name TEXT PRIMARY KEY)"
        );

        try (Cursor check = target.rawQuery(
            "SELECT name FROM migration_status " +
            "WHERE name='old_stock_v1'",
            null
        )) {
            if (check.moveToFirst()) return;
        }

        File oldFile = getDatabasePath("nassar_stock.db");

        if (!oldFile.exists()) {
            target.execSQL(
                "INSERT OR IGNORE INTO migration_status(name) " +
                "VALUES('old_stock_v1')"
            );
            return;
        }

        SQLiteDatabase old = SQLiteDatabase.openDatabase(
            oldFile.getAbsolutePath(),
            null,
            SQLiteDatabase.OPEN_READONLY
        );

        try {
            target.beginTransaction();

            try (Cursor c = old.rawQuery(
                "SELECT id,name,qty,buy,sell FROM products",
                null
            )) {
                while (c.moveToNext()) {

                    ContentValues values = new ContentValues();

                    values.put("name", c.getString(1));
                    values.put("quantity", c.getDouble(2));
                    values.put("buy_price", c.getDouble(3));
                    values.put("sell_price", c.getDouble(4));

                    target.insertOrThrow(
                        "products",
                        null,
                        values
                    );
                }
            }

            target.execSQL(
                "INSERT INTO migration_status(name) " +
                "VALUES('old_stock_v1')"
            );

            target.setTransactionSuccessful();

        } finally {
            target.endTransaction();
            old.close();
        }
    }

    private void makePage(String heading) {

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        page.setPadding(18, 24, 18, 18);
        page.setBackgroundColor(Color.rgb(248, 247, 252));

        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextSize(26);
        title.setTextColor(purple);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 18);

        page.addView(title);
        setContentView(page);
    }

    private Button button(String label, LinearLayout parent) {

        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 3, 0, 7);
        parent.addView(b, p);

        return b;
    }

    private String money(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void showHome() {

        stockOpen = false;
        makePage("نصار للحسابات");

        TextView subtitle = new TextView(this);
        subtitle.setText("إدارة الحسابات والمخازن");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle);

        ScrollView scroll = new ScrollView(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 16, 0, 0);

        String[] sections = {
            "المبيعات",
            "المشتريات",
            "المرتجعات",
            "الأصناف والمخازن",
            "العملاء والموردون",
            "الديون والتحصيلات",
            "الخزينة والبنوك",
            "التسعير",
            "المصروفات",
            "التقارير",
            "المستخدمون والصلاحيات",
            "الإعدادات"
        };

        for (String section : sections) {

            Button b = button(section, list);

            b.setOnClickListener(v -> {

                if (
                    section.equals("المبيعات") ||
                    section.equals("المشتريات")
                ) {

                    startActivity(
                        new Intent(
                            this,
                            InvoicesActivity.class
                        )
                    );

                } else if (
                    section.equals("الأصناف والمخازن")
                ) {

                    showStock();

                } else if (
                    section.equals("العملاء والموردون")
                ) {

                    startActivity(
                        new Intent(
                            this,
                            CustomersActivity.class
                        )
                    );

                } else {

                    new AlertDialog.Builder(this)
                        .setTitle(section)
                        .setMessage("القسم ده لسه تحت التطوير.")
                        .setPositiveButton("موافق", null)
                        .show();
                }
            });
        }

        scroll.addView(list);
        page.addView(scroll);
    }

    private void showStock() {

        stockOpen = true;
        makePage("الأصناف والمخازن");

        button("+ إضافة صنف", page)
            .setOnClickListener(v ->
                editProduct(-1, "", "0", "0", "0")
            );

        button("رجوع للرئيسية", page)
            .setOnClickListener(v -> showHome());

        ScrollView scroll = new ScrollView(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        try (Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,name,quantity,buy_price,sell_price " +
            "FROM products ORDER BY id DESC",
            null
        )) {

            if (c.getCount() == 0) {

                TextView empty = new TextView(this);
                empty.setText("مفيش أصناف مسجلة.");
                empty.setTextSize(17);
                empty.setPadding(10, 25, 10, 25);

                list.addView(empty);
            }

            while (c.moveToNext()) {

                long id = c.getLong(0);
                String name = c.getString(1);
                String qty = money(c.getDouble(2));
                String buy = money(c.getDouble(3));
                String sell = money(c.getDouble(4));

                Button item = button(
                    name +
                    "\nالكمية: " + qty +
                    " | شراء: " + buy +
                    " | بيع: " + sell,
                    list
                );

                item.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                        .setTitle(name)
                        .setItems(
                            new String[]{
                                "تعديل الصنف",
                                "حذف الصنف"
                            },
                            (dialog, which) -> {

                                if (which == 0) {
                                    editProduct(
                                        id, name, qty, buy, sell
                                    );
                                } else {
                                    confirmDelete(id, name);
                                }
                            }
                        )
                        .show()
                );
            }
        }

        scroll.addView(list);

        page.addView(
            scroll,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );
    }

    private void confirmDelete(long id, String name) {

        new AlertDialog.Builder(this)
            .setTitle("حذف الصنف")
            .setMessage("هل تريد حذف الصنف: " + name + "؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف", (d, w) -> {

                try {

                    int count = db.getWritableDatabase().delete(
                        "products",
                        "id=?",
                        new String[]{String.valueOf(id)}
                    );

                    if (count > 0) {
                        showStock();
                    }

                } catch (SQLiteException e) {

                    Toast.makeText(
                        this,
                        "لا يمكن حذف صنف مرتبط بفواتير",
                        Toast.LENGTH_LONG
                    ).show();
                }
            })
            .show();
    }

    private EditText field(
        LinearLayout form,
        String label,
        String value,
        boolean number
    ) {

        EditText input = new EditText(this);

        input.setHint(label);
        input.setText(value);
        input.setSingleLine(true);

        if (number) {
            input.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
        }

        form.addView(
            input,
            new LinearLayout.LayoutParams(-1, -2)
        );

        return input;
    }

    private void editProduct(
        long id,
        String oldName,
        String oldQty,
        String oldBuy,
        String oldSell
    ) {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(30, 8, 30, 8);

        EditText name = field(
            form, "اسم الصنف", oldName, false
        );

        EditText qty = field(
            form, "الكمية", oldQty, true
        );

        EditText buy = field(
            form, "سعر الشراء", oldBuy, true
        );

        EditText sell = field(
            form, "سعر البيع", oldSell, true
        );

        AlertDialog dialog =
            new AlertDialog.Builder(this)
                .setTitle(
                    id < 0 ? "إضافة صنف" : "تعديل صنف"
                )
                .setView(form)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();

        dialog.setOnShowListener(d ->
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String n = name.getText().toString().trim();

                if (n.isEmpty()) {
                    name.setError("اسم الصنف مطلوب");
                    return;
                }

                try {

                    double q = Double.parseDouble(
                        qty.getText().toString().trim()
                    );

                    double b = Double.parseDouble(
                        buy.getText().toString().trim()
                    );

                    double s = Double.parseDouble(
                        sell.getText().toString().trim()
                    );

                    if (
                        !Double.isFinite(q) ||
                        !Double.isFinite(b) ||
                        !Double.isFinite(s) ||
                        q < 0 || b < 0 || s < 0
                    ) {
                        Toast.makeText(
                            this,
                            "أدخل أرقام صحيحة وغير سالبة",
                            Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    ContentValues values =
                        new ContentValues();

                    values.put("name", n);
                    values.put("quantity", q);
                    values.put("buy_price", b);
                    values.put("sell_price", s);

                    try {

                        if (id < 0) {

                            db.getWritableDatabase().insertOrThrow(
                                "products", null, values
                            );

                        } else {

                            db.getWritableDatabase().update(
                                "products",
                                values,
                                "id=?",
                                new String[]{String.valueOf(id)}
                            );
                        }

                        dialog.dismiss();
                        showStock();

                    } catch (Exception e) {

                        Toast.makeText(
                            this,
                            "تعذر حفظ الصنف",
                            Toast.LENGTH_LONG
                        ).show();
                    }

                } catch (NumberFormatException e) {

                    Toast.makeText(
                        this,
                        "راجع الكمية والأسعار",
                        Toast.LENGTH_SHORT
                    ).show();
                }
            })
        );

        dialog.show();
    }

    @Override
    public void onBackPressed() {

        if (stockOpen) {
            showHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
