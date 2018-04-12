package com.kingpuller.gocompanion.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kingpuller.gocompanion.helper.Constants;
import com.kingpuller.gocompanion.helper.NotificationHelper;
import com.kingpuller.gocompanion.model.GoPlus;

import java.util.List;

import static com.kingpuller.gocompanion.helper.Constants.DISCONNECT_NOTIFICATION_ID;
import static com.kingpuller.gocompanion.helper.Constants.SERVICE_NOTIFICATION_ID;

public class ScanService extends Service {

    private GoPlus goPlus;
    private BroadcastReceiver receiver;
    private NotificationManager notificationManager;
    private static String channelId = "default_channel_id";
    private static String channelDescription = "Service Channel";
    private String channel2Description = "Disconnect Channel";
    private String channelId2 = "disconnect_channel_id";


    public ScanService() {
        receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    if (device.getName() != null && device.getName().equals(Constants.DEVICE_NAME)) {
                        goPlus.setup();
                    }
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) && device.getName().equals(Constants.DEVICE_NAME)) {
                    Notification disconnectNotification = NotificationHelper.showDisconnectNotification(context, channelId2, channel2Description, notificationManager);
                    notificationManager.notify(DISCONNECT_NOTIFICATION_ID, disconnectNotification);
                    Notification resetNotification = NotificationHelper.buildNotification(context, channelId, channelDescription, notificationManager);
                    notificationManager.notify(SERVICE_NOTIFICATION_ID, resetNotification);
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        Notification notification = NotificationHelper.buildNotification(getBaseContext(), channelId, channelDescription, notificationManager);
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        this.goPlus = new GoPlus(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(this.receiver, filter);
        alreadyConnected();
        return Service.START_STICKY;
    }

    public void alreadyConnected() {
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Service.BLUETOOTH_SERVICE);
        if (bluetoothManager != null && bluetoothManager.getAdapter() != null && bluetoothManager.getAdapter().isEnabled()) {
            List<BluetoothDevice> btDevices = bluetoothManager.getConnectedDevices(BluetoothGatt.GATT);
            for (BluetoothDevice bluetoothDevice : btDevices) {
                if (bluetoothDevice.getName() != null && bluetoothDevice.getName().equals(Constants.DEVICE_NAME)) {
                    ScanService.this.goPlus.setup();
                }
            }
        }
    }

    public void onDestroy() {
        if (this.receiver != null) {
            try {
                unregisterReceiver(this.receiver);
            } catch (IllegalArgumentException ex) {
                Log.d("ScanService", "onDestroy: " + ex.toString());
            }
            this.receiver = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }
}
