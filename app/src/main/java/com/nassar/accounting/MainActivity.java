
package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.content.ContentValues;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private final int purple = Color.rgb(92, 45, 145);
    private LinearLayout page;
    private StockDb db;
    private boolean stockOpen = false;

    static class StockDb extends SQLiteOpenHelper {

        StockDb(Context context) {
            super(context, "nassar_stock.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "qty REAL NOT NULL DEFAULT 0, " +
                "buy REAL NOT NULL DEFAULT 0, " +
                "sell REAL NOT NULL DEFAULT 0)"
            );
        }

        @Override
        public void onUpgrade(
            SQLiteDatabase database,
            int oldVersion,
            int newVersion
        ) {
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new StockDb(this);
        showHome();
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
        b.setTextSize(17);
        b.setAllCaps(false);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 3, 0, 7);
        parent.addView(b, p);

        return b;
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

                if (section.equals("الأصناف والمخازن")) {
                    showStock();

                } else if (section.equals("العملاء والموردون")) {
                    startActivity(
                        new Intent(
                            MainActivity.this,
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

        SQLiteDatabase database = db.getReadableDatabase();

        Cursor c = database.rawQuery(
            "SELECT id,name,qty,buy,sell " +
            "FROM products ORDER BY id DESC",
            null
        );

        if (c.getCount() == 0) {

            TextView empty = new TextView(this);
            empty.setText(
                "مفيش أصناف مسجلة. اضغط إضافة صنف."
            );
            empty.setTextSize(17);
            empty.setPadding(10, 25, 10, 25);

            list.addView(empty);
        }

        while (c.moveToNext()) {

            int id = c.getInt(0);
            String name = c.getString(1);
            String qty = String.valueOf(c.getDouble(2));
            String buy = String.valueOf(c.getDouble(3));
            String sell = String.valueOf(c.getDouble(4));

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

                                new AlertDialog.Builder(this)
                                    .setTitle("تأكيد الحذف")
                                    .setMessage(
                                        "حذف الصنف: " + name + "؟"
                                    )
                                    .setNegativeButton(
                                        "إلغاء", null
                                    )
                                    .setPositiveButton(
                                        "حذف",
                                        (d, w) -> {

                                            db.getWritableDatabase()
                                                .delete(
                                                    "products",
                                                    "id=?",
                                                    new String[]{
                                                        String.valueOf(id)
                                                    }
                                                );

                                            showStock();
                                        }
                                    )
                                    .show();
                            }
                        }
                    )
                    .show()
            );
        }

        c.close();

        scroll.addView(list);
        page.addView(scroll);
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
                InputType.TYPE_NUMBER_FLAG_DECIMAL |
                InputType.TYPE_NUMBER_FLAG_SIGNED
            );
        }

        form.addView(
            input,
            new LinearLayout.LayoutParams(-1, -2)
        );

        return input;
    }

    private void editProduct(
        int id,
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

                String n =
                    name.getText().toString().trim();

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
                    values.put("qty", q);
                    values.put("buy", b);
                    values.put("sell", s);

                    if (id < 0) {

                        db.getWritableDatabase()
                            .insertOrThrow(
                                "products", null, values
                            );

                    } else {

                        db.getWritableDatabase()
                            .update(
                                "products",
                                values,
                                "id=?",
                                new String[]{
                                    String.valueOf(id)
                                }
                            );
                    }

                    dialog.dismiss();
                    showStock();

                } catch (NumberFormatException e) {

                    Toast.makeText(
                        this,
                        "راجع الكمية والأسعار",
                        Toast.LENGTH_SHORT
                    ).show();

                } catch (Exception e) {

                    Toast.makeText(
                        this,
                        "تعذر حفظ الصنف",
                        Toast.LENGTH_LONG
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
}
