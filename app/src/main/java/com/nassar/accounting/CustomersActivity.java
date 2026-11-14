package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class CustomersActivity extends Activity {

    private AccountingDb helper;
    private LinearLayout list;
    private EditText search;

    private final int purple = Color.rgb(92, 45, 145);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        helper = new AccountingDb(this);

        try {
            migrateOldContacts();
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                .setTitle("تنبيه نقل البيانات")
                .setMessage(
                    "تعذر نقل بعض البيانات القديمة. " +
                    "لم يتم حذف قاعدة البيانات الأصلية.\n\n" +
                    e.getMessage()
                )
                .setPositiveButton("موافق", null)
                .show();
        }

        buildScreen();
    }

    private void migrateOldContacts() {

        SQLiteDatabase target = helper.getWritableDatabase();

        target.execSQL(
            "CREATE TABLE IF NOT EXISTS migration_status (" +
            "name TEXT PRIMARY KEY)"
        );

        try (Cursor check = target.rawQuery(
            "SELECT name FROM migration_status " +
            "WHERE name='old_contacts_v1'",
            null
        )) {
            if (check.moveToFirst()) {
                return;
            }
        }

        File oldFile = getDatabasePath("nassar_contacts.db");

        if (!oldFile.exists()) {
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
                "SELECT id,type,name,phone,address,balance " +
                "FROM contacts ORDER BY id",
                null
            )) {
                while (c.moveToNext()) {

                    String oldType = c.getString(1);

                    String type = "مورد".equals(oldType)
                        ? "supplier"
                        : "customer";

                    ContentValues values = new ContentValues();

                    values.put("type", type);
                    values.put("name", c.getString(2));
                    values.put("phone", c.getString(3));
                    values.put("address", c.getString(4));
                    values.put(
                        "opening_balance",
                        c.getDouble(5)
                    );

                    target.insertOrThrow(
                        "parties",
                        null,
                        values
                    );
                }
            }

            target.execSQL(
                "INSERT INTO migration_status(name) " +
                "VALUES('old_contacts_v1')"
            );

            target.setTransactionSuccessful();

        } finally {
            if (target.inTransaction()) {
                target.endTransaction();
            }

            old.close();
        }
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(16, 20, 16, 16);
        root.setBackgroundColor(
            Color.rgb(248, 247, 252)
        );

        TextView title = new TextView(this);
        title.setText("العملاء والموردون");
        title.setTextSize(26);
        title.setTextColor(purple);
        title.setGravity(Gravity.CENTER);

        root.addView(title);

        TextView description = new TextView(this);
        description.setText(
            "الأرصدة والفواتير في قاعدة بيانات واحدة"
        );
        description.setTextSize(14);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 4, 0, 12);

        root.addView(description);

        Button add = new Button(this);
        add.setText("+ إضافة عميل أو مورد");
        add.setAllCaps(false);

        root.addView(add);

        add.setOnClickListener(v ->
            editContact(
                -1,
                "customer",
                "",
                "",
                "",
                "0"
            )
        );

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
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());

        root.addView(back);

        setContentView(root);

        search.addTextChangedListener(
            new TextWatcher() {

                @Override
                public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
                ) {}

                @Override
                public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
                ) {
                    loadContacts(s.toString());
                }

                @Override
                public void afterTextChanged(
                    Editable e
                ) {}
            }
        );

        loadContacts("");
    }

    private String money(double value) {
        return String.format(
            Locale.US,
            "%.2f",
            value
        );
    }

    private String labelForType(String type) {
        return "supplier".equals(type)
            ? "مورد"
            : "عميل";
    }

    private double invoiceBalance(
        SQLiteDatabase database,
        long partyId
    ) {

        try (Cursor c = database.rawQuery(
            "SELECT COALESCE(" +
            "SUM(total - discount - paid), 0) " +
            "FROM invoices WHERE party_id=?",
            new String[]{
                String.valueOf(partyId)
            }
        )) {

            if (c.moveToFirst()) {
                return c.getDouble(0);
            }
        }

        return 0;
    }

    private void loadContacts(String query) {

        list.removeAllViews();

        String pattern = "%" + query.trim() + "%";

        SQLiteDatabase database =
            helper.getReadableDatabase();

        try (Cursor c = database.rawQuery(
            "SELECT id,type,name,phone,address," +
            "opening_balance " +
            "FROM parties " +
            "WHERE name LIKE ? OR phone LIKE ? " +
            "ORDER BY id DESC",
            new String[]{pattern, pattern}
        )) {

            if (c.getCount() == 0) {

                TextView empty = new TextView(this);
                empty.setText("لا توجد بيانات");
                empty.setTextSize(17);
                empty.setPadding(10, 30, 10, 30);

                list.addView(empty);
            }

            while (c.moveToNext()) {

                long id = c.getLong(0);
                String type = c.getString(1);
                String name = c.getString(2);

                String phone = c.isNull(3)
                    ? ""
                    : c.getString(3);

                String address = c.isNull(4)
                    ? ""
                    : c.getString(4);

                double opening = c.getDouble(5);

                double invoicesDue =
                    invoiceBalance(database, id);

                double balance = opening + invoicesDue;

                Button item = new Button(this);
                item.setAllCaps(false);

                item.setText(
                    labelForType(type) + " | " + name +
                    "\nالهاتف: " + phone +
                    "\nالرصيد الافتتاحي: " +
                    money(opening) + " جنيه" +
                    "\nمتبقي الفواتير: " +
                    money(invoicesDue) + " جنيه" +
                    "\nإجمالي الرصيد: " +
                    money(balance) + " جنيه"
                );

                list.addView(item);

                item.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                        .setTitle(name)
                        .setItems(
                            new String[]{
                                "تعديل البيانات",
                                "تفاصيل الرصيد",
                                "حذف"
                            },
                            (dialog, choice) -> {

                                if (choice == 0) {

                                    editContact(
                                        id,
                                        type,
                                        name,
                                        phone,
                                        address,
                                        money(opening)
                                    );

                                } else if (choice == 1) {

                                    showBalance(
                                        name,
                                        opening,
                                        invoicesDue
                                    );

                                } else {

                                    confirmDelete(
                                        id,
                                        name
                                    );
                                }
                            }
                        )
                        .show()
                );
            }
        }
    }

    private void showBalance(
        String name,
        double opening,
        double invoicesDue
    ) {

        new AlertDialog.Builder(this)
            .setTitle("رصيد: " + name)
            .setMessage(
                "الرصيد الافتتاحي: " +
                money(opening) + " جنيه\n\n" +
                "متبقي الفواتير: " +
                money(invoicesDue) + " جنيه\n\n" +
                "إجمالي الرصيد: " +
                money(opening + invoicesDue) +
                " جنيه\n\n" +
                "الرصيد الموجب يعني مبلغًا مستحقًا " +
                "على العميل أو للمورد حسب نوع الحساب.\n\n" +
                "لا يشمل هذا الحساب أي تحصيلات أو " +
                "مدفوعات مستقلة خارج الفواتير."
            )
            .setPositiveButton("إغلاق", null)
            .show();
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
        long id,
        String oldType,
        String oldName,
        String oldPhone,
        String oldAddress,
        String oldBalance
    ) {

        ScrollView scroll = new ScrollView(this);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(28, 10, 28, 10);

        Spinner type = new Spinner(this);

        String[] types = {
            "عميل",
            "مورد"
        };

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                types
            );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        type.setAdapter(adapter);

        type.setSelection(
            "supplier".equals(oldType) ? 1 : 0
        );

        form.addView(type);

        EditText name = input(
            form,
            "الاسم",
            oldName
        );

        EditText phone = input(
            form,
            "رقم الهاتف",
            oldPhone
        );

        phone.setInputType(
            InputType.TYPE_CLASS_PHONE
        );

        EditText address = input(
            form,
            "العنوان",
            oldAddress
        );

        EditText balance = input(
            form,
            "الرصيد الافتتاحي",
            oldBalance
        );

        balance.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL |
            InputType.TYPE_NUMBER_FLAG_SIGNED
        );

        scroll.addView(form);

        AlertDialog dialog =
            new AlertDialog.Builder(this)
                .setTitle(
                    id < 0
                        ? "إضافة عميل أو مورد"
                        : "تعديل البيانات"
                )
                .setView(scroll)
                .setNegativeButton(
                    "إلغاء",
                    null
                )
                .setPositiveButton(
                    "حفظ",
                    null
                )
                .create();

        dialog.setOnShowListener(d ->
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String n =
                    name.getText().toString().trim();

                if (n.isEmpty()) {
                    name.setError("الاسم مطلوب");
                    return;
                }

                double amount;

                try {

                    amount = Double.parseDouble(
                        balance.getText()
                            .toString()
                            .trim()
                    );

                    if (!Double.isFinite(amount)) {
                        throw new NumberFormatException();
                    }

                } catch (NumberFormatException ex) {

                    balance.setError(
                        "أدخل رصيدًا صحيحًا"
                    );

                    return;
                }

                String selectedType =
                    type.getSelectedItemPosition() == 1
                        ? "supplier"
                        : "customer";

                ContentValues values =
                    new ContentValues();

                values.put("type", selectedType);
                values.put("name", n);

                values.put(
                    "phone",
                    phone.getText()
                        .toString()
                        .trim()
                );

                values.put(
                    "address",
                    address.getText()
                        .toString()
                        .trim()
                );

                values.put(
                    "opening_balance",
                    amount
                );

                SQLiteDatabase database =
                    helper.getWritableDatabase();

                try {

                    if (id < 0) {

                        database.insertOrThrow(
                            "parties",
                            null,
                            values
                        );

                    } else {

                        if (
                            !selectedType.equals(oldType) &&
                            hasInvoices(database, id)
                        ) {

                            Toast.makeText(
                                this,
                                "لا يمكن تغيير نوع حساب " +
                                "مرتبط بفواتير",
                                Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        database.update(
                            "parties",
                            values,
                            "id=?",
                            new String[]{
                                String.valueOf(id)
                            }
                        );
                    }

                    dialog.dismiss();

                    loadContacts(
                        search.getText().toString()
                    );

                } catch (Exception ex) {

                    Toast.makeText(
                        this,
                        "تعذر حفظ البيانات: " +
                        ex.getMessage(),
                        Toast.LENGTH_LONG
                    ).show();
                }
            })
        );

        dialog.show();
    }

    private boolean hasInvoices(
        SQLiteDatabase database,
        long id
    ) {

        try (Cursor c = database.rawQuery(
            "SELECT 1 FROM invoices " +
            "WHERE party_id=? LIMIT 1",
            new String[]{
                String.valueOf(id)
            }
        )) {

            return c.moveToFirst();
        }
    }

    private void confirmDelete(
        long id,
        String name
    ) {

        new AlertDialog.Builder(this)
            .setTitle("تأكيد الحذف")
            .setMessage(
                "هل تريد حذف " + name + "؟"
            )
            .setNegativeButton(
                "إلغاء",
                null
            )
            .setPositiveButton(
                "حذف",
                (d, w) -> {

                    SQLiteDatabase database =
                        helper.getWritableDatabase();

                    try {

                        if (
                            hasInvoices(database, id)
                        ) {

                            Toast.makeText(
                                this,
                                "لا يمكن حذف حساب " +
                                "مرتبط بفواتير",
                                Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        database.delete(
                            "parties",
                            "id=?",
                            new String[]{
                                String.valueOf(id)
                            }
                        );

                        loadContacts(
                            search.getText().toString()
                        );

                    } catch (SQLiteException e) {

                        Toast.makeText(
                            this,
                            "تعذر حذف الحساب " +
                            "لوجود سجلات مرتبطة به",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                }
            )
            .show();
    }

    @Override
    protected void onDestroy() {

        if (helper != null) {
            helper.close();
        }

        super.onDestroy();
    }
}
