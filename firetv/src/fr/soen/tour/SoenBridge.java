package fr.soen.tour;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

/* Ce que le jeu (JavaScript) peut demander à l'application : l'état des touches. */
public class SoenBridge implements Runnable {
    private final MainActivity act;
    public SoenBridge(MainActivity a) { act = a; }
    @JavascriptInterface public int state() { int r = act.keys | act.latched; act.latched = 0; return r; }   // un appui très court reste visible une image
    @JavascriptInterface public int presses() { return act.presses; }
    @JavascriptInterface public void seen() { if (!act.jsSeen) { act.jsSeen = true; act.runOnUiThread(this); } }
    public void run() { Toast.makeText(act, "Le jeu reçoit la manette !", Toast.LENGTH_SHORT).show(); }
}
