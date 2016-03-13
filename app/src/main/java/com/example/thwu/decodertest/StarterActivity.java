package com.example.thwu.decodertest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class StarterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(StarterActivity.this);
                builder.setTitle("Choose your image processing platform!");
                builder.setMessage("Pick desired your way");

                builder.setPositiveButton("Phone Processing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent home = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(home);
                        finish();
                    }
                });

                builder.setNegativeButton("PC Processing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent home = new Intent(getApplicationContext(), Main2Activity.class);
                        startActivity(home);
                        finish();
                    }
                });
                AlertDialog alertdialog = builder.create();
                alertdialog.show();
            }
        }, 1000);
    }
}
