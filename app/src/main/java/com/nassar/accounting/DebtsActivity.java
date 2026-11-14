package com.nassar.accounting;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public class DebtsActivity extends Activity {

    AccountingDb helper;
    LinearLayout list;
    Spinner party;
    EditText amount, notes;
    ArrayList<Long> ids = new ArrayList<>();
    ArrayList<String> types = new ArrayList<>();
    final int purple = Color.rgb(92,45,145);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        helper = new AccountingDb(this);
        showScreen();
    }

    TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(17);
        t.setPadding(8,12,8,12);
        return t;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    String money(double d) {
        return String.format(Locale.US,"%.2f",d);
    }

    void showScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(18,20,18,18);
        root.setBackgroundColor(Color.rgb(248,247,252));

        TextView title = label("الديون والتحصيلات");
        title.setTextSize(26);
        title.setTextColor(purple);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        root.addView(label("اختر العميل أو المورد"));
        party = new Spinner(this);
        root.addView(party);

        amount = new EditText(this);
        amount.setHint("المبلغ");
        amount.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(amount);

        notes = new EditText(this);
        notes.setHint("ملاحظات");
        root.addView(notes);

        Button save = button("تسجيل التحصيل / الدفع");
        root.addView(save);
        save.setOnClickListener(v -> savePayment());

        root.addView(label("الأرصدة الحالية"));

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);
        root.addView(scroll,
            new LinearLayout.LayoutParams(-1,0,1));

        Button back = button("رجوع");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(root);
        loadParties();
        loadBalances();
    }

    void loadParties() {
        ids.clear();
        types.clear();
        ArrayList<String> names = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();
        try(Cursor c = db.rawQuery(
            "SELECT id,type,name FROM parties ORDER BY name",
            null
        )) {
            while(c.moveToNext()) {
                ids.add(c.getLong(0));
                types.add(c.getString(1));
                names.add(
                    c.getString(2) + " (" +
                    (c.getString(1).equals("supplier")
                        ? "مورد" : "عميل") + ")"
                );
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            names
        );
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        party.setAdapter(adapter);
    }

    double getBalance(SQLiteDatabase db,long id) {
        double balance = 0;

        try(Cursor c = db.rawQuery(
            "SELECT opening_balance FROM parties WHERE id=?",
            new String[]{String.valueOf(id)}
        )) {
            if(c.moveToFirst()) balance = c.getDouble(0);
        }

        try(Cursor c = db.rawQuery(
            "SELECT COALESCE(SUM(total-discount-paid),0) " +
            "FROM invoices WHERE party_id=?",
            new String[]{String.valueOf(id)}
        )) {
            if(c.moveToFirst()) balance += c.getDouble(0);
        }

        String kind = "customer";
        try(Cursor c = db.rawQuery(
            "SELECT type FROM parties WHERE id=?",
            new String[]{String.valueOf(id)}
        )) {
            if(c.moveToFirst()) kind = c.getString(0);
        }

        String movement = kind.equals("supplier")
            ? "payment" : "receipt";

        try(Cursor c = db.rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM cash_movements " +
            "WHERE party_id=? AND movement_type=? " +
            "AND invoice_id IS NULL",
            new String[]{String.valueOf(id),movement}
        )) {
            if(c.moveToFirst()) balance -= c.getDouble(0);
        }

        return balance;
    }

    void loadBalances() {
        list.removeAllViews();
        SQLiteDatabase db = helper.getReadableDatabase();

        try(Cursor c = db.rawQuery(
            "SELECT id,type,name FROM parties ORDER BY name",
            null
        )) {
            while(c.moveToNext()) {
                long id = c.getLong(0);
                String type = c.getString(1);
                String name = c.getString(2);
                double balance = getBalance(db,id);

                Button row = button(
                    name + " | " +
                    (type.equals("supplier") ? "مورد" : "عميل") +
                    "\nالرصيد المستحق: " + money(balance) + " جنيه"
                );
                list.addView(row);
            }
        }
    }

    void savePayment() {
        int pos = party.getSelectedItemPosition();

        if(pos < 0 || pos >= ids.size()) {
            toast("أضف عميلًا أو موردًا أولًا");
            return;
        }

        double value;
        try {
            value = Double.parseDouble(
                amount.getText().toString().trim()
            );
            if(!Double.isFinite(value) || value <= 0)
                throw new NumberFormatException();
        } catch(Exception e) {
            toast("أدخل مبلغًا صحيحًا أكبر من صفر");
            return;
        }

        long id = ids.get(pos);
        String kind = types.get(pos);

        SQLiteDatabase db = helper.getWritableDatabase();
        double balance = getBalance(db,id);

        if(balance <= 0 || value > balance + 0.000001) {
            toast("المبلغ أكبر من الرصيد المستحق");
            return;
        }

        String movement = kind.equals("supplier")
            ? "payment" : "receipt";

        ContentValues v = new ContentValues();
        v.put("movement_type",movement);
        v.put("amount",value);
        v.put("party_id",id);
        v.put("movement_date",System.currentTimeMillis());
        v.put("notes",notes.getText().toString().trim());

        try {
            db.insertOrThrow("cash_movements",null,v);
            amount.setText("");
            notes.setText("");
            loadBalances();
            toast("تم تسجيل الحركة بنجاح");
        } catch(Exception e) {
            toast("تعذر حفظ الحركة");
        }
    }

    void toast(String s) {
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if(helper != null) helper.close();
        super.onDestroy();
    }
}
