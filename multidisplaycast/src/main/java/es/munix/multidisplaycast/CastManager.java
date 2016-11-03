package es.munix.multidisplaycast;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.device.DevicePicker;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.google.gson.Gson;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;
import es.munix.multidisplaycast.services.CastReceiver;
import es.munix.multidisplaycast.services.DummyService;

/**
 * Created by munix on 1/11/16.
 */

public class CastManager implements DiscoveryManagerListener, MenuItem.OnMenuItemClickListener, ConnectableDeviceListener, MediaControl.PlayStateListener {

    private static final boolean ENABLE_LOG = true;
    private static final String TAG = "CastInstance";
    private static final String SHARED_PREFS = "MultiCast";
    private static final int NOTIFICATION_ID = 800;
    private static CastManager instance;
    private Context context;

    //Multimedia
    private DiscoveryManager discoveryManager;
    private MenuItem castMenuItem;
    private ConnectableDevice connectableDevice;
    private CastListener castListener;
    private PlayStatusListener playStatusListener;
    private MediaControl mMediaControl;

    //Unset at destroy
    private Activity activity;

    //Dialogos
    private AlertDialog connectToCastDialog;
    private AlertDialog pairingAlertDialog;
    private AlertDialog pairingCodeDialog;

    //Listeners no implemetables
    private MediaControl.DurationListener durationListener;
    private MediaControl.PositionListener positionListener;

    //Otros
    private Timer refreshTimer;
    private long totalDuration = -1;
    private long currentPosition = 0;
    private String title;
    private String icon;


    public static CastManager getInstance() {
        if ( instance == null ) {
            instance = new CastManager();
        }
        return instance;
    }


    public static void register( Context context ) {
        DiscoveryManager.init( context );
        getInstance().setContext( context );
    }

    private void setContext( Context context ) {
        this.context = context;
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

    public void setCastListener( CastListener listener ) {
        this.castListener = listener;
    }

    public void setPlayStatusListener( PlayStatusListener listener ) {
        this.playStatusListener = listener;
    }

    public void registerForActivity( Activity activity, Menu menu, int menuId ) {
        log( "registerForActivity" );
        this.activity = activity;
        castMenuItem = menu.findItem( menuId );
        castMenuItem.setIcon( R.drawable.cast_off );
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
        calculateMenuVisibility();
        String mRecentDeviceId = getRecentDeviceId();

        if ( mRecentDeviceId != null && connectableDevice == null ) {
            log( "reconnect from previous device" );

            if ( device.getId().equalsIgnoreCase( mRecentDeviceId ) ) {
                log( "onDeviceAdded launch connect for " + mRecentDeviceId );
                device.addListener( this );
                device.connect();
            }
        }
    }

    @Override
    public void onDeviceUpdated( DiscoveryManager manager, ConnectableDevice device ) {
        calculateMenuVisibility();
    }

    @Override
    public void onDeviceRemoved( DiscoveryManager manager, ConnectableDevice device ) {
        log( "onDeviceRemoved" );
        calculateMenuVisibility();
    }

    @Override
    public void onDiscoveryFailed( DiscoveryManager manager, ServiceCommandError error ) {
        Log.e( TAG, "onDiscoveryFailed" );
        calculateMenuVisibility();
    }

    public void disconnect() {
        stop();
        stopUpdating();
        connectableDevice.disconnect();
        setRecentDeviceId( "" );
        castMenuItem.setIcon( R.drawable.cast_off );
    }

    private void showDisconnectAlert( String title, String image ) {
        View customView = View.inflate( activity, R.layout.cast_disconnect, null );
        final TextView deviceName = (TextView) customView.findViewById( R.id.deviceName );
        if ( connectableDevice.getFriendlyName() != null ) {
            deviceName.setText( connectableDevice.getFriendlyName() );
        } else {
            deviceName.setText( connectableDevice.getModelName() );
        }

        final TextView mediaTitle = (TextView) customView.findViewById( R.id.mediaTitle );
        mediaTitle.setText( title );

        final ImageView mediaImage = (ImageView) customView.findViewById( R.id.mediaImage );
        if ( image != null ) {
            Glide.with( activity ).load( image ).into( mediaImage );
        } else {
            mediaImage.setVisibility( View.GONE );
        }

        new AlertDialog.Builder( activity ).setView( customView )
                .setPositiveButton( "Dejar de enviar contenido", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick( DialogInterface dialogInterface, int i ) {
                        dialogInterface.cancel();
                        disconnect();
                    }
                } )
                .show();
    }

    @Override
    public boolean onMenuItemClick( MenuItem menuItem ) {
        if ( isConnected() ) {
            if ( mMediaControl != null ) {
                connectableDevice.getCapability( MediaPlayer.class )
                        .getMediaInfo( new MediaPlayer.MediaInfoListener() {
                            @Override
                            public void onSuccess( MediaInfo object ) {
                                String image = null;
                                if ( object.getImages() != null && object.getImages().size() > 0 ) {
                                    image = object.getImages().get( 0 ).getUrl();
                                }
                                showDisconnectAlert( object.getTitle(), image );
                            }

                            @Override
                            public void onError( ServiceCommandError error ) {
                                if ( !TextUtils.isEmpty( title ) && !TextUtils.isEmpty( icon ) ) {
                                    showDisconnectAlert( title, icon );
                                } else {
                                    showDisconnectAlert( "Sin información multimedia", null );
                                }
                            }
                        } );
            } else {
                disconnect();
            }
        } else {
            final DevicePicker devicePicker = new DevicePicker( activity );
            connectToCastDialog = devicePicker.getPickerDialog( "Selecciona dispositivo", new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView<?> adapterView, View view, int i, long l ) {
                    connectToCastDialog.cancel();
                    connectableDevice = (ConnectableDevice) adapterView.getItemAtPosition( i );
                    connectableDevice.addListener( CastManager.this );
                    connectableDevice.connect();
                }
            } );
            connectToCastDialog.show();

            pairingAlertDialog = new AlertDialog.Builder( activity ).setTitle( "Conectando con su TV" )
                    .setMessage( "Confirme la conexión con su TV" )
                    .setPositiveButton( "Aceptar", null )
                    .setNegativeButton( "Cancelar", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick( DialogInterface dialog, int which ) {
                            devicePicker.cancelPicker();
                            connectToCastDialog.show();
                        }
                    } )
                    .create();

            View v = View.inflate( activity, R.layout.input_code_dialog, null );
            final EditText input = (EditText) v.findViewById( R.id.input );
            input.setMaxLines( 1 );


            final InputMethodManager imm = (InputMethodManager) activity.getApplicationContext()
                    .getSystemService( Context.INPUT_METHOD_SERVICE );

            pairingCodeDialog = new AlertDialog.Builder( activity ).setTitle( "Ingrese el código que ve en la TV" )
                    .setView( v )
                    .setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface arg0, int arg1 ) {
                            if ( connectableDevice != null ) {
                                String value = input.getText().toString().trim();
                                connectableDevice.sendPairingKey( value );
                                imm.hideSoftInputFromWindow( input.getWindowToken(), 0 );
                            }
                        }
                    } )
                    .setNegativeButton( android.R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            devicePicker.cancelPicker();
                            imm.hideSoftInputFromWindow( input.getWindowToken(), 0 );
                        }
                    } )
                    .create();
        }
        return false;
    }

    public void cancelNotification() {
        NotificationManager nMgr = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
        nMgr.cancel( NOTIFICATION_ID );
    }

    private void showNotification( String title, String subtitle, final String icon ) {
        Intent disconnectIntent = new Intent( context, CastReceiver.class );
        disconnectIntent.putExtra( "action", "disconnect" );
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast( context, NOTIFICATION_ID, disconnectIntent, 0 );

        final NotificationCompat.Builder notification = new NotificationCompat.Builder( context ).setOngoing( true )
                .setAutoCancel( false )
                .setContentTitle( title )
                .setContentText( subtitle )
                .addAction( R.drawable.ic_stop_white_24dp, "Detener", disconnectPendingIntent )
                .setSmallIcon( R.drawable.cast_on );

        final Handler mHandler = new Handler() {
            @Override
            public void handleMessage( Message msg ) {
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
                notificationManager.notify( NOTIFICATION_ID, notification.build() );
            }
        };

        new Thread() {
            @Override
            public void run() {
                try {
                    Bitmap largeIcon = Glide.
                            with( context ).
                            load( icon ).
                            asBitmap().
                            into( 100, 100 ).
                            get();
                    notification.setLargeIcon( largeIcon );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                } catch ( ExecutionException e ) {
                    e.printStackTrace();
                } catch ( OutOfMemoryError oom ) {
                    oom.printStackTrace();
                }
                mHandler.sendEmptyMessage( 1 );
            }
        }.start();
    }

    public void playMedia( String url, String mimeType, final String title, final String subtitle, final String icon ) {
        if ( isConnected() ) {

            MediaInfo mediaInfo = new MediaInfo.Builder( url, mimeType ).setTitle( title )
                    .setDescription( subtitle )
                    .setIcon( icon )
                    .build();


            connectableDevice.getCapability( MediaPlayer.class )
                    .playMedia( mediaInfo, false, new MediaPlayer.LaunchListener() {

                        public void onSuccess( MediaPlayer.MediaLaunchObject object ) {

                            CastManager.this.title = title;
                            CastManager.this.icon = icon;

                            showNotification( title, subtitle, icon );

                            mMediaControl = object.mediaControl;
                            mMediaControl.subscribePlayState( CastManager.this );


                            /*try {
                                final LaunchSession session = LaunchSession.launchSessionFromJSONObject( object.launchSession
                                        .toJSONObject() );

                                new Handler().postDelayed( new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText( activity, "desconectar desde sesión", Toast.LENGTH_LONG )
                                                .show();

                                        connectableDevice.getCapability( MediaPlayer.class )
                                                .closeMedia( session, new ResponseListener<Object>() {
                                                    @Override
                                                    public void onError( ServiceCommandError error ) {

                                                    }

                                                    @Override
                                                    public void onSuccess( Object object ) {

                                                    }
                                                } );
                                    }
                                }, 2000 );
                            } catch ( JSONException e ) {
                                e.printStackTrace();
                            }*/

                            if ( playStatusListener != null ) {
                                playStatusListener.onPlayStatusChanged( PlayStatusListener.STATUS_PLAYING );
                            }

                            Intent i = new Intent( context, DummyService.class );
                            i.addCategory( "DummyServiceControl" );
                            context.startService( i );

                            createListeners();
                            startUpdating();
                        }

                        @Override
                        public void onError( ServiceCommandError error ) {
                            stop();
                            Toast.makeText( activity, "Contenido no compatible", Toast.LENGTH_LONG )
                                    .show();
                        }
                    } );
        }
    }

    private void unsetMediaControl() {
        mMediaControl = null;
        cancelNotification();
        if ( castListener != null ) {
            if ( playStatusListener != null ) {
                playStatusListener.onPlayStatusChanged( PlayStatusListener.STATUS_STOPPED );
            }
        }
        icon = null;
        title = null;
    }

    public void stop() {
        if ( isConnected() && mMediaControl != null ) {
            mMediaControl.stop( new ResponseListener<Object>() {

                @Override
                public void onSuccess( Object response ) {
                    unsetMediaControl();
                }

                @Override
                public void onError( ServiceCommandError error ) {
                    unsetMediaControl();
                }
            } );
        }
    }

    public Boolean isConnected() {
        return connectableDevice != null && connectableDevice.isConnected();
    }

    private void calculateMenuVisibility() {
        if ( discoveryManager != null ) {
            setCastMenuVisible( discoveryManager.getAllDevices().size() > 0 );
        }
    }

    public String getRecentDeviceId() {
        SharedPreferences preferences = context.getSharedPreferences( SHARED_PREFS, Context.MODE_PRIVATE );
        return preferences.getString( "recentDeviceId", "" );
    }

    public void setRecentDeviceId( String deviceId ) {
        SharedPreferences preferences = context.getSharedPreferences( SHARED_PREFS, Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString( "recentDeviceId", deviceId );
        editor.commit();
    }

    @Override
    public void onDeviceReady( ConnectableDevice device ) {
        log( "onDeviceReady is connected " + device.isConnected() );
        if ( device.isConnected() ) {
            connectableDevice = device;
            setRecentDeviceId( device.getId() );
            castMenuItem.setIcon( R.drawable.cast_on );
            if ( castListener != null ) {
                castListener.isConnected();
            }
        }
    }

    @Override
    public void onDeviceDisconnected( ConnectableDevice device ) {
        if ( castListener != null ) {
            castListener.isDisconnected();
        }
    }

    @Override
    public void onPairingRequired( ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType ) {
        switch( pairingType ) {
            case FIRST_SCREEN:
                pairingAlertDialog.show();
                break;

            case PIN_CODE:
            case MIXED:
                pairingCodeDialog.show();
                break;

            case NONE:
            default:
                break;
        }
    }

    @Override
    public void onCapabilityUpdated( ConnectableDevice device, List<String> added, List<String> removed ) {
        log( "onCapabilityUpdated " + new Gson().toJson( added ) );
    }

    @Override
    public void onConnectionFailed( ConnectableDevice device, ServiceCommandError error ) {
        Log.e( TAG, "onConnectionFailed" );
    }

    public void onDestroy() {
        title = null;
        icon = null;
        cancelNotification();
        if ( connectToCastDialog != null ) {
            connectToCastDialog.cancel();
            connectToCastDialog = null;
        }
        if ( connectableDevice != null ) {
            connectableDevice.disconnect();
            connectableDevice.removeListener( this );
            connectableDevice = null;
        }
        if ( discoveryManager != null ) {
            discoveryManager.removeListener( this );
        }

        if ( pairingAlertDialog != null ) {
            pairingAlertDialog.cancel();
            pairingAlertDialog = null;
        }
        if ( pairingCodeDialog != null ) {
            pairingCodeDialog.cancel();
            pairingCodeDialog = null;
        }

        positionListener = null;
        durationListener = null;
        stopUpdating();

        activity = null;
    }

    private void stopUpdating() {
        if ( refreshTimer == null )
            return;

        refreshTimer.cancel();
        refreshTimer = null;
    }

    private void startUpdating() {
        stopUpdating();
        refreshTimer = new Timer();
        refreshTimer.schedule( new TimerTask() {

            @Override
            public void run() {
                try {
                    mMediaControl.getPosition( positionListener );

                    mMediaControl.getDuration( durationListener );
                } catch ( Exception e ) {
                    if ( playStatusListener != null ) {
                        playStatusListener.onPlayStatusChanged( PlayStatusListener.STATUS_NOT_SUPPORT_LISTENER );
                        stopUpdating();
                    }
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis( 1 ) );
    }

    //Control de reproducción del video en la pantalla remota
    @Override
    public void onSuccess( MediaControl.PlayStateStatus playState ) {
        switch( playState ) {
            case Playing:
                log( "PlayStateStatus: playing" );
                break;

            case Finished:
                log( "PlayStateStatus: finished" );
                break;

            case Buffering:
                log( "PlayStateStatus: buffering" );
                break;

            case Idle:
                log( "PlayStateStatus: idle" );
                break;

            case Paused:
                log( "PlayStateStatus: paused" );
                break;

            case Unknown:
                log( "PlayStateStatus: unknown" );
                break;
        }
    }

    private void evaluatePositionAndDuration() {
        if ( playStatusListener != null ) {

            playStatusListener.onTotalDurationObtained( totalDuration );
            playStatusListener.onPositionChanged( currentPosition );

            if ( totalDuration > 0 && currentPosition >= totalDuration ) {
                playStatusListener.onPlayStatusChanged( PlayStatusListener.STATUS_FINISHED );
            }
        }
    }

    private void createListeners() {
        positionListener = new MediaControl.PositionListener() {

            @Override
            public void onError( ServiceCommandError error ) {
                error.printStackTrace();
            }

            @Override
            public void onSuccess( Long position ) {
                currentPosition = position;
                evaluatePositionAndDuration();
            }
        };
        durationListener = new MediaControl.DurationListener() {

            @Override
            public void onError( ServiceCommandError error ) {
                error.printStackTrace();
            }

            @Override
            public void onSuccess( Long duration ) {
                totalDuration = duration;
                evaluatePositionAndDuration();
            }
        };
    }

    @Override
    public void onError( ServiceCommandError error ) {
    }
    //////////////////////////////////////////////////////////////
}
