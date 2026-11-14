
package com.nassar.accounting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
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

import java.util.ArrayList;
import java.util.Locale;

public class InvoicesActivity extends Activity {

    private AccountingDb helper;
    private LinearLayout root;
    private LinearLayout linesContainer;

    private Spinner invoiceType;
    private Spinner partySpinner;
    private Spinner productSpinner;

    private EditText quantityInput;
    private EditText priceInput;
    private EditText paidInput;
    private EditText notesInput;

    private TextView totalLabel;

    private final ArrayList<Long> partyIds =
        new ArrayList<>();

    private final ArrayList<Long> productIds =
        new ArrayList<>();

    private final ArrayList<String> productNames =
        new ArrayList<>();

    private final ArrayList<Double> productBuy =
        new ArrayList<>();

    private final ArrayList<Double> productSell =
        new ArrayList<>();

    private final ArrayList<InvoiceLine> lines =
        new ArrayList<>();

    private static class InvoiceLine {
        long productId;
        String name;
        double quantity;
        double price;

        InvoiceLine(
            long productId,
            String name,
            double quantity,
            double price
        ) {
            this.productId = productId;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        double total() {
            return quantity * price;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        helper = new AccountingDb(this);
        showInvoiceForm();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(45, 40, 55));
        t.setPadding(8, 12, 8, 8);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        return e;
    }

    private EditText numberInput(String hint) {
        EditText e = input(hint);
        e.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
            );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        s.setAdapter(adapter);
        return s;
    }

    private void showInvoiceForm() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(18, 20, 18, 18);
        root.setBackgroundColor(Color.rgb(248, 247, 252));

        ScrollView scroll = new ScrollView(this);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("الفواتير", 27);
        title.setTextColor(Color.rgb(92, 45, 145));
        title.setGravity(Gravity.CENTER);
        form.addView(title);

        form.addView(text("نوع الفاتورة", 16));

        invoiceType = spinner(new String[]{
            "مبيعات",
            "مشتريات"
        });

        form.addView(invoiceType);

        form.addView(text("العميل / المورد", 16));

        partySpinner = new Spinner(this);
        form.addView(partySpinner);

        invoiceType.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(
                    android.widget.AdapterView<?> parent,
                    View view,
                    int position,
                    long id
                ) {
                    loadParties();
                    updateDefaultPrice();
                }

                @Override
                public void onNothingSelected(
                    android.widget.AdapterView<?> parent
                ) {
                }
            }
        );

        form.addView(text("الصنف", 16));

        productSpinner = new Spinner(this);
        form.addView(productSpinner);

        loadProducts();

        productSpinner.setOnItemSelectedListener(
            new android.widget.AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(
                    android.widget.AdapterView<?> parent,
                    View view,
                    int position,
                    long id
                ) {
                    updateDefaultPrice();
                }

                @Override
                public void onNothingSelected(
                    android.widget.AdapterView<?> parent
                ) {
                }
            }
        );

        quantityInput = numberInput("الكمية");
        quantityInput.setText("1");
        form.addView(quantityInput);

        priceInput = numberInput("سعر الوحدة");
        form.addView(priceInput);

        Button addLine = button("+ إضافة الصنف للفاتورة");
        form.addView(addLine);

        linesContainer = new LinearLayout(this);
        linesContainer.setOrientation(LinearLayout.VERTICAL);
        form.addView(linesContainer);

        totalLabel = text("الإجمالي: 0.00 جنيه", 21);
        totalLabel.setTextColor(Color.rgb(92, 45, 145));
        form.addView(totalLabel);

        paidInput = numberInput("المبلغ المدفوع");
        paidInput.setText("0");
        form.addView(paidInput);

        notesInput = input("ملاحظات الفاتورة");
        form.addView(notesInput);

        Button save = button("حفظ الفاتورة");
        form.addView(save);

        Button history = button("عرض الفواتير السابقة");
        form.addView(history);

        Button back = button("رجوع");
        form.addView(back);

        addLine.setOnClickListener(v -> addLine());

        save.setOnClickListener(v -> saveInvoice());

        history.setOnClickListener(v -> showHistory());

        back.setOnClickListener(v -> finish());

        scroll.addView(form);
        root.addView(scroll);
        setContentView(root);

        loadParties();
        updateDefaultPrice();
    }

    private void loadParties() {

        partyIds.clear();

        ArrayList<String> names = new ArrayList<>();
        names.add("بدون عميل / مورد");
        partyIds.add(-1L);

        String kind =
            invoiceType.getSelectedItemPosition() == 0
                ? "customer"
                : "supplier";

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor c = db.rawQuery(
            "SELECT id,name FROM parties " +
            "WHERE type=? ORDER BY name",
            new String[]{kind}
        )) {
            while (c.moveToNext()) {
                partyIds.add(c.getLong(0));
                names.add(c.getString(1));
            }
        }

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                names
            );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        partySpinner.setAdapter(adapter);
    }

    private void loadProducts() {

        productIds.clear();
        productNames.clear();
        productBuy.clear();
        productSell.clear();

        SQLiteDatabase db = helper.getReadableDatabase();

        try (Cursor c = db.rawQuery(
            "SELECT id,name,buy_price,sell_price " +
            "FROM products ORDER BY name",
            null
        )) {
            while (c.moveToNext()) {

                productIds.add(c.getLong(0));
                productNames.add(c.getString(1));
                productBuy.add(c.getDouble(2));
                productSell.add(c.getDouble(3));
            }
        }

        ArrayAdapter<String> adapter =
            new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                productNames
            );

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );

        productSpinner.setAdapter(adapter);
    }

    private void updateDefaultPrice() {

        if (
            priceInput == null ||
            productSpinner == null ||
            invoiceType == null
        ) {
            return;
        }

        int position = productSpinner.getSelectedItemPosition();

        if (position < 0 || position >= productIds.size()) {
            return;
        }

        boolean sale =
            invoiceType.getSelectedItemPosition() == 0;

        double price = sale
            ? productSell.get(position)
            : productBuy.get(position);

        priceInput.setText(
            String.format(Locale.US, "%.2f", price)
        );
    }

    private double parseNumber(EditText input) {
        return Double.parseDouble(
            input.getText().toString().trim()
        );
    }

    private void addLine() {

        int position = productSpinner.getSelectedItemPosition();

        if (position < 0 || position >= productIds.size()) {
            message("أضف أصنافًا أولًا");
            return;
        }

        try {

            double qty = parseNumber(quantityInput);
            double price = parseNumber(priceInput);

            if (
                !Double.isFinite(qty) ||
                !Double.isFinite(price) ||
                qty <= 0 ||
                price < 0
            ) {
                message("راجع الكمية والسعر");
                return;
            }

            InvoiceLine line = new InvoiceLine(
                productIds.get(position),
                productNames.get(position),
                qty,
                price
            );

            if (!Double.isFinite(line.total())) {
                message("قيمة الصنف غير صحيحة");
                return;
            }

            lines.add(line);
            refreshLines();

        } catch (NumberFormatException e) {
            message("أدخل الكمية والسعر بشكل صحيح");
        }
    }

    private void refreshLines() {

        linesContainer.removeAllViews();

        for (int i = 0; i < lines.size(); i++) {

            final int index = i;
            InvoiceLine line = lines.get(i);

            Button row = button(
                line.name +
                "\n" +
                line.quantity +
                " × " +
                money(line.price) +
                " = " +
                money(line.total()) +
                " جنيه\nاضغط للحذف"
            );

            row.setOnClickListener(v -> {

                lines.remove(index);
                refreshLines();
            });

            linesContainer.addView(row);
        }

        totalLabel.setText(
            "الإجمالي: " + money(calculateTotal()) + " جنيه"
        );
    }

    private double calculateTotal() {

        double total = 0;

        for (InvoiceLine line : lines) {
            total += line.total();
        }

        return total;
    }

    private String money(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void saveInvoice() {

        if (lines.isEmpty()) {
            message("أضف صنفًا واحدًا على الأقل");
            return;
        }

        double total = calculateTotal();
        double paid;

        try {
            paid = parseNumber(paidInput);
        } catch (NumberFormatException e) {
            message("أدخل المبلغ المدفوع");
            return;
        }

        if (
            !Double.isFinite(total) ||
            !Double.isFinite(paid) ||
            total < 0 ||
            paid < 0 ||
            paid > total
        ) {
            message("راجع الإجمالي والمبلغ المدفوع");
            return;
        }

        boolean sale =
            invoiceType.getSelectedItemPosition() == 0;

        long partyId = partyIds.get(
            partySpinner.getSelectedItemPosition()
        );

        if (paid < total && partyId < 0) {
            message("الفاتورة الآجلة تحتاج عميلًا أو موردًا");
            return;
        }

        SQLiteDatabase db = helper.getWritableDatabase();

        try {

            db.beginTransaction();

            // Check stock before a sale.
            if (sale) {

                for (InvoiceLine line : lines) {

                    double available = 0;

                    try (Cursor c = db.rawQuery(
                        "SELECT quantity FROM products WHERE id=?",
                        new String[]{
                            String.valueOf(line.productId)
                        }
                    )) {
                        if (!c.moveToFirst()) {
                            throw new IllegalStateException(
                                "الصنف غير موجود"
                            );
                        }

                        available = c.getDouble(0);
                    }

                    double required = 0;

                    for (InvoiceLine other : lines) {
                        if (other.productId == line.productId) {
                            required += other.quantity;
                        }
                    }

                    if (available + 0.000001 < required) {
                        throw new IllegalStateException(
                            "الكمية غير كافية: " + line.name
                        );
                    }
                }
            }

            ContentValues invoice = new ContentValues();

            invoice.put(
                "invoice_type",
                sale ? "sale" : "purchase"
            );

            if (partyId >= 0) {
                invoice.put("party_id", partyId);
            }

            long now = System.currentTimeMillis();

            invoice.put("invoice_date", now);
            invoice.put("total", total);
            invoice.put("discount", 0);
            invoice.put("paid", paid);
            invoice.put(
                "notes",
                notesInput.getText().toString()
            );

            long invoiceId = db.insertOrThrow(
                "invoices", null, invoice
            );

            for (InvoiceLine line : lines) {

                ContentValues item = new ContentValues();

                item.put("invoice_id", invoiceId);
                item.put("product_id", line.productId);
                item.put("quantity", line.quantity);
                item.put("price", line.price);
                item.put("total", line.total());

                db.insertOrThrow(
                    "invoice_items", null, item
                );

                double delta = sale
                    ? -line.quantity
                    : line.quantity;

                db.execSQL(
                    "UPDATE products " +
                    "SET quantity=quantity+? WHERE id=?",
                    new Object[]{
                        delta,
                        line.productId
                    }
                );

                ContentValues move = new ContentValues();

                move.put("product_id", line.productId);
                move.put("invoice_id", invoiceId);
                move.put("quantity_change", delta);
                move.put("movement_date", now);
                move.put(
                    "notes",
                    sale ? "فاتورة بيع" : "فاتورة شراء"
                );

                db.insertOrThrow(
                    "stock_movements", null, move
                );
            }

            if (paid > 0) {

                ContentValues cash = new ContentValues();

                cash.put(
                    "movement_type",
                    sale ? "receipt" : "payment"
                );

                cash.put("amount", paid);

                if (partyId >= 0) {
                    cash.put("party_id", partyId);
                }

                cash.put("invoice_id", invoiceId);
                cash.put("movement_date", now);
                cash.put(
                    "notes",
                    "دفعة فاتورة رقم " + invoiceId
                );

                db.insertOrThrow(
                    "cash_movements", null, cash
                );
            }

            db.setTransactionSuccessful();

            message("تم حفظ الفاتورة رقم " + invoiceId);

            lines.clear();
            showInvoiceForm();

        } catch (Exception e) {

            new AlertDialog.Builder(this)
                .setTitle("تعذر حفظ الفاتورة")
                .setMessage(e.getMessage())
                .setPositiveButton("موافق", null)
                .show();

        } finally {

            if (db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    private void showHistory() {

        SQLiteDatabase db = helper.getReadableDatabase();

        StringBuilder result = new StringBuilder();

        try (Cursor c = db.rawQuery(
            "SELECT id,invoice_type,total,paid " +
            "FROM invoices ORDER BY id DESC LIMIT 100",
            null
        )) {

            while (c.moveToNext()) {

                long id = c.getLong(0);

                String kind = c.getString(1).equals("sale")
                    ? "بيع"
                    : "شراء";

                double total = c.getDouble(2);
                double paid = c.getDouble(3);

                result.append("فاتورة رقم: ")
                    .append(id)
                    .append("\nالنوع: ")
                    .append(kind)
                    .append("\nالإجمالي: ")
                    .append(money(total))
                    .append("\nالمدفوع: ")
                    .append(money(paid))
                    .append("\nالمتبقي: ")
                    .append(money(total - paid))
                    .append("\n\n");
            }
        }

        if (result.length() == 0) {
            result.append("لا توجد فواتير محفوظة");
        }

        new AlertDialog.Builder(this)
            .setTitle("آخر 100 فاتورة")
            .setMessage(result.toString())
            .setPositiveButton("إغلاق", null)
            .show();
    }

    private void message(String value) {
        Toast.makeText(
            this,
            value,
            Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        if (helper != null) {
            helper.close();
        }
        super.onDestroy();
    }
}
