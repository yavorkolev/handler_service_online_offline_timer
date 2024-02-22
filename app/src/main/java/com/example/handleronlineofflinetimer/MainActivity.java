package com.example.handleronlineofflinetimer;

import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {
    TextView timerTv;
    Button startBtn, stopBtn;
    private Handler customHandlerNetworkChecker = new Handler();
    boolean isTimerStartedFromButton = false;
    boolean isTimerStoppedFromNoConnection = false;
    boolean isConnected = false;
    TimerReceiver timerReceiver;
    ForegroundServiceTimer foregroundServiceTimer;
    Intent serviceIntent;
    Thread tmpThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerTv = findViewById(R.id.timerTv);

        timerReceiver = new TimerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ForegroundServiceTimer.MY_ACTION);
        registerReceiver(timerReceiver, intentFilter);

        startBtn = findViewById(R.id.startBtn);
        startBtn.setOnClickListener(this);

        stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(this);

        customHandlerNetworkChecker.postDelayed(updateNetworkCheckerThread, 0);
    }

    private class TimerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String dataPassed = arg1.getStringExtra("DATA_PASSED");
            timerTv.setText(dataPassed);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startBtn) {
            serviceIntent = new Intent(this, ForegroundServiceTimer.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            isTimerStartedFromButton = true;
        } else if (v.getId() == R.id.stopBtn) {
            stopService(serviceIntent);
            isTimerStartedFromButton = false;
        }
    }

    private Runnable updateNetworkCheckerThread = new Runnable() {
        public void run() {
            isConnected = false;
            ConnectivityManager conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] networkInfo = conManager.getAllNetworkInfo();
            for (NetworkInfo netInfo : networkInfo) {
                netInfo.isConnected();
                if (netInfo.getTypeName().equalsIgnoreCase("WIFI")) {
                    if (netInfo.isConnected()) {
                        isConnected = true;
                    }
                } else if (netInfo.getTypeName().equalsIgnoreCase("MOBILE")) {
                    if (netInfo.isConnected()) {
                        isConnected = true;
                    }
                }
            }
            if (isTimerStartedFromButton && !isConnected && !isTimerStoppedFromNoConnection) {
                Log.i("MyTag", "STOP TIMER FROM NO CONNECTION");
                isTimerStoppedFromNoConnection = true;
                foregroundServiceTimer.timerThread.stop();
            } else if (isTimerStartedFromButton && isConnected && isTimerStoppedFromNoConnection) {
                Log.i("MyTag", "START TIMER FROM HAVE CONNECTION");
                isTimerStoppedFromNoConnection = false;
                tmpThread = new Thread() {
                    public void run() {
                        foregroundServiceTimer.timerThread.start();
                    }
                };
                tmpThread.start();
            }
            customHandlerNetworkChecker.postDelayed(this, 1000);
        }
    };

}