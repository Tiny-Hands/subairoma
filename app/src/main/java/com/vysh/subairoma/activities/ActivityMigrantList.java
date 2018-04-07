package com.vysh.subairoma.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vysh.subairoma.ApplicationClass;
import com.vysh.subairoma.R;
import com.vysh.subairoma.SQLHelpers.SQLDatabaseHelper;
import com.vysh.subairoma.SharedPrefKeys;
import com.vysh.subairoma.adapters.MigrantListAdapter;
import com.vysh.subairoma.adapters.RecyclerItemTouchHelper;
import com.vysh.subairoma.dialogs.DialogCountryChooser;
import com.vysh.subairoma.models.CountryModel;
import com.vysh.subairoma.models.MigrantModel;
import com.vysh.subairoma.services.LocationChecker;
import com.vysh.subairoma.utils.CustomTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Vishal on 6/16/2017.
 */

public class ActivityMigrantList extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    private final String API = "/getmigrants.php";
    private final String ApiDISABLE = "/deactivatemigrant.php";
    private final int REQUEST_LOCATION = 1;

    @BindView(R.id.rvMigrants)
    RecyclerView recyclerView;
    @BindView(R.id.btnAddMigrant)
    Button btnAddMigrant;
    @BindView(R.id.rlRoot)
    RelativeLayout rootLayout;
    @BindView(R.id.ivHam)
    ImageView ivHam;
    @BindView(R.id.drawerLayout)
    DrawerLayout drawerLayout;
    NavigationView navView;

    CustomTextView tvName, tvPhone, tvNavCounty;
    ImageView ivUserAvatar;

    int userType;
    ArrayList<MigrantModel> migrantModels;
    MigrantListAdapter migrantListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_migrant);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ButterKnife.bind(this);
        userType = ApplicationClass.getInstance().getUserId();
        migrantModels = new ArrayList();
        getMigrants();
        if (isLocationAccessAllowed())
            getUpdatedMigrantCounties();
        else
            requestLocationAccess();
        //setUpRecyclerView(null);
        if (userType == -1)
            btnAddMigrant.setVisibility(View.GONE);
        btnAddMigrant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityMigrantList.this, ActivityRegister.class);
                intent.putExtra("migrantmode", true);
                startActivity(intent);
            }
        });

        navView = findViewById(R.id.nav_view);
        setUpNavigationButtons();
        setUpListeners();
    }

    private void setUpListeners() {
        ivHam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_profile:
                        Intent intent = new Intent(ActivityMigrantList.this, ActivityProfileEdit.class);
                        intent.putExtra("userType", 0);
                        startActivity(intent);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSavedMigrants();
    }

    private void setUpNavigationButtons() {
        View view = navView.getHeaderView(0);
        if (view == null)
            Log.d("mylog", "Header view Null");
        if (navView == null)
            Log.d("mylog", "Nav View Null");
        tvName = view.findViewById(R.id.tvMName);
        tvNavCounty = view.findViewById(R.id.tvMCountry);
        tvPhone = view.findViewById(R.id.tvMPhone);
        ivUserAvatar = view.findViewById(R.id.ivUserAva);


        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefKeys.sharedPrefName, MODE_PRIVATE);
        tvPhone.setText(sharedPreferences.getString(SharedPrefKeys.userPhone, ""));
        tvName.setText(sharedPreferences.getString(SharedPrefKeys.userName, ""));
        String sex = sharedPreferences.getString(SharedPrefKeys.userSex, "");
        String age = sharedPreferences.getString(SharedPrefKeys.userAge, "");
        tvNavCounty.setText("Age: " + age);

        if (sex != null) {
            if (sex.equals("male"))
                ivUserAvatar.setImageResource(R.drawable.ic_male);
            else
                ivUserAvatar.setImageResource(R.drawable.ic_female);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    getUpdatedMigrantCounties();
                break;
        }
    }

    private void requestLocationAccess() {
        if (Build.VERSION.SDK_INT > 22)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
    }

    private boolean isLocationAccessAllowed() {
        if (Build.VERSION.SDK_INT >= 22) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void getUpdatedMigrantCounties() {
        Intent intent = new Intent(ActivityMigrantList.this, LocationChecker.class);
        startService(intent);
    }

    private void getSavedMigrants() {
        SQLDatabaseHelper dbHelper = new SQLDatabaseHelper(ActivityMigrantList.this);
        migrantModels = getMigrantsToDisplay(dbHelper.getMigrants());
        migrantListAdapter = new MigrantListAdapter();
        if (userType != -1) {
            Log.d("mylog", "Usertype: " + userType);
            setUpRecyclerView(migrantModels);
        } else {
            if (migrantModels.size() > 0) {
                int migId = migrantModels.get(0).getMigrantId();
                ApplicationClass.getInstance().setMigrantId(migId);
                String cid = new SQLDatabaseHelper(ActivityMigrantList.this).getResponse(migId, "mg_destination");
                Log.d("mylog", "Country ID: " + cid);
                if (cid != null && !cid.isEmpty()) {
                    Intent intent = new Intent(ActivityMigrantList.this, ActivityTileHome.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("countryId", cid);
                    intent.putExtra("migrantName", migrantModels.get(0).getMigrantName());
                    intent.putExtra("migrantPhone", migrantModels.get(0).getMigrantPhone());
                    intent.putExtra("migrantGender", migrantModels.get(0).getMigrantSex());
                    CountryModel savedCountry = new SQLDatabaseHelper(ActivityMigrantList.this).getCountry(cid);
                    Log.d("mylog", "Country name: " + savedCountry.getCountryName());
                    intent.putExtra("countryName", savedCountry.getCountryName().toUpperCase());
                    intent.putExtra("countryStatus", savedCountry.getCountrySatus());
                    intent.putExtra("countryBlacklist", savedCountry.getCountryBlacklist());
                    startActivity(intent);
                } else {
                    DialogCountryChooser dialog = DialogCountryChooser.newInstance();
                    dialog.setMigrantName(migrantModels.get(0).getMigrantName());
                    dialog.setCancelable(false);
                    Log.d("mylog", "Migrant name: " + migrantModels.get(0).getMigrantName() + " : " + migrantModels.get(0).getMigrantId());
                    dialog.show(getSupportFragmentManager(), "countrychooser");
                    recyclerView.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private ArrayList<MigrantModel> getMigrantsToDisplay(ArrayList<MigrantModel> migs) {
        ArrayList<MigrantModel> tempMig = new ArrayList<>();
        ArrayList<MigrantModel> disableMigs = new ArrayList<>();
        for (int i = 0; i < migs.size(); i++) {
            if (migs.get(i).getInactiveDate() == null || migs.get(i).getInactiveDate().isEmpty() || (migs.get(i).getInactiveDate().length() < 5)) {
                tempMig.add(migs.get(i));
            } else {
                disableMigs.add(migs.get(i));
            }
        }
        sendDeactivationStatus(disableMigs);
        return tempMig;
    }

    private void getMigrants() {
        String api = ApplicationClass.getInstance().getAPIROOT() + API;
        final ProgressDialog progressDialog = new ProgressDialog(ActivityMigrantList.this);
        progressDialog.setMessage(getResources().getString(R.string.getting_mig_details));
        try {
            progressDialog.show();
        } catch (Exception ex) {
            Log.d("mylog", "Exception in progress list: " + ex.getMessage());
        }
        StringRequest getRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ex) {
                    Log.d("mylog", "Exception in progress dis: " + ex.getMessage());
                }
                try {
                    boolean firstRun = false;
                    migrantModels = getMigrantsToDisplay(parseResponse(response));
                    if (migrantModels == null || migrantModels.size() < 1) firstRun = true;
                    //migrantListAdapter.notifyDataSetChanged();
                    if (userType == -1 && firstRun) {
                        DialogCountryChooser dialog = DialogCountryChooser.newInstance();
                        dialog.setMigrantName(migrantModels.get(0).getMigrantName());
                        Log.d("mylog", "Migrant name: " + migrantModels.get(0).getMigrantName() + " : " + migrantModels.get(0).getMigrantId());
                        dialog.show(getSupportFragmentManager(), "countrychooser");
                        recyclerView.setVisibility(View.INVISIBLE);
                    } else {
                        setUpRecyclerView(migrantModels);
                    }
                    Log.d("mylog", "response : " + response);
                } catch (Exception ex) {
                    Log.d("mylog", "response exception: " + ex.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ex) {
                    Log.d("mylog", "Exception in progress dis error: " + ex.getMessage());
                }
                try {
                    String err = error.toString();
                    Log.d("mylog", "error : " + err);
                    if (!err.isEmpty() && err.contains("TimeoutError"))
                        Toast.makeText(ActivityMigrantList.this, getResources().getString(R.string.server_noconnect), Toast.LENGTH_SHORT).show();
                    else if (!err.isEmpty() && err.contains("NoConnection")) {
                        Toast.makeText(ActivityMigrantList.this, getResources().getString(R.string.server_noconnect), Toast.LENGTH_SHORT).show();
                    } else
                        showSnackbar(error.toString());
                } catch (Exception ex) {
                    Log.d("mylog", "Error exception: " + ex.toString());
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                int user_id = ApplicationClass.getInstance().getUserId();
                int mig_id = ApplicationClass.getInstance().getMigrantId();
                Log.d("mylog", "User ID: " + user_id);
                Log.d("mylog", "Mig ID: " + mig_id);
                params.put("user_id", user_id + "");
                params.put("migrant_id", mig_id + "");
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(ActivityMigrantList.this);
        queue.add(getRequest);
    }

    private ArrayList<MigrantModel> parseResponse(String response) {
        ArrayList<MigrantModel> migrantModelsTemp = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            Boolean error = jsonObject.getBoolean("error");
            if (error) {
                showSnackbar(jsonObject.getString("message"));
            } else {
                JSONArray migrantJSON = jsonObject.getJSONArray("migrants");
                if (migrantJSON != null) {
                    JSONObject migrantObj;
                    SQLDatabaseHelper dbHelper = new SQLDatabaseHelper(ActivityMigrantList.this);
                    int uid = ApplicationClass.getInstance().getUserId();
                    for (int i = 0; i < migrantJSON.length(); i++) {
                        migrantObj = migrantJSON.getJSONObject(i);
                        MigrantModel migrantModel = new MigrantModel();
                        if (migrantObj.has("migrant_id")) {
                            int id = migrantObj.getInt("migrant_id");
                            migrantModel.setMigrantId(id);
                            String name = migrantObj.getString("migrant_name");
                            migrantModel.setMigrantName(name);
                            int age = migrantObj.getInt("migrant_age");
                            migrantModel.setMigrantAge(age);
                            String sex = migrantObj.getString("migrant_sex");
                            migrantModel.setMigrantSex(sex);
                            String phone = migrantObj.getString("migrant_phone");
                            migrantModel.setMigrantPhone(phone);
                            String inactiveDate = migrantObj.getString("inactive_date");
                            migrantModel.setInactiveDate(inactiveDate);
                            migrantModel.setUserId(uid);

                            migrantModelsTemp.add(migrantModel);
                            //Saving in Database
                            dbHelper.insertMigrants(id, name, age, phone, sex, uid);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.d("mylog", "Error in parsing: " + e.toString());
        }
        return migrantModelsTemp;
    }

    private void showSnackbar(String msg) {
        Snackbar snack = Snackbar.make(rootLayout, msg, Snackbar.LENGTH_LONG);
        View view = snack.getView();
        TextView tv = view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 17)
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }

    private void setUpRecyclerView(ArrayList<MigrantModel> migrantModels) {
        recyclerView.setLayoutManager(new LinearLayoutManager(ActivityMigrantList.this));
        Log.d("mylog", "Number of migrants: " + migrantModels.size());
        migrantListAdapter.setMigrants(migrantModels);
        recyclerView.setAdapter(migrantListAdapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof MigrantListAdapter.MigrantHolder) {
            // get the removed item name to display it in snack bar
            String name = migrantModels.get(viewHolder.getAdapterPosition()).getMigrantName();

            // backup of removed item for undo purpose
            //final ClipData.Item deletedItem = cartList.get(viewHolder.getAdapterPosition());
            //final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view

            int uid = ApplicationClass.getInstance().getUserId();
            int mid = migrantModels.get(viewHolder.getAdapterPosition()).getMigrantId();
            long time = System.currentTimeMillis();
            SQLDatabaseHelper dbHelper = new SQLDatabaseHelper(ActivityMigrantList.this);
            dbHelper.insertMigrantDeletion(mid, uid, System.currentTimeMillis());
            migrantListAdapter.removeItem(viewHolder.getAdapterPosition());
            deactivateUser(uid, mid, time + "", null);

            // showing snack bar with Undo option
            /*Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, name + " removed from cart!", Snackbar.LENGTH_LONG);
            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // undo is selected, restore the deleted item
                    mAdapter.restoreItem(deletedItem, deletedIndex);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();*/
        }
    }

    public void sendDeactivationStatus(ArrayList<MigrantModel> disableMigs) {
        int uid = ApplicationClass.getInstance().getUserId();
        RequestQueue queue = Volley.newRequestQueue(ActivityMigrantList.this);
        for (int i = 0; i < disableMigs.size(); i++) {
            deactivateUser(uid, disableMigs.get(i).getMigrantId(), disableMigs.get(i).getInactiveDate(), queue);
        }
    }

    public void deactivateUser(final int uid, final int migId, final String deactivateTime, RequestQueue queue) {
        if (queue == null)
            queue = Volley.newRequestQueue(ActivityMigrantList.this);
        String api = ApplicationClass.getInstance().getAPIROOT() + ApiDISABLE;
        StringRequest getRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("mylog", "deactivation response : " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("mylog", "Error exception: " + error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                int user_id = ApplicationClass.getInstance().getUserId();
                int mig_id = ApplicationClass.getInstance().getMigrantId();
                params.put("user_id", uid + "");
                params.put("migrant_id", migId + "");
                params.put("inactive_date", deactivateTime);
                return params;
            }
        };
        queue.add(getRequest);
    }
}
