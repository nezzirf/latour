package fr.soen.tour;

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.util.HashMap;

/* Soen : l'Épreuve de la Tour — le jeu (page web) dans une application Fire TV / Android TV.
   L'application reçoit toutes les touches de la manette et de la télécommande et les transmet au jeu. */
public class MainActivity extends Activity {
    private WebView web;
    private final HashMap<String, Boolean> held = new HashMap<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(0xFF0A0628);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void send(String k, boolean down) {
        Boolean was = held.get(k);
        if (was != null && was == down) return;
        held.put(k, down);
        web.evaluateJavascript("window.soenKey&&window.soenKey('" + k + "'," + down + ")", null);
    }

    private static String keyName(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "right";
            case KeyEvent.KEYCODE_DPAD_UP: return "up";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "down";
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: case KeyEvent.KEYCODE_SPACE: return "a";
            case KeyEvent.KEYCODE_BUTTON_B: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_ESCAPE: return "b";
            case KeyEvent.KEYCODE_BUTTON_X: case KeyEvent.KEYCODE_BUTTON_Y: case KeyEvent.KEYCODE_BUTTON_L1: case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_C: case KeyEvent.KEYCODE_BUTTON_Z: case KeyEvent.KEYCODE_BUTTON_L2: case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_BUTTON_THUMBL: case KeyEvent.KEYCODE_BUTTON_THUMBR: return "x";
            case KeyEvent.KEYCODE_BUTTON_START: case KeyEvent.KEYCODE_BUTTON_SELECT: case KeyEvent.KEYCODE_BUTTON_MODE: case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY: case KeyEvent.KEYCODE_MEDIA_PAUSE: return "pause";
            // manettes génériques : boutons numérotés
            case KeyEvent.KEYCODE_BUTTON_1: case KeyEvent.KEYCODE_BUTTON_3: return "a";
            case KeyEvent.KEYCODE_BUTTON_2: case KeyEvent.KEYCODE_BUTTON_4: return "b";
            case KeyEvent.KEYCODE_BUTTON_9: case KeyEvent.KEYCODE_BUTTON_10: case KeyEvent.KEYCODE_BUTTON_12: return "pause";
            default: return null;
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        String k = keyName(e.getKeyCode());
        if (k == null && (KeyEvent.isGamepadButton(e.getKeyCode()) || (e.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)) k = "x";   // autre bouton de manette : sauter
        if (k == null) return super.dispatchKeyEvent(e);
        if (e.getAction() == KeyEvent.ACTION_DOWN) send(k, true);
        else if (e.getAction() == KeyEvent.ACTION_UP) send(k, false);
        return true;   // la touche est pour le jeu (le bouton Retour ne ferme pas l'application : utiliser le bouton Accueil)
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) {
        int src = e.getSource();
        if (((src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                && e.getAction() == MotionEvent.ACTION_MOVE) {
            float x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
            float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
            send("left", x < -0.45f || hx < -0.5f);
            send("right", x > 0.45f || hx > 0.5f);
            send("up", y < -0.6f || hy < -0.5f);
            send("down", y > 0.6f || hy > 0.5f);
            return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }

    @Override protected void onPause() { super.onPause(); if (web != null) { web.evaluateJavascript("window.soenAppPause&&window.soenAppPause()", null); web.onPause(); } }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
}
