package com.vysh.subairoma.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vysh.subairoma.ApplicationClass;
import com.vysh.subairoma.R;
import com.vysh.subairoma.utils.CustomTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Vishal on 6/15/2017.
 */

public class ActivityRegister extends AppCompatActivity {
    final String apiURL = "/saveuser.php";
    final String apiURLMigrant = "/savemigrant.php";
    final String apiAlreadyRegistered = "/checkphonenumber.php";
    int userType;

    @BindView(R.id.btnNext)
    Button btnNext;
    @BindView(R.id.tvTitle)
    CustomTextView tvTitle;
    @BindView(R.id.etName)
    EditText etName;
    @BindView(R.id.etAge)
    EditText etAge;
    @BindView(R.id.etNumber)
    EditText etNumber;
    @BindView(R.id.tvHint)
    TextView tvHint;
    @BindView(R.id.rbMale)
    RadioButton rbMale;
    @BindView(R.id.rbFemale)
    RadioButton rbFemale;
    @BindView(R.id.rlRoot)
    RelativeLayout rootLayout;
    @BindView(R.id.btnAlreadyRegistered)
    Button btnAlreadyRegistered;

    Boolean userRegistered = false;
    String sex = "male";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Checking if already logged in on current device

        if (checkUserExists() && !getIntent().hasExtra("migrantmode")) {
            Intent intent = new Intent(ActivityRegister.this, ActivityMigrantList.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        setContentView(R.layout.activity_register);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ButterKnife.bind(this);

        rbFemale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbMale.setChecked(false);
                sex = "female";
            }
        });
        rbMale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbFemale.setChecked(false);
                sex = "male";
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateData()) {
                    saveUser();
                }
            }
        });
        btnAlreadyRegistered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegistrationDialog();
            }
        });
        if (getIntent().hasExtra("migrantmode")) {
            userRegistered = true;
            Log.d("mylog", "Loading migrant view");
            loadMigrantView();
        }
    }

    private boolean checkUserExists() {
        SharedPreferences sharedPreferences = getSharedPreferences(ApplicationClass.getInstance().getSHAREDPREFSNAME(), MODE_PRIVATE);
        int userId = sharedPreferences.getInt("id", -10);
        String type = sharedPreferences.getString("type", "");
        Log.d("mylog", "User id: " + userId + " Type: " + type);
        if (userId != -10) {
            if (type.equalsIgnoreCase("helper"))
                ApplicationClass.getInstance().setUserId(userId);
            else {
                ApplicationClass.getInstance().setMigrantId(userId);
                ApplicationClass.getInstance().setUserId(-1);
            }
            return true;
        } else return false;
    }

    private void showRegistrationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.edittext_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText etRNumber = (EditText) dialogView.findViewById(R.id.etInput);
        etRNumber.setHint("Phone number");
        dialogBuilder.setTitle("Login");
        dialogBuilder.setMessage("Enter the number that you registered");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String number = etRNumber.getText().toString();
                if (!number.isEmpty() && number.length() == 10) {
                    checkUserRegistration(number);
                } else {
                    etNumber.setError("Please enter a valid number");
                }
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void checkUserRegistration(String number) {
        final String pNumber = number;
        String api = ApplicationClass.getInstance().getAPIROOT() + apiAlreadyRegistered;
        final ProgressDialog progressDialog = new ProgressDialog(ActivityRegister.this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Checking Registration...");
        progressDialog.show();
        StringRequest checkRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                Log.d("mylog", "response : " + response);
                try {
                    JSONObject jsonRes = new JSONObject(response);
                    boolean error = jsonRes.getBoolean("error");
                    if (error) {
                        showSnackbar(jsonRes.getString("message"));
                    } else {
                        ApplicationClass.getInstance().setUserId(jsonRes.getInt("user_id"));
                        startOTPActivity();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("mylog", "Error in getting user_id: " + e.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                String err = error.toString();
                Log.d("mylog", "error : " + err);
                if (!err.isEmpty() && err.contains("TimeoutError"))
                    showSnackbar("Failed to connect to server :(");
                else
                    showSnackbar(error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                params.put("number", pNumber);
                return params;
            }
        };
        RequestQueue queue = Volley.newRequestQueue(ActivityRegister.this);
        checkRequest.setShouldCache(false);
        queue.add(checkRequest);
    }

    private void saveUser() {
        if (!userRegistered) {
            userType = 0;
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityRegister.this);
            builder.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String api;
                    if (userType == 0) {
                        api = ApplicationClass.getInstance().getAPIROOT() + apiURLMigrant;
                    } else {
                        api = ApplicationClass.getInstance().getAPIROOT() + apiURL;
                    }
                    Log.d("mylog", "API called: " + api);
                    final ProgressDialog progressDialog = new ProgressDialog(ActivityRegister.this);
                    progressDialog.setTitle("Please wait");
                    progressDialog.setMessage("Registering...");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    StringRequest saveRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            progressDialog.dismiss();
                            Log.d("mylog", "response : " + response);
                            parseResponse(response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                            String err = error.toString();
                            Log.d("mylog", "error : " + err);
                            if (!err.isEmpty() && err.contains("TimeoutError"))
                                showSnackbar("Failed to connect to server :(");
                            else
                                showSnackbar(error.toString());
                        }
                    }) {
                        @Override
                        protected Map<String, String> getParams() throws AuthFailureError {
                            HashMap<String, String> params = new HashMap<>();
                            params.put("full_name", etName.getText().toString());
                            params.put("phone_number", etNumber.getText().toString());
                            params.put("age", etAge.getText().toString());
                            params.put("gender", sex);
                            if (userType == 0) {
                                params.put("user_id", "-1");
                            } else if (userRegistered)
                                params.put("user_id", ApplicationClass.getInstance().getUserId() + "");
                            return params;
                        }
                    };
                    RequestQueue queue = Volley.newRequestQueue(ActivityRegister.this);
                    saveRequest.setShouldCache(false);
                    queue.add(saveRequest);
                }
            });
            builder.setSingleChoiceItems(new String[]{"I am an aspiring Migrant", "I am helping others"}, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d("mylog", "Which: " + which);
                    userType = which;
                }
            });
            builder.show();
        } else {
            String api = ApplicationClass.getInstance().getAPIROOT() + apiURLMigrant;
            final ProgressDialog progressDialog = new ProgressDialog(ActivityRegister.this);
            progressDialog.setTitle("Please wait");
            progressDialog.setMessage("Registering...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            StringRequest saveRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    progressDialog.dismiss();
                    Log.d("mylog", "response : " + response);
                    parseResponse(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    progressDialog.dismiss();
                    String err = error.toString();
                    Log.d("mylog", "error : " + err);
                    if (!err.isEmpty() && err.contains("TimeoutError"))
                        showSnackbar("Failed to connect to server :(");
                    else
                        showSnackbar(error.toString());
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("full_name", etName.getText().toString());
                    params.put("phone_number", etNumber.getText().toString());
                    params.put("age", etAge.getText().toString());
                    params.put("gender", sex);
                    if (userRegistered) {
                        params.put("user_id", ApplicationClass.getInstance().getUserId() + "");
                        Log.d("mylog", "User ID: " + ApplicationClass.getInstance().getUserId());
                    }
                    else if (userType == 0) {
                        params.put("user_id", "-1");
                        Log.d("mylog", "User ID: " + -1);
                    }
                    return params;
                }
            };
            RequestQueue queue = Volley.newRequestQueue(ActivityRegister.this);
            saveRequest.setShouldCache(false);
            queue.add(saveRequest);
        }
    }

    private void parseResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            Boolean error = jsonResponse.getBoolean("error");
            if (!error) {
                //Means the registered person was user
                if (!userRegistered) {
                    if (userType == 0) {
                        ApplicationClass.getInstance().setUserId(-1);
                        int mig_id = jsonResponse.getInt("migrant_id");
                        Log.d("mylog", "Migrant ID: " + mig_id);
                        ApplicationClass.getInstance().setMigrantId(mig_id);
                        startOTPActivity();
                        return;
                    } else {
                        int user_id = jsonResponse.getInt("user_id");
                        ApplicationClass.getInstance().setUserId(user_id);
                    }
                    //Save to application class
                    userRegistered = true;
                    startOTPActivity();
                    return;
                    //loadMigrantView();
                }
                //Means the registered person was migrant
                else {
                    //startOTPActivity();
                    Intent intent = new Intent(ActivityRegister.this, ActivityMigrantList.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            } else {
                String message = jsonResponse.getString("message");
                showSnackbar(message);
            }
        } catch (JSONException e) {
            Log.d("mylog", "Error in parsing: " + e.getMessage());
        }
    }

    private boolean validateData() {
        if (etName.getText().toString().isEmpty() || etName.getText().toString().length() < 5) {
            etName.setError("Name must be more than 5 characters long");
            return false;
        }
        if (etAge.getText().toString().isEmpty() || etAge.getText().toString().length() != 2) {
            etAge.setError("Age must be between 12 - 90");
            return false;
        }
        if (Integer.parseInt(etAge.getText().toString()) < 12 || Integer.parseInt(etAge.getText().toString()) > 90) {
            etAge.setError("Age must be between 12 - 90");
            return false;
        }
        if (etNumber.getText().toString().isEmpty() || etNumber.getText().toString().length() < 10) {
            etNumber.setError("Please enter a valid mobile number");
            return false;
        }
        return true;
    }

    private void startOTPActivity() {
        String type;
        if (ApplicationClass.getInstance().getUserId() != -1) {
            type = "helper";
        } else type = "migrant";
        SharedPreferences sharedPreferences = getSharedPreferences(ApplicationClass.getInstance().getSHAREDPREFSNAME(), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("id", ApplicationClass.getInstance().getUserId());
        editor.putString("type", type);
        editor.commit();
        Intent intent = new Intent(ActivityRegister.this, ActivityOTPVerification.class);
        startActivity(intent);
    }

    private void loadMigrantView() {
        etNumber.setText("");
        etAge.setText("");
        etName.setText("");
        tvHint.setText("Please enter Migrant's details");
        tvTitle.setText("ADD MIGRANT");
        etName.setHint("Migrant's Name");
        etAge.setHint("Migrant's Age");
        etNumber.setHint("Migrant's Phone Number");
        btnAlreadyRegistered.setVisibility(View.INVISIBLE);
    }

    private void showSnackbar(String msg) {
        Snackbar snack = Snackbar.make(rootLayout, msg, Snackbar.LENGTH_LONG);
        View view = snack.getView();
        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 17)
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }
}
