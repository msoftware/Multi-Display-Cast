package es.munix.multidisplaycast.utils;


import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class Effect {

    public static void appear( View v, int duration ) {
        if ( v.getVisibility() != View.VISIBLE ) {
            ObjectAnimator.ofFloat( v, "alpha", 0, 1 ).setDuration( duration ).start();
            v.setVisibility( View.VISIBLE );
        }
    }

    public static void disappear( View v, int duration ) {
        if ( v.getVisibility() == View.VISIBLE ) {
            Animation fadeInAnimation = AnimationUtils.loadAnimation( v.getContext(), android.R.anim.fade_out );
            fadeInAnimation.setDuration( duration );
            v.startAnimation( fadeInAnimation );
            v.setVisibility( View.GONE );
        }
    }
}
