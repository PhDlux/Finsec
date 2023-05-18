package com.example.finsec_finalfinalnajud;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensePage extends AppCompatActivity implements View.OnClickListener {

    TextView txtGoal;
    TextView txtCurrentSavings;
    TextView txtTotalCurrentSavings;
    DatabaseReference dbFinsec = FirebaseDatabase.getInstance().getReferenceFromUrl("https://finsec-14c51-default-rtdb.firebaseio.com/");
    String email3;
    LinearLayout layoutlist;
    Button btnAddSavings;
    AlertDialog addSavingsDialog;
    AlertDialog newGoalDialog;
    String goal, date;
    ProgressBar progressBar;
    HashMap<String, Integer> indexMap = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expense_page);

        Button back = (Button) findViewById(R.id.btnGoalSavingsBack);
        layoutlist = (LinearLayout) findViewById(R.id.layout_list);
        btnAddSavings = (Button) findViewById(R.id.btnAddSavings);
        Intent i = getIntent();
        email3 = i.getStringExtra("email2");
        date = i.getStringExtra("currentDate");

        txtCurrentSavings = findViewById(R.id.txtCurrentSavings);

        btnAddSavings.setOnClickListener(this);
        back.setOnClickListener(this);


        buildNewGoalDialog();
        buildAddSavingsDialog();
        loadData();

        int backgroundColor = Color.parseColor("#F1F1F1");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isColorDark(backgroundColor)) {
                    getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                } else {
                    getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        }
    }

    private void buildAddSavingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.another_customdialog, null);

        EditText etGoal = view.findViewById(R.id.etGoalName);
        EditText etSavingsAdded = view.findViewById(R.id.etSavingsAdded);
        builder.setView(view);
        builder.setTitle("Add Goal Savings")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        double expense = Double.parseDouble(etSavingsAdded.getText().toString());
                        try {
                            NumberFormat n = NumberFormat.getInstance(Locale.US);

                            double temp = n.parse(txtCurrentSavings.getText().toString().substring(1).replace(",", "")).doubleValue();
                            String num;
                            double goals;

                            if(goal==null) {
                                goal = txtGoal.getText().toString().substring(2);
                            }

                            Number x = n.parse(goal);
                            num = x.toString();
                            goals = Double.parseDouble(num);
                            int percent;
                            if(goals >= expense) {
                                if(goals == 0) {
                                    throw new IllegalArgumentException("Set the your goal-savings first!");
                                }
                                percent = (int) ((expense/goals) * 100);
                            } else {
                                throw new IllegalArgumentException("Goal max cap exceeded!");
                            }

                            if((temp + expense) > goals) {
                                throw new IllegalArgumentException("You have reached your goal. Set another one!");
                            }

                            DatabaseReference dateRef = dbFinsec.child("users").child(email3).child(date);

                            dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String expense = etSavingsAdded.getText().toString();
                                    String dbPercent = String.valueOf(percent);
                                    if(!snapshot.hasChild(etGoal.getText().toString())) {
                                        dateRef.child(etGoal.getText().toString());
                                    } else {
                                        String dbSavingsVal = snapshot.child(etGoal.getText().toString()).child("expense").getValue(String.class);
                                        String percentVal = snapshot.child(etGoal.getText().toString()).child("percent").getValue(String.class);
                                        int fs = Integer.parseInt(dbSavingsVal) + Integer.parseInt(expense);
                                        if(fs > goals) {
                                            throw new IllegalArgumentException("You have reached your goal. Set another one!");
                                        }
                                        int finalSavings = fs;
                                        expense = String.valueOf(finalSavings);
                                        int finalPercent = percent + Integer.parseInt(percentVal);
                                        dbPercent = String.valueOf(finalPercent);
                                    }
                                    dateRef.child(etGoal.getText().toString()).child("expense").setValue(expense);
                                    dateRef.child(etGoal.getText().toString()).child("percent").setValue(dbPercent);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            setTxtCurrentSavings(etGoal.getText().toString(),temp + expense);
                            addView(etGoal.getText().toString(), Double.parseDouble(etSavingsAdded.getText().toString()), percent);

                        }  catch (ParseException e) {
                            throw new RuntimeException(e);
                        } catch(IllegalArgumentException il) {
                            Toast.makeText(ExpensePage.this, il.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        addSavingsDialog = builder.create();
    }

    private void buildNewGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.custom_dialog, null);

        EditText etGoalSavings = view.findViewById(R.id.etGoalSavings);

        builder.setView(view);
        builder.setTitle("New Goal")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        NumberFormat n = NumberFormat.getInstance();
                        n.setMaximumFractionDigits(2);
                        n.setMinimumFractionDigits(2);

                        double goalValue = Double.parseDouble(etGoalSavings.getText().toString());


                        goal = String.valueOf((int)goalValue);
                        dbFinsec.child("users").child(email3).child("goal").setValue(goal);

                        setTxtGoal();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        newGoalDialog = builder.create();
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btnAddSavings:
                addSavingsDialog.show();
                break;
            case R.id.btnNewGoal:
                newGoalDialog.show();
                break;
            case R.id.btnGoalSavingsBack:
                Intent returnIntent = new Intent();
                returnIntent.putExtra("totalCurrentSavings", txtTotalCurrentSavings.getText().toString());
                setResult(Activity.RESULT_OK, returnIntent);

                finish();
                break;
        }
    }

    // Create a List to store the goal name order
    List<String> goalNameOrder = new ArrayList<>();

    public void addView(String goalname, double expense, int percent) {
        DatabaseReference userRef = dbFinsec.child("users").child(email3);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ExpensePage.GoalData goalData = new ExpensePage.GoalData(expense, percent);

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    // Skip if it is otherfields
                    if (date.equals("contactNumber") || date.equals("dateofbirth") || date.equals("firstname")
                            || date.equals("gender") || date.equals("goal") || date.equals("lastname")
                            || date.equals("password")) continue;

                    if (dateSnapshot.hasChild(goalname)) {
                        String dbExpense = dateSnapshot.child(goalname).child("expense").getValue(String.class);
                        String dbPercent = dateSnapshot.child(goalname).child("percent").getValue(String.class);

                        double dbSavingsValue = Double.parseDouble(dbExpense);
                        int dbPercentValue = Integer.parseInt(dbPercent);

                        goalData.expense += dbSavingsValue;
                        goalData.percent += dbPercentValue;
                    }
                }

                // Update the view with consolidated data
                updateView(goalname, goalData.expense, goalData.percent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public void updateView(String goalname, double savings, int percent) {
        View view = getLayoutInflater().inflate(R.layout.add_expense, null);

        TextView txtSavingsAdded = view.findViewById(R.id.txtSavingsAdded);
        TextView txtGoalName = view.findViewById(R.id.txtGoalName);

        NumberFormat n = NumberFormat.getInstance();
        n.setMaximumFractionDigits(2);
        n.setMinimumFractionDigits(2);

        txtGoalName.setText(goalname);
        txtSavingsAdded.setText("₱ " + n.format(savings));

        // if goal already exists, remove the old view
        if (goalNameOrder.contains(goalname)) {
            int index = goalNameOrder.indexOf(goalname);
            layoutlist.removeViewAt(index);
            goalNameOrder.remove(index);
        }

        // Add the new view at index 0 and update goalNameOrder
        layoutlist.addView(view, 0);
        goalNameOrder.add(0, goalname);
    }

    private void removeAllViewsExceptLast() {
        int childCount = layoutlist.getChildCount();

        // leave the last child (Button)
        if (childCount > 1) {
            layoutlist.removeViews(0, childCount - 1);
        }
    }

    public void loadData() {

        setTxtGoal();
        DatabaseReference userRef = dbFinsec.child("users").child(email3);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                removeAllViewsExceptLast();

                // A map to store the consolidated data
                Map<String, ExpensePage.GoalData> consolidatedData = new HashMap<>();

                // Variables to store the total savings
                double totalSavings = 0;

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey();

                    // Skip if it is otherfields
                    if (date.equals("contactNumber") || date.equals("dateofbirth") || date.equals("firstname")
                            || date.equals("gender") || date.equals("goal") || date.equals("lastname")
                            || date.equals("password")) continue;

                    for (DataSnapshot goalSnapshot : dateSnapshot.getChildren()) {
                        String goalName = goalSnapshot.getKey();

                        // Check for null values
                        if (goalSnapshot.child("savings").getValue() != null && goalSnapshot.child("percent").getValue() != null) {
                            String savingsStr = goalSnapshot.child("savings").getValue(String.class);
                            String percentStr = goalSnapshot.child("percent").getValue(String.class);

                            double savings = 0;
                            int percent = 0;

                            try {
                                savings = Double.parseDouble(savingsStr);
                                percent = Integer.parseInt(percentStr);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Failed to parse savings or percent for goal " + goalName);
                            }

                            // Consolidate data
                            if (consolidatedData.containsKey(goalName)) {
                                ExpensePage.GoalData existingGoalData = consolidatedData.get(goalName);
                                existingGoalData.expense += savings;
                                existingGoalData.percent += percent;
                            } else {
                                consolidatedData.put(goalName, new ExpensePage.GoalData(savings, percent));
                            }


                        } else {
                            Log.e(TAG, "Null value for savings or percent for goal " + goalName);
                        }
                    }
                }

                // Update views with consolidated data
                for (Map.Entry<String, ExpensePage.GoalData> entry : consolidatedData.entrySet()) {
                    updateView(entry.getKey(), entry.getValue().expense, entry.getValue().percent);
                }

                NumberFormat n = NumberFormat.getInstance();
                n.setMaximumFractionDigits(2);
                n.setMinimumFractionDigits(2);

                txtCurrentSavings.setText("₱ " + n.format(totalSavings));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public static class GoalData {
        public double expense;
        public int percent;

        public GoalData(double expense, int percent) {
            this.expense = expense;
            this.percent = percent;
        }
    }



    public void setTxtGoal() {
        NumberFormat n = NumberFormat.getInstance();
        n.setMaximumFractionDigits(2);
        n.setMinimumFractionDigits(2);
        dbFinsec.child("users").child(email3).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.hasChild("goal")) {
                    goal = snapshot.child("goal").getValue(String.class);
                    double numValue = Double.parseDouble(goal);

                } else {
                    System.out.println("LOL its zero");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void setTxtCurrentSavings(String name, double expense) {
        NumberFormat n = NumberFormat.getInstance();
        n.setMaximumFractionDigits(2);
        n.setMinimumFractionDigits(2);

        DatabaseReference dbSavingsRef = dbFinsec.child("users").child(email3).child(date).child(name);
        dbSavingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

//                if(snapshot.hasChild())
                txtCurrentSavings.setText("₱ " + n.format(expense));


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
}