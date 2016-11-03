package es.munix.multicast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import es.munix.multidisplaycast.CastManager;
import es.munix.multidisplaycast.interfaces.CastListener;
import es.munix.multidisplaycast.interfaces.PlayStatusListener;
import es.munix.multidisplaycast.utils.Format;

public class MainActivity extends AppCompatActivity implements CastListener, PlayStatusListener {

    Button videoButton;
    TextView duration;
    TextView position;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        CastManager.getInstance().setDiscoveryManager();
        CastManager.getInstance().setCastListener( this );
        CastManager.getInstance().setPlayStatusListener( this );

        videoButton = (Button) findViewById( R.id.videoButton );
        position = (TextView) findViewById( R.id.position );
        duration = (TextView) findViewById( R.id.duration );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate( R.menu.cast_menu, menu );
        CastManager.getInstance().registerForActivity( this, menu, R.id.castMenu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    protected void onDestroy() {
        Log.v( "Activity", "ondestroy" );
        CastManager.getInstance().onDestroy();
        super.onDestroy();
    }

    @Override
    public void isConnected() {
        videoButton.setText( "Reproducir video" );
        videoButton.setEnabled( true );
    }

    @Override
    public void isDisconnected() {
        videoButton.setText( "Reproducir video" );
        videoButton.setEnabled( false );
    }

    public void playVideo( View v ) {
        if ( videoButton.getText().toString().equals( "Detener video" ) ) {
            CastManager.getInstance().stop();
        } else {
            CastManager.getInstance()
                    .playMedia( "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4", "video/mp4", "Big Buck Bunny", "MP4", "http://camendesign.com/code/video_for_everybody/poster.jpg" );
        }
    }

    @Override
    public void onPlayStatusChanged( int playStatus ) {
        switch( playStatus ) {
            case STATUS_PLAYING:
                videoButton.setText( "Detener video" );
                break;

            case STATUS_FINISHED:
            case STATUS_STOPPED:
                videoButton.setText( "Reproducir video" );
                break;

            case STATUS_PAUSED:
                videoButton.setText( "Reanudar video" );
                break;

            case STATUS_NOT_SUPPORT_LISTENER:
                position.setText( "No soportado por este dispositivo" );
                duration.setText( "No soportado por este dispositivo" );
                break;
        }
    }

    @Override
    public void onPositionChanged( long currentPosition ) {
        position.setText( Format.time( currentPosition ) );
    }

    @Override
    public void onTotalDurationObtained( long totalDuration ) {
        duration.setText( Format.time( totalDuration ) );
    }
}
