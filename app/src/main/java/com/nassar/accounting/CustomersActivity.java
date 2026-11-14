
package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;

public class CustomersActivity extends Activity {

    private LinearLayout list;
    private EditText search;
    private Database db;
    private final int purple = Color.rgb(92, 45, 145);

    static class Database extends SQLiteOpenHelper {

        Database(Context context) {
            super(context, "nassar_contacts.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase d) {
            d.execSQL(
                "CREATE TABLE contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "phone TEXT," +
                "address TEXT," +
                "balance REAL DEFAULT 0)"
            );
        }

        @Override
        public void onUpgrade(
            SQLiteDatabase d, int oldVersion, int newVersion
        ) {
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        db = new Database(this);
        buildScreen();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(16, 20, 16, 16);
        root.setBackgroundColor(Color.rgb(248, 247, 252));

        TextView title = new TextView(this);
        title.setText("العملاء والموردون");
        title.setTextSize(26);
        title.setTextColor(purple);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        Button add = new Button(this);
        add.setText("+ إضافة عميل أو مورد");
        root.addView(add);

        add.setOnClickListener(v -> editContact(
            -1, "عميل", "", "", "", "0"
        ));

        search = new EditText(this);
        search.setHint("بحث بالاسم أو الهاتف");
        search.setSingleLine(true);
        root.addView(search);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        root.addView(
            scroll,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );

        Button back = new Button(this);
        back.setText("رجوع للرئيسية");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(root);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after
            ) {}

            public void onTextChanged(
                CharSequence s, int start, int before, int count
            ) {
                loadContacts(s.toString());
            }

            public void afterTextChanged(Editable e) {}
        });

        loadContacts("");
    }

    private void loadContacts(String query) {

        list.removeAllViews();

        String pattern = "%" + query + "%";

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id,type,name,phone,address,balance " +
            "FROM contacts " +
            "WHERE name LIKE ? OR phone LIKE ? " +
            "ORDER BY id DESC",
            new String[]{pattern, pattern}
        );

        if (c.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد بيانات");
            empty.setTextSize(17);
            empty.setPadding(10, 30, 10, 30);
            list.addView(empty);
        }

        while (c.moveToNext()) {

            int id = c.getInt(0);
            String type = c.getString(1);
            String name = c.getString(2);
            String phone = c.getString(3);
            String address = c.getString(4);
            String balance = String.valueOf(c.getDouble(5));

            Button item = new Button(this);
            item.setAllCaps(false);
            item.setText(
                type + " | " + name +
                "\nالهاتف: " + phone +
                "\nالرصيد الافتتاحي: " + balance
            );

            list.addView(item);

            item.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setItems(
                        new String[]{"تعديل", "حذف"},
                        (dialog, choice) -> {

                            if (choice == 0) {
                                editContact(
                                    id, type, name,
                                    phone, address, balance
                                );
                            } else {
                                confirmDelete(id, name);
                            }
                        }
                    ).show()
            );
        }

        c.close();
    }

    private EditText input(
        LinearLayout form,
        String hint,
        String value
    ) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        form.addView(e);
        return e;
    }

    private void editContact(
        int id,
        String oldType,
        String oldName,
        String oldPhone,
        String oldAddress,
        String oldBalance
    ) {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(28, 10, 28, 10);

        Spinner type = new Spinner(this);

        String[] types = {"عميل", "مورد"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        );

        type.setAdapter(adapter);
        type.setSelection(
            oldType.equals("مورد") ? 1 : 0
        );
        form.addView(type);

        EditText name = input(
            form, "الاسم", oldName
        );

        EditText phone = input(
            form, "رقم الهاتف", oldPhone
        );
        phone.setInputType(InputType.TYPE_CLASS_PHONE);

        EditText address = input(
            form, "العنوان", oldAddress
        );

        EditText balance = input(
            form, "الرصيد الافتتاحي", oldBalance
        );

        balance.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL |
            InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(
                id < 0 ? "إضافة عميل أو مورد" : "تعديل البيانات"
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
                    name.setError("الاسم مطلوب");
                    return;
                }

                double amount;

                try {
                    amount = Double.parseDouble(
                        balance.getText().toString().trim()
                    );

                    if (!Double.isFinite(amount)) {
                        throw new NumberFormatException();
                    }

                } catch (NumberFormatException ex) {
                    balance.setError("أدخل رصيدًا صحيحًا");
                    return;
                }

                ContentValues values = new ContentValues();

                values.put(
                    "type", type.getSelectedItem().toString()
                );
                values.put("name", n);
                values.put(
                    "phone", phone.getText().toString().trim()
                );
                values.put(
                    "address", address.getText().toString().trim()
                );
                values.put("balance", amount);

                try {
                    if (id < 0) {
                        db.getWritableDatabase().insertOrThrow(
                            "contacts", null, values
                        );
                    } else {
                        db.getWritableDatabase().update(
                            "contacts",
                            values,
                            "id=?",
                            new String[]{String.valueOf(id)}
                        );
                    }

                    dialog.dismiss();
                    loadContacts(search.getText().toString());

                } catch (Exception ex) {
                    Toast.makeText(
                        this,
                        "تعذر حفظ البيانات",
                        Toast.LENGTH_LONG
                    ).show();
                }
            })
        );

        dialog.show();
    }

    private void confirmDelete(int id, String name) {

        new AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل تريد حذف " + name + "؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف", (d, w) -> {

                db.getWritableDatabase().delete(
                    "contacts",
                    "id=?",
                    new String[]{String.valueOf(id)}
                );

                loadContacts(search.getText().toString());
            })
            .show();
    }
}
