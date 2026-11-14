
package com.nassar.accounting;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AccountingDb extends SQLiteOpenHelper {

    private static final String DB_NAME = "nassar_accounting.db";
    private static final int DB_VERSION = 1;

    public AccountingDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Customers and suppliers
        db.execSQL(
            "CREATE TABLE parties (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "type TEXT NOT NULL, " +
            "name TEXT NOT NULL, " +
            "phone TEXT, " +
            "address TEXT, " +
            "opening_balance REAL DEFAULT 0)"
        );

        // Products and inventory
        db.execSQL(
            "CREATE TABLE products (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "barcode TEXT, " +
            "unit TEXT DEFAULT 'قطعة', " +
            "quantity REAL DEFAULT 0, " +
            "buy_price REAL DEFAULT 0, " +
            "sell_price REAL DEFAULT 0)"
        );

        // Sales, purchases and returns
        db.execSQL(
            "CREATE TABLE invoices (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "invoice_type TEXT NOT NULL, " +
            "party_id INTEGER, " +
            "invoice_date INTEGER NOT NULL, " +
            "total REAL DEFAULT 0, " +
            "discount REAL DEFAULT 0, " +
            "paid REAL DEFAULT 0, " +
            "notes TEXT, " +
            "FOREIGN KEY(party_id) REFERENCES parties(id))"
        );

        // Invoice products
        db.execSQL(
            "CREATE TABLE invoice_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "invoice_id INTEGER NOT NULL, " +
            "product_id INTEGER NOT NULL, " +
            "quantity REAL NOT NULL, " +
            "price REAL NOT NULL, " +
            "total REAL NOT NULL, " +
            "FOREIGN KEY(invoice_id) REFERENCES invoices(id), " +
            "FOREIGN KEY(product_id) REFERENCES products(id))"
        );

        // Inventory movements
        db.execSQL(
            "CREATE TABLE stock_movements (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "product_id INTEGER NOT NULL, " +
            "invoice_id INTEGER, " +
            "quantity_change REAL NOT NULL, " +
            "movement_date INTEGER NOT NULL, " +
            "notes TEXT, " +
            "FOREIGN KEY(product_id) REFERENCES products(id))"
        );

        // Cashbox and payments
        db.execSQL(
            "CREATE TABLE cash_movements (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "movement_type TEXT NOT NULL, " +
            "amount REAL NOT NULL, " +
            "party_id INTEGER, " +
            "invoice_id INTEGER, " +
            "movement_date INTEGER NOT NULL, " +
            "category TEXT, " +
            "notes TEXT)"
        );

        // Expenses
        db.execSQL(
            "CREATE TABLE expenses (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "category TEXT NOT NULL, " +
            "amount REAL NOT NULL, " +
            "expense_date INTEGER NOT NULL, " +
            "notes TEXT)"
        );

        // Performance indexes
        db.execSQL(
            "CREATE INDEX idx_invoices_date " +
            "ON invoices(invoice_date)"
        );

        db.execSQL(
            "CREATE INDEX idx_stock_product " +
            "ON stock_movements(product_id)"
        );

        db.execSQL(
            "CREATE INDEX idx_cash_date " +
            "ON cash_movements(movement_date)"
        );
    }

    @Override
    public void onUpgrade(
        SQLiteDatabase db,
        int oldVersion,
        int newVersion
    ) {
        // Future upgrades will preserve existing records.
    }

    // Add customer or supplier
    public long addParty(
        String type,
        String name,
        String phone,
        String address
    ) {
        ContentValues values = new ContentValues();

        values.put("type", type);
        values.put("name", name);
        values.put("phone", phone);
        values.put("address", address);

        return getWritableDatabase().insertOrThrow(
            "parties", null, values
        );
    }

    // Add product
    public long addProduct(
        String name,
        double buyPrice,
        double sellPrice,
        double quantity
    ) {
        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("buy_price", buyPrice);
        values.put("sell_price", sellPrice);
        values.put("quantity", quantity);

        return getWritableDatabase().insertOrThrow(
            "products", null, values
        );
    }

    // Record cash movement
    public long addCashMovement(
        String type,
        double amount,
        String notes
    ) {
        ContentValues values = new ContentValues();

        values.put("movement_type", type);
        values.put("amount", amount);
        values.put(
            "movement_date",
            System.currentTimeMillis()
        );
        values.put("notes", notes);

        return getWritableDatabase().insertOrThrow(
            "cash_movements", null, values
        );
    }

    // Calculate cash balance
    public double getCashBalance() {

        double balance = 0;

        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(" +
            "CASE " +
            "WHEN movement_type IN ('receipt','capital') " +
            "THEN amount " +
            "ELSE -amount END), 0) " +
            "FROM cash_movements",
            null
        );

        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }

        cursor.close();

        return balance;
    }
}
