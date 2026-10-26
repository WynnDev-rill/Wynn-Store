package id.wynn.furina

import android.app.Activity
import android.os.Bundle
import android.widget.Button

/** Debug-only underlying click target; never packaged in an owner release. */
class OverlayProbeActivity: Activity() {
 companion object {@Volatile var clicks=0}
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContentView(Button(this).apply {text="Overlay test target";setOnClickListener {clicks++;text="Clicks: $clicks"}})
 }
}
