package com.example.aidl_client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aidl_service.aidlInterface;

import java.util.List;

public class MainActivity extends Activity {

    private TextView latDisplay, longDisplay;
    private Button getLocationButton, setLocationButton;
    private Context mContext;

    public aidlInterface aidlInterfaceObject;

    private double currentLat, currentLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI components
        mContext = this;
        latDisplay = findViewById(R.id.lat_display);
        longDisplay = findViewById(R.id.long_display);
        getLocationButton = findViewById(R.id.get_location_button);
        setLocationButton = findViewById(R.id.set_location_button);

        // Set up the button click to fetch location
        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchLocation();
            }
        });

        // Set up the button click to set location
        setLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocation();
            }
        });

        // Bind to the AIDL service
        bindAIDLService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);  // Unbind the service
            serviceConnection = null;          // Set to null to prevent leaks
        }
    }

    // Fetch location from the AIDL service
    private void fetchLocation() {
        if (aidlInterfaceObject != null) {
            try {
                // Call AIDL method to get random location
                double[] latLong = aidlInterfaceObject.getRandomLatLong();
                currentLat = latLong[0];
                currentLon = latLong[1];

                // Display the latitude and longitude
                latDisplay.setText("Latitude: " + currentLat);
                longDisplay.setText("Longitude: " + currentLon);

            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(mContext, "Error fetching location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Set location by dividing the received values by 2 and sending them back to the server
    private void setLocation() {
        if (aidlInterfaceObject != null) {
            try {
                // Divide the current latitude and longitude by 2
                double newLat = currentLat / 2;
                double newLon = currentLon / 2;

                // Send the new lat/long back to the service
                aidlInterfaceObject.setNewLatLong(newLat, newLon);

            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error setting location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            aidlInterfaceObject = aidlInterface.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            aidlInterfaceObject = null;
        }
    };

    private void bindAIDLService() {
        try {
            Intent intent = new Intent("com.example.aidl_service");
            bindService(convertImplicitIntentToExplicitIntent(intent, mContext), serviceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            Toast.makeText(mContext, "Service App may not be present", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }

    public Intent convertImplicitIntentToExplicitIntent(Intent implicitIntent, Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentServices(implicitIntent, 0);
        if (resolveInfoList == null || resolveInfoList.size() != 1) {
            return null;
        }
        ResolveInfo serviceInfo = resolveInfoList.get(0);
        ComponentName component = new ComponentName(serviceInfo.serviceInfo.packageName, serviceInfo.serviceInfo.name);
        Intent explicitIntent = new Intent(implicitIntent);
        explicitIntent.setComponent(component);
        return explicitIntent;
    }
}
