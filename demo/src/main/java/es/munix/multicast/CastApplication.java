package es.munix.multicast;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import es.munix.multidisplaycast.CastInstance;
import io.fabric.sdk.android.Fabric;

/**
 * Created by munix on 1/11/16.
 */

public class CastApplication extends Application {

    private static Context context;

    public static Context getAppContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with( this, new Crashlytics() );
        context = this.getApplicationContext();
        CastInstance.register( getApplicationContext() );
    }
}
