package com.vysh.subairoma.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import com.vysh.subairoma.adapters.FeedbackQuestionAdapter;
import com.vysh.subairoma.models.FeedbackQuestionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Vishal on 12/30/2017.
 */

public class ActivityFeedback extends AppCompatActivity {
    private final String saveAPI = "/savefeedbackresponse.php";

    RecyclerView rvFeedback;
    Button btnNext;
    String countryId, migName, countryName, status, blacklist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        rvFeedback = findViewById(R.id.rvFeedbackQuestions);
        btnNext = findViewById(R.id.btnNext);

        setUpMigrantCountryData(getIntent());
        setUpRecyclerView();
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getAllFeedbackResponses();
                openTileHomeActivity();

            }
        });
    }

    private void setUpMigrantCountryData(Intent intent) {
        countryId = intent.getStringExtra("countryId");
        migName = intent.getStringExtra("migrantName");
        countryName = intent.getStringExtra("countryName");
        status = intent.getStringExtra("countryStatus");
        blacklist = intent.getStringExtra("countryBlacklist");
    }

    private void openTileHomeActivity() {
        Intent intent = new Intent(ActivityFeedback.this, ActivityTileHome.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("countryId", countryId);
        intent.putExtra("migrantName", migName);
        intent.putExtra("countryName", countryName);
        intent.putExtra("countryStatus", status);
        intent.putExtra("countryBlacklist", blacklist);

        startActivity(intent);
    }

    private void setUpRecyclerView() {
        SQLDatabaseHelper helper = new SQLDatabaseHelper(this);
        ArrayList<FeedbackQuestionModel> questionModels = helper.getFeedbackQuestions();
        rvFeedback.setLayoutManager(new LinearLayoutManager(this));
        rvFeedback.setAdapter(new FeedbackQuestionAdapter(this, questionModels));
    }

    private void getAllFeedbackResponses() {
        int migId = ApplicationClass.getInstance().getMigrantId();
        SQLDatabaseHelper sqlDatabaseHelper = new SQLDatabaseHelper(ActivityFeedback.this);
        ArrayList<HashMap> responses = sqlDatabaseHelper.getAllFeedbackResponses(migId);

        RequestQueue queue = Volley.newRequestQueue(ActivityFeedback.this);
        for (int i = 0; i < responses.size(); i++) {
            saveResponseToServer(responses.get(i), queue);
        }
    }

    private void saveResponseToServer(final HashMap<String, String> fParams, RequestQueue queue) {
        String api = ApplicationClass.getInstance().getAPIROOT() + saveAPI;
        StringRequest stringRequest = new StringRequest(Request.Method.POST, api, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("mylog", "Response: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String err = error.toString();
                if (!err.isEmpty() && err.contains("NoConnection")) {
                    //showSnackbar("Response cannot be saved at the moment, please check your Intenet connection.");
                    Log.d("mylog", "Feedback couldn't be saved, please check your Intenet connection.");
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                for (Object key : fParams.keySet()) {
                    Log.d("mylog", key + ": " + fParams.get(key));
                }
                return fParams;
            }
        };

        queue.add(stringRequest);
    }
}
