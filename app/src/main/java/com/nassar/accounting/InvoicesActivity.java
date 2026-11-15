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

public class InvoicesActivity extends Activity {
    AccountingDb helper;
    LinearLayout form, itemsView;
    Spinner type, party, product;
    EditText qty, price, discount, paid, notes;
    TextView totals;
    ArrayList<Long> partyIds = new ArrayList<>(), productIds = new ArrayList<>();
    ArrayList<Double> buyPrices = new ArrayList<>(), sellPrices = new ArrayList<>();
    ArrayList<Line> lines = new ArrayList<>();
    static class Line {
        long id; String name; double qty, price;
        Line(long i, String n, double q, double p) { id=i; name=n; qty=q; price=p; }
        double total() { return qty*price; }
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); helper = new AccountingDb(this); showForm();
    }
    TextView label(String s, int size) {
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size);
        v.setTextColor(0xFF592B93); v.setPadding(8,14,8,8); return v;
    }
    void caption(String s) { form.addView(label(s,16)); }
    EditText field(String name, boolean numeric, String initial) {
        caption(name); EditText e=new EditText(this); e.setSingleLine(true);
        e.setHint("أدخل " + name); e.setText(initial); e.setSelectAllOnFocus(true);
        if(numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(e); return e;
    }
    Button button(String title, Runnable run) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false);
        form.addView(b); b.setOnClickListener(v->run.run()); return b;
    }
    Spinner spinner(String[] names) {
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a); return s;
    }
    void showForm() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFF8F6FC);
        form=new LinearLayout(this); form.setOrientation(1);
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); form.setPadding(18,16,18,28);
        scroll.addView(form); setContentView(scroll);
        TextView title=label("نصار للحسابات — الفواتير",24);
        title.setGravity(Gravity.CENTER); form.addView(title);
        caption("نوع الفاتورة"); type=spinner(new String[]{"فاتورة بيع","فاتورة شراء"});
        form.addView(type);
        caption("العميل / المورد"); party=spinner(new String[]{"بدون حساب"}); form.addView(party);
        caption("الصنف"); product=spinner(new String[]{"لا توجد أصناف"}); form.addView(product);
        qty=field("الكمية",true,"1");
        price=field("سعر الوحدة",true,"");
        button("＋ إضافة الصنف للفاتورة",this::addLine);
        caption("أصناف الفاتورة (اضغط على صنف لحذفه)");
        itemsView=new LinearLayout(this); itemsView.setOrientation(1); form.addView(itemsView);
        discount=field("الخصم بالجنيه",true,"");
        paid=field("المبلغ المدفوع",true,"");
        notes=field("ملاحظات الفاتورة",false,"");
        totals=label("الإجمالي: 0.00 جنيه",20); form.addView(totals);
        button("💾 حفظ الفاتورة",this::save);
        button("عرض الفواتير السابقة",this::history);
        button("رجوع",this::finish);
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id) { loadParties(); defaultPrice(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        product.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id) { defaultPrice(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        discount.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int c,int f) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { refresh(); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        String initial=getIntent().getStringExtra("invoice_type");
        if(initial==null) initial=getIntent().getStringExtra("type");
        if("purchase".equals(initial) || (initial==null && !getIntent().getBooleanExtra("isSale",true))) type.setSelection(1);
        loadProducts(); loadParties(); defaultPrice(); refresh();
    }
    void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    double read(EditText e) {
        String s=e.getText().toString().trim().replace(",","");
        if(s.isEmpty()) return 0;
        double n=Double.parseDouble(s);
        if(!Double.isFinite(n)||n<0) throw new IllegalArgumentException("القيمة غير صحيحة");
        return n;
    }
    String fmt(double d) { return String.format(Locale.US,"%.2f",d); }
    boolean sale() { return type.getSelectedItemPosition()==0; }
    void loadParties() {
        partyIds.clear(); ArrayList<String> names=new ArrayList<>();
        names.add("نقدي / بدون حساب"); partyIds.add(-1L);
        try(Cursor c=helper.getReadableDatabase().rawQuery(
                "SELECT id,name FROM parties WHERE type=? ORDER BY name",
                new String[]{sale()?"customer":"supplier"})) {
            while(c.moveToNext()) { partyIds.add(c.getLong(0)); names.add(c.getString(1)); }
        }
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); party.setAdapter(a);
    }
    void loadProducts() {
        productIds.clear(); buyPrices.clear(); sellPrices.clear();
        ArrayList<String> names=new ArrayList<>();
        try(Cursor c=helper.getReadableDatabase().rawQuery(
                "SELECT id,name,buy_price,sell_price FROM products ORDER BY name",null)) {
            while(c.moveToNext()) { productIds.add(c.getLong(0)); names.add(c.getString(1));
                buyPrices.add(c.getDouble(2)); sellPrices.add(c.getDouble(3)); }
        }
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); product.setAdapter(a);
    }
    void defaultPrice() {
        if(price==null||product==null||type==null) return;
        int i=product.getSelectedItemPosition();
        if(i>=0&&i<productIds.size()) price.setText(fmt(sale()?sellPrices.get(i):buyPrices.get(i)));
    }
    void addLine() {
        int i=product.getSelectedItemPosition();
        if(i<0||i>=productIds.size()) { toast("أضف صنفًا من قسم الأصناف أولًا"); return; }
        try {
            double q=read(qty),p=read(price);
            if(q<=0) { toast("الكمية يجب أن تكون أكبر من صفر"); return; }
            String name=product.getSelectedItem().toString();
            if(!Double.isFinite(q*p)) throw new IllegalArgumentException("الإجمالي غير صحيح");
            lines.add(new Line(productIds.get(i),name,q,p)); refresh();
        } catch(Exception e) { toast("راجع الكمية والسعر: "+e.getMessage()); }
    }
    double subtotal() { double s=0; for(Line l:lines) s+=l.total(); return s; }
    void refresh() {
        if(itemsView==null||totals==null) return;
        itemsView.removeAllViews();
        for(int i=0;i<lines.size();i++) {
            final int index=i; Line l=lines.get(i);
            Button b=new Button(this); b.setAllCaps(false);
            b.setText(l.name+" — "+fmt(l.qty)+" × "+fmt(l.price)+" = "+fmt(l.total())+" ج  ✕");
            b.setOnClickListener(v->{lines.remove(index);refresh();}); itemsView.addView(b);
        }
        double d=0; try {d=read(discount);} catch(Exception ignored) {}
        totals.setText("الإجمالي قبل الخصم: "+fmt(subtotal())+" ج\nالخصم: "+fmt(d)+" ج\nالصافي: "+fmt(Math.max(0,subtotal()-d))+" ج");
    }
    void save() {
        if(lines.isEmpty()) { toast("أضف صنفًا واحدًا على الأقل"); return; }
        final double d,amount,total;
        try {
            d=read(discount); amount=read(paid); total=subtotal()-d;
            if(!Double.isFinite(total)||total<0||amount>total)
                throw new IllegalArgumentException("راجع الخصم والمدفوع");
        } catch(Exception e) { toast("راجع الخصم والمدفوع: "+e.getMessage()); return; }
        int pos=party.getSelectedItemPosition();
        long partyId=pos>=0&&pos<partyIds.size()?partyIds.get(pos):-1;
        if(amount<total&&partyId<0) { toast("الفاتورة الآجلة تحتاج عميلًا أو موردًا"); return; }
        SQLiteDatabase db=helper.getWritableDatabase();
        try {
            db.beginTransaction();
            if(sale()) {
                HashMap<Long,Double> required=new HashMap<>();
                for(Line l:lines) required.put(l.id,required.containsKey(l.id)?required.get(l.id)+l.qty:l.qty);
                for(Map.Entry<Long,Double> e:required.entrySet()) {
                    try(Cursor c=db.rawQuery("SELECT quantity FROM products WHERE id=?",
                            new String[]{String.valueOf(e.getKey())})) {
                        if(!c.moveToFirst()||c.getDouble(0)+0.000001<e.getValue())
                            throw new IllegalStateException("كمية المخزون غير كافية لأحد الأصناف");
                    }
                }
            }
            long now=System.currentTimeMillis();
            ContentValues inv=new ContentValues();
            inv.put("invoice_type",sale()?"sale":"purchase");
            if(partyId>=0) inv.put("party_id",partyId);
            inv.put("invoice_date",now); inv.put("total",subtotal());
            inv.put("discount",d); inv.put("paid",amount);
            inv.put("notes",notes.getText().toString());
            long invoiceId=db.insertOrThrow("invoices",null,inv);
            for(Line l:lines) {
                ContentValues item=new ContentValues();
                item.put("invoice_id",invoiceId); item.put("product_id",l.id);
                item.put("quantity",l.qty); item.put("price",l.price); item.put("total",l.total());
                db.insertOrThrow("invoice_items",null,item);
                double delta=sale()?-l.qty:l.qty;
                db.execSQL("UPDATE products SET quantity=quantity+? WHERE id=?",new Object[]{delta,l.id});
                ContentValues move=new ContentValues();
                move.put("product_id",l.id); move.put("invoice_id",invoiceId);
                move.put("quantity_change",delta); move.put("movement_date",now);
                move.put("notes",sale()?"فاتورة بيع":"فاتورة شراء");
                db.insertOrThrow("stock_movements",null,move);
            }
            if(amount>0) {
                ContentValues cash=new ContentValues();
                cash.put("movement_type",sale()?"receipt":"payment");
                cash.put("amount",amount); if(partyId>=0) cash.put("party_id",partyId);
                cash.put("invoice_id",invoiceId); cash.put("movement_date",now);
                cash.put("notes","دفعة فاتورة رقم "+invoiceId);
                db.insertOrThrow("cash_movements",null,cash);
            }
            db.setTransactionSuccessful();
            toast("تم حفظ الفاتورة رقم "+invoiceId); lines.clear(); showForm();
        } catch(Exception e) {
            new AlertDialog.Builder(this).setTitle("تعذر الحفظ")
                .setMessage(e.getMessage()).setPositiveButton("موافق",null).show();
        } finally {if(db.inTransaction()) db.endTransaction();}
    }
    void history() {
        StringBuilder out=new StringBuilder();
        try(Cursor c=helper.getReadableDatabase().rawQuery(
                "SELECT id,invoice_type,total,discount,paid FROM invoices ORDER BY id DESC LIMIT 100",null)) {
            while(c.moveToNext()) {
                out.append("فاتورة #").append(c.getLong(0)).append(" — ")
                    .append("sale".equals(c.getString(1))?"بيع":"شراء")
                    .append("\nالصافي: ").append(fmt(c.getDouble(2)-c.getDouble(3)))
                    .append(" | المدفوع: ").append(fmt(c.getDouble(4))).append(" ج\n\n");
            }
        }
        new AlertDialog.Builder(this).setTitle("الفواتير السابقة")
            .setMessage(out.length()==0?"لا توجد فواتير":out.toString())
            .setPositiveButton("إغلاق",null).show();
    }
    @Override protected void onDestroy() {if(helper!=null) helper.close();super.onDestroy();}
}
