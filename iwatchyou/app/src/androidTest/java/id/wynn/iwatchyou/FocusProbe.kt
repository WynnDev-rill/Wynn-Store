package id.wynn.iwatchyou

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** A real external app window with neither a launcher entry nor an Activity name suffix. */
class FocusProbe : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
  setContentView(LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   addView(TextView(this@FocusProbe).apply { text="External focus fixture" })
   addView(EditText(this@FocusProbe).apply { hint="Keyboard fixture" })
  })
 }
}
