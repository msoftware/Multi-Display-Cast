package es.munix.multicast;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import es.munix.multidisplaycast.CastListener;
import es.munix.multidisplaycast.CastManager;

public class MainActivity extends AppCompatActivity implements CastListener {

    Button videoButton;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        CastManager.getInstance().setDiscoveryManager();
        CastManager.getInstance().setListener( this );
        videoButton = (Button) findViewById( R.id.videoButton );
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

    @Override
    public void onPlayStart() {
        videoButton.setText( "Detener video" );
    }

    @Override
    public void onPlayStop() {
        videoButton.setText( "Reproducir video" );
    }

    public void playVideo( View v ) {
        if ( videoButton.getText().toString().equals( "Detener video" ) ) {
            CastManager.getInstance().stop();
        } else {
            CastManager.getInstance()
                    .playMedia( "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4", "video/mp4", "Big Buck Bunny", "MP4", "http://camendesign.com/code/video_for_everybody/poster.jpg" );
        }
    }
}
