package com.sanshy.farmmanagement.extras.side_business;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.sanshy.farmmanagement.MyStatic;
import com.sanshy.farmmanagement.R;
import com.sanshy.farmmanagement.extras.SingleExpOrInc;
import com.sanshy.farmmanagement.ThreeColumnsList;
import com.sanshy.farmmanagement.ThreeColumnsListAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.sanshy.farmmanagement.MyStatic.ChooseDateDialogFrom;
import static com.sanshy.farmmanagement.MyStatic.ChooseDateDialogSingle;
import static com.sanshy.farmmanagement.MyStatic.ChooseDateDialogTo;
import static com.sanshy.farmmanagement.MyStatic.DateToString;
import static com.sanshy.farmmanagement.MyStatic.DateToString2;
import static com.sanshy.farmmanagement.MyStatic.EXPENDITURE_FIRESTORE_FIELD_KEY;
import static com.sanshy.farmmanagement.MyStatic.HideProgress;
import static com.sanshy.farmmanagement.MyStatic.INCOME_FIRESTORE_FIELD_KEY;
import static com.sanshy.farmmanagement.MyStatic.ShowProgress;
import static com.sanshy.farmmanagement.MyStatic.sideIncomeLocation;
import static com.sanshy.farmmanagement.MyStatic.sideReportLocation;

public class SideIncomeHistory extends AppCompatActivity {

    ListView SideIncomeListView;
    ArrayList<ThreeColumnsList> SEList = new ArrayList<>();
    ArrayList<SingleExpOrInc> DataList = new ArrayList<>();
    ThreeColumnsListAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_side_income_history);
        SideIncomeListView = findViewById(R.id.side_income_history_list);

        myAdapter = new ThreeColumnsListAdapter(this,SEList);

        SideIncomeListView.setAdapter(myAdapter);

        SideIncomeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SideIncomeHistory.this);

                builder.setTitle(getString(R.string.undo));
                builder.setMessage(getString(R.string.remark)+" : "+DataList.get(position).getRemark()+"\n" +
                        getString(R.string.amount)+" : "+String.valueOf(DataList.get(position).getAmount())+"\n" +
                        getString(R.string.date)+" : "+DateToString(DataList.get(position).getDate()));
                builder.setPositiveButton(getString(R.string.undo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ShowProgress(SideIncomeHistory.this);
                        sideIncomeLocation.document(DataList.get(position).getId()).delete();
                        sideReportLocation.document(DateToString2(DataList.get(position).getDate())).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                double oldValue = documentSnapshot.getDouble(INCOME_FIRESTORE_FIELD_KEY);

                                double newValue = oldValue - DataList.get(position).getAmount();

                                Map<String,Object> DataMap = new HashMap<>();
                                DataMap.put(INCOME_FIRESTORE_FIELD_KEY,newValue);

                                sideReportLocation.document(DateToString2(DataList.get(position).getDate())).update(DataMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        DataList.remove(position);
                                        SEList.remove(position);
                                        myAdapter.notifyDataSetChanged();
                                        HideProgress();
                                    }
                                });
                            }
                        });
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel),null);

                builder.create().show();
            }
        });

        AddData();

    }

    private void AddData() {
        ShowProgress(this);

        sideIncomeLocation.orderBy(MyStatic.DATE_SIDE_BUSINESS_KEY, Query.Direction.DESCENDING).get().addOnSuccessListener(myListener);

    }

    public void FilterSideIncomeHistory(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customFilterView = layoutInflater.inflate(R.layout.custom_filter_for_side_business,null);

        AutoCompleteTextView RemarkFilter = customFilterView.findViewById(R.id.remark_filter);

        EditText MinAmountFilter = customFilterView.findViewById(R.id.min_amount_filter);
        EditText MaxAmountFilter = customFilterView.findViewById(R.id.max_amount_filter);

        RadioGroup DateFilter = customFilterView.findViewById(R.id.date_filter);
        RadioButton SingleDateFilter = customFilterView.findViewById(R.id.single_date_filter);
        RadioButton BetweenDateFilter = customFilterView.findViewById(R.id.between_date_filter);

        LinearLayout SingleDateContainer = customFilterView.findViewById(R.id.single_date_filter_container);
        Button SingleChooseDateFilter = customFilterView.findViewById(R.id.single_choose_date_filter);
        final TextView SingleShowDateFilter = customFilterView.findViewById(R.id.single_show_date_filter);

        final ArrayList<Boolean> singleChoose = new ArrayList<>();
        singleChoose.add(false);

        SingleChooseDateFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleChoose.add(0,ChooseDateDialogSingle(SideIncomeHistory.this,SingleShowDateFilter));
            }
        });

        LinearLayout BetweenDateContainer = customFilterView.findViewById(R.id.between_dates_cotainer);
        Button FromChooseDate = customFilterView.findViewById(R.id.from_choose_date_filter);
        final TextView FromShowDate = customFilterView.findViewById(R.id.from_show_date_filter);

        final ArrayList<Boolean> fromChoose = new ArrayList<>();
        fromChoose.add(false);

        FromChooseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromChoose.add(0,ChooseDateDialogFrom(SideIncomeHistory.this,FromShowDate));
            }
        });

        Button ToChooseDate = customFilterView.findViewById(R.id.to_choose_date_filter);
        final TextView ToShowDate = customFilterView.findViewById(R.id.to_show_date_filter);

        final ArrayList<Boolean> toChoose = new ArrayList<>();
        toChoose.add(false);

        ToChooseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toChoose.add(0,ChooseDateDialogTo(SideIncomeHistory.this,ToShowDate));
            }
        });


        Button FilterBt = customFilterView.findViewById(R.id.filter_bt);

        FilterBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        builder.setView(customFilterView);

        builder.create().show();
    }
    OnSuccessListener<QuerySnapshot> myListener = new OnSuccessListener<QuerySnapshot>() {
        @Override
        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
            SEList.clear();
            DataList.clear();
            for (DocumentSnapshot documentSnapshot: queryDocumentSnapshots){
                Map<String, Object> MapObj = new HashMap<>();
                MapObj = documentSnapshot.getData();
                assert MapObj != null;
                SingleExpOrInc Data = new SingleExpOrInc(MapObj.get(MyStatic.ID_SIDE_FOR_ALL).toString(),MapObj.get(MyStatic.REMARK_SIDE_FOR_ALL).toString(),(Double)MapObj.get(MyStatic.AMOUNT_SIDE_FOR_ALL),(Date) MapObj.get(MyStatic.DATE_SIDE_FOR_ALL));
                DataList.add(Data);
                ThreeColumnsList item = new ThreeColumnsList(DateToString(Data.getDate()),Data.getRemark(),String.valueOf(Data.getAmount()));
                SEList.add(item);
            }
            myAdapter.notifyDataSetChanged();
            HideProgress();
        }
    };
}