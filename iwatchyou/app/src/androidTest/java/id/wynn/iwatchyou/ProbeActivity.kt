package id.wynn.iwatchyou

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import android.widget.Button

/** Separate test APK window; never included in the production application. */
class ProbeActivity: Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  if(intent.getBooleanExtra("finish",false)) { finish();return }
  setContentView(Button(this).apply { text="PiP fixture";isAllCaps=false;setOnClickListener { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16,9)).build()) } })
 }
}
