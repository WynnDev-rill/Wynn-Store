package id.wynn.iwatchyou

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** A real external app window with neither a launcher entry nor an Activity name suffix. */
class FocusProbe : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContentView(TextView(this).apply { text="External focus fixture" })
 }
}
