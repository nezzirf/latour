package fr.soen.tour;

import android.app.Activity;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/* Soen : l'Épreuve de la Tour — le jeu (page web) dans une application Fire TV / Android TV.
   L'application note l'état des touches (manette et télécommande) ; le jeu le lit à chaque image. */
public class MainActivity extends Activity {
    private WebView web;
    // bits : 0 gauche, 1 droite, 2 haut, 3 bas, 4 A, 5 B, 6 X, 7 pause ; 8 à 11 : gauche, droite, haut, bas du joystick
    volatile int keys = 0;
    volatile int presses = 0;
    volatile int latched = 0;   // compteur d'appuis : le jeu ne rate jamais un appui très court
    private int toasts = 0;
    boolean jsSeen = false;

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
        web.addJavascriptInterface(new SoenBridge(this), "SoenAndroid");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(0xFF0A0628);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.setFocusable(false);   // la vue web ne doit pas « manger » les touches
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void set(int bit, boolean down) {
        int m = 1 << bit;
        if (down) { if ((keys & m) == 0) { presses++; latched |= m; } keys |= m; } else keys &= ~m;
    }

    private static int keyBit(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return 0;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 1;
            case KeyEvent.KEYCODE_DPAD_UP: return 2;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 3;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: case KeyEvent.KEYCODE_SPACE: case KeyEvent.KEYCODE_BUTTON_1: case KeyEvent.KEYCODE_BUTTON_3: return 4;
            case KeyEvent.KEYCODE_BUTTON_B: case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_ESCAPE: case KeyEvent.KEYCODE_BUTTON_2: case KeyEvent.KEYCODE_BUTTON_4: return 5;
            case KeyEvent.KEYCODE_BUTTON_START: case KeyEvent.KEYCODE_BUTTON_SELECT: case KeyEvent.KEYCODE_BUTTON_MODE: case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_MEDIA_PLAY: case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_BUTTON_9: case KeyEvent.KEYCODE_BUTTON_10: case KeyEvent.KEYCODE_BUTTON_12: return 7;
            default: return KeyEvent.isGamepadButton(code) ? 6 : -1;   // X, Y, gâchettes… : sauter
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int bit = keyBit(e.getKeyCode());
        if (bit < 0 && (e.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) bit = 6;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0 && toasts < 6) {   // contrôle : ce que reçoit Android
            toasts++;
            Toast.makeText(this, "Touche : " + KeyEvent.keyCodeToString(e.getKeyCode()).replace("KEYCODE_", "") + (bit < 0 ? " (ignorée)" : ""), Toast.LENGTH_SHORT).show();
        }
        if (bit < 0) return super.dispatchKeyEvent(e);
        if (e.getAction() == KeyEvent.ACTION_DOWN) set(bit, true);
        else if (e.getAction() == KeyEvent.ACTION_UP) set(bit, false);
        return true;   // la touche est pour le jeu (pour quitter : bouton Accueil)
    }

    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) {
        int src = e.getSource();
        if (((src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                && e.getAction() == MotionEvent.ACTION_MOVE) {
            float x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
            float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
            set(8, x < -0.45f || hx < -0.5f);
            set(9, x > 0.45f || hx > 0.5f);
            set(10, y < -0.6f || hy < -0.5f);
            set(11, y > 0.6f || hy > 0.5f);
            return true;
        }
        return super.dispatchGenericMotionEvent(e);
    }

    @Override protected void onPause() { super.onPause(); keys = 0; if (web != null) { web.evaluateJavascript("window.soenAppPause&&window.soenAppPause()", null); web.onPause(); } }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
}
