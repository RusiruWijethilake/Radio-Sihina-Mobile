package lk.microlion.dev.radiosihina;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class RadioPlayerWebInterface {

    private Context context;

    public RadioPlayerWebInterface(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
}
