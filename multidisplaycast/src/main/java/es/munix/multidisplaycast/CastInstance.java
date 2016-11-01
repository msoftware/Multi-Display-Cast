package es.munix.multidisplaycast;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DIALService;
import com.connectsdk.service.command.ServiceCommandError;

/**
 * Created by munix on 1/11/16.
 */

public class CastInstance implements DiscoveryManagerListener, MenuItem.OnMenuItemClickListener {

    private static final String TAG = "CastInstance";
    private static final boolean ENABLE_LOG = true;
    private static CastInstance instance;
    private static Context appContext;
    private DiscoveryManager discoveryManager;
    private MenuItem castMenuItem;
    private Activity activity;

    public static CastInstance getInstance() {
        if ( instance == null ) {
            instance = new CastInstance();
        }
        return instance;
    }


    public static void register( Context context ) {
        DIALService.registerApp( "multicast" );
        DiscoveryManager.init( context );
        appContext = context;
    }

    public void setDiscoveryManager() {
        if ( discoveryManager == null ) {
            discoveryManager = DiscoveryManager.getInstance();
            DiscoveryManager.getInstance().registerDefaultDeviceTypes();
            discoveryManager.setPairingLevel( DiscoveryManager.PairingLevel.ON );
            discoveryManager.addListener( this );
            discoveryManager.start();
        } else {
            discoveryManager.addListener( this );
            discoveryManager.start();
            calculateMenuVisibility();
        }
    }

    public void registerForActivity( Activity activity, Menu menu, int menuId ) {
        this.activity = activity;
        castMenuItem = menu.findItem( menuId );
        castMenuItem.setOnMenuItemClickListener( this );
        calculateMenuVisibility();
    }

    public void setCastMenuVisible( Boolean visible ) {
        if ( castMenuItem != null ) {
            castMenuItem.setVisible( visible );
        }
    }

    private void log( String log ) {
        if ( ENABLE_LOG ) {
            Log.i( TAG, log );
        }
    }

    @Override
    public void onDeviceAdded( DiscoveryManager manager, ConnectableDevice device ) {
        log( "onDeviceAdded " + manager.getAllDevices().size() );
        calculateMenuVisibility();
    }

    @Override
    public void onDeviceUpdated( DiscoveryManager manager, ConnectableDevice device ) {
        log( "onDeviceUpdated" );
        calculateMenuVisibility();
    }

    @Override
    public void onDeviceRemoved( DiscoveryManager manager, ConnectableDevice device ) {
        log( "onDeviceRemoved" );
        calculateMenuVisibility();
    }

    @Override
    public void onDiscoveryFailed( DiscoveryManager manager, ServiceCommandError error ) {
        log( "onDiscoveryFailed" );
        calculateMenuVisibility();
    }

    @Override
    public boolean onMenuItemClick( MenuItem menuItem ) {
        DevicePicker devicePicker = new DevicePicker( activity );
        AlertDialog dialog = devicePicker.getPickerDialog( "Selecciona dispositivo", null );
        dialog.show();

        return false;
    }

    private void calculateMenuVisibility() {
        if ( discoveryManager != null ) {
            setCastMenuVisible( discoveryManager.getAllDevices().size() > 0 );
        }
    }

    public void onDestroy() {
        activity = null;
    }
}
