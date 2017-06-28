package com.vysh.subairoma;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.vysh.subairoma.utils.CustomTextView;
import com.vysh.subairoma.volley.VolleyController;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Vishal on 6/15/2017.
 */

public class ActivityRegister extends AppCompatActivity {
    final String apiURL = "http://192.168.184.1/subairoma/saveuser.php";
    final String apiURLMigrant = "http://192.168.184.1/subairoma/migrant.php";

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

    Boolean userRegistered = false;
    String sex;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void saveUser() {
        String api = apiURL;
        if (userRegistered) {
            api = apiURLMigrant;
            startTilesActivity();
            return;
        }
        final ProgressDialog progressDialog = new ProgressDialog(ActivityRegister.this);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Registering User");
        progressDialog.show();
        StringRequest saveRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressDialog.dismiss();
                Log.d("mylog", "response : " + response.toString());
                showSnackbar(response.toString());
                if (!userRegistered) {
                    userRegistered = true;
                    loadMigrantView();
                } else {
                    startTilesActivity();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();
                Log.d("mylog", "error : " + error.toString());
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
                return params;
            }
        };
        VolleyController.getInstance(getApplicationContext()).addToRequestQueue(saveRequest);
    }

    private boolean validateData() {
        if (etName.getText().toString().isEmpty() || etName.getText().toString().length() < 5) {
            etName.setError("Name must be more than 5 characters long");
            return false;
        }
        if (etAge.getText().toString().isEmpty() || etAge.getText().toString().length() != 2) {
            etAge.setError("Age must be between 18 - 60");
            return false;
        }
        if (Integer.parseInt(etAge.getText().toString()) < 18 || Integer.parseInt(etAge.getText().toString()) > 60) {
            etAge.setError("Age must be between 18 - 60");
            return false;
        }
        if (etNumber.getText().toString().isEmpty() || etNumber.getText().toString().length() < 10) {
            etNumber.setError("Please enter a valid mobile number");
            return false;
        }
        return true;
    }

    private void startTilesActivity() {
        Intent intent = new Intent(ActivityRegister.this, ActivityOTPVerification.class);
        startActivity(intent);
    }

    private void loadMigrantView() {
        tvHint.setText("Please enter Migrant's details");
        tvTitle.setText("ADD MIGRANT");
        etName.setHint("Migrant's Name");
        etAge.setHint("Migrant's Age");
        etNumber.setHint("Migrant's Phone Number");
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
