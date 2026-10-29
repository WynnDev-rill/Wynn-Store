package id.wynn.iwatchyou;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Standalone test-APK window: no launcher, Activity suffix or target-APK dependencies. */
public final class FocusProbe extends Activity {
    private TextView touches;
    private int touchCount;
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN && touches != null)
            touches.setText("Touches: " + (++touchCount));
        return super.dispatchTouchEvent(event);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        TextView label = new TextView(this);
        label.setText("External focus fixture");
        content.addView(label);
        EditText editor = new EditText(this);
        editor.setHint("Keyboard fixture");
        content.addView(editor);
        Button keyboard = new Button(this);
        keyboard.setText("Show keyboard");
        keyboard.setAllCaps(false);
        keyboard.setOnClickListener(view -> {
            editor.requestFocus();
            getSystemService(InputMethodManager.class).showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);
        });
        content.addView(keyboard);
        touches = new TextView(this);
        touches.setText("Touches: 0");
        content.addView(touches);
        setContentView(content);
    }
}
