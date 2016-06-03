package com.industries.sarker.uber;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button startButton;
    private Switch riderOrDriverSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        riderOrDriverSwitch = (Switch) findViewById(R.id.riderOrDriverSwitch);

        startButton = (Button) findViewById(R.id.startButton);

        startButton.setOnClickListener(this);

        if (ParseUser.getCurrentUser() == null) {

            ParseAnonymousUtils.logInInBackground();

        } else {
            if (ParseUser.getCurrentUser().get("riderOrDriver") != null) {
                redirectUser();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                String riderOrDriver = "rider";

                if (riderOrDriverSwitch.isChecked()) {
                    riderOrDriver = "driver";
                }

                ParseUser.getCurrentUser().put("riderOrDriver", riderOrDriver);
                ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            redirectUser();
                        }
                    }
                });
        }
    }

    public void redirectUser() {

        if (ParseUser.getCurrentUser().get("riderOrDriver").equals("rider")) {
            Intent intent = new Intent(MainActivity.this, YourLocation.class);
            startActivity(intent);

        } else {
            Intent intent = new Intent(MainActivity.this, ViewRequests.class);
            startActivity(intent);
        }
    }
}
