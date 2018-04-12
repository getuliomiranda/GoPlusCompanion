package com.kingpuller.gocompanion.model;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.kingpuller.gocompanion.R;
import com.kingpuller.gocompanion.helper.Constants;
import com.kingpuller.gocompanion.helper.NotificationHelper;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GoPlus {

    private Context context;
    private NotificationManager notificationManager;

    public GoPlus(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void setup() {
        try {
            Toast.makeText(this.context, context.getString(R.string.connecting), Toast.LENGTH_SHORT).show();
        } catch(Exception e){
            // WindowManager$BadTokenException will be caught and the app would not display
            // the 'Force Close' message
        }
        BluetoothDevice goPlus = null;
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Service.BLUETOOTH_SERVICE);
        if (bluetoothManager != null && bluetoothManager.getAdapter() != null && bluetoothManager.getAdapter().isEnabled()) {
            long time = System.currentTimeMillis();
            while (goPlus == null) {
                List<BluetoothDevice> btDevices = bluetoothManager.getConnectedDevices(BluetoothGatt.GATT);
                for (BluetoothDevice bluetoothDevice : btDevices) {
                    if (bluetoothDevice.getName().equals(Constants.DEVICE_NAME)) {
                        goPlus = bluetoothDevice;
                    }
                }
                if (System.currentTimeMillis() > 30000 + time) {
                    try {
                    Toast.makeText(this.context, context.getString(R.string.connecting_failed), Toast.LENGTH_SHORT).show();
                    } catch(Exception e){
                        // WindowManager$BadTokenException will be caught and the app would not display
                        // the 'Force Close' message
                    }
                    break;
                }
            }
            if (goPlus != null) {
                BluetoothGatt goPlusGatt = goPlus.connectGatt(this.context, false, new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (status == 0) {
                            gatt.discoverServices();
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        if (status == 0) {
                            for (BluetoothGattService gattService : gatt.getServices()) {
                                Log.i("GoPlus", "Service UUID Found: " + gattService.getUuid().toString());
                            }
                        }
                        BluetoothGattService batteryService = gatt.getService(Constants.Battery_Service_UUID);
                        if (batteryService == null) {
                            Log.d("GoPlus", "Battery service not found!");
                        }
                        assert batteryService != null;
                        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(Constants.Battery_Level_UUID);
                        if (batteryLevel == null) {
                            Log.d("GoPlus", "Battery level not found!");
                        }
                        loopBattery(gatt, batteryLevel);
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                        if (characteristic.getUuid().equals(Constants.Battery_Level_UUID)) {
                            int goPlusBatt = characteristic.getIntValue(33, 0);
                            Notification notification = NotificationHelper.updateNotification(context, goPlusBatt);
                            notificationManager.notify(Constants.SERVICE_NOTIFICATION_ID, notification);
                        }
                    }
                });

                if (goPlusGatt != null) {
                    goPlusGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                }
            }
        }
    }

    private void loopBattery(final BluetoothGatt gatt, final BluetoothGattCharacteristic batteryLevel) {
        final ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                gatt.readCharacteristic(batteryLevel);
                Log.d("GoPlus", "Read battery");
            }
        }, 0, 10, TimeUnit.MINUTES);
    }
}
