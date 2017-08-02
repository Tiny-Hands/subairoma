package com.vysh.subairoma;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vysh.subairoma.SQLHelpers.SQLDatabaseHelper;
import com.vysh.subairoma.adapters.TileAdapter;
import com.vysh.subairoma.models.TilesModel;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Vishal on 8/2/2017.
 */

public class ActivityGasSection extends AppCompatActivity {
    ArrayList<TilesModel> tiles;

    int[] tileIcons;

    @BindView(R.id.rvTiles)
    RecyclerView rvTiles;
    @BindView(R.id.rlRoot)
    RelativeLayout rootLayout;
    @BindView(R.id.tvMigrantName)
    TextView tvMigrantName;
    @BindView(R.id.tvCountry)
    TextView tvCountry;
    @BindView(R.id.btnNext)
    Button btnNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile_home);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ButterKnife.bind(this);

        String cid = getIntent().getStringExtra("countryId");
        if (cid.equalsIgnoreCase("in")) {
            //GET GIS TILES
            tiles = new SQLDatabaseHelper(ActivityGasSection.this).getTiles("GAS");
        } else {
            //GET FEP TILES
            tiles = new SQLDatabaseHelper(ActivityGasSection.this).getTiles("GAS");
        }
        btnNext.setText("Complete");
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ActivityGasSection.this, "The Process is Completed", Toast.LENGTH_SHORT).show();
            }
        });
        String migrantName = getIntent().getStringExtra("migrantName").toUpperCase();
        tvMigrantName.setText(migrantName);
        tvCountry.setText(getIntent().getStringExtra("countryName"));
        int status = getIntent().getIntExtra("countryStatus", -1);
        int blacklist = getIntent().getIntExtra("countryBlacklist", -1);
        if (status == 1) {
            tvCountry.setTextColor(getResources().getColor(R.color.colorNeutral));
        }
        if (blacklist == 1) {
            tvCountry.setTextColor(getResources().getColor(R.color.colorError));
        }
        setTileIcons();
        setUpRecyclerView();
    }

    private void setTileIcons() {
        int size = tiles.size();
        tileIcons = new int[size];
        for (int i = 0; i < size; i++)
            tileIcons[i] = R.drawable.roller;
    }

    private void showSnackbar(String msg) {
        Snackbar snack = Snackbar.make(rootLayout, msg, Snackbar.LENGTH_LONG);
        View view = snack.getView();
        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        snack.show();
    }

    private void setUpRecyclerView() {
        rvTiles.setLayoutManager(new GridLayoutManager(this, 2));
        rvTiles.setAdapter(new TileAdapter(tiles, tileIcons));
    }
}