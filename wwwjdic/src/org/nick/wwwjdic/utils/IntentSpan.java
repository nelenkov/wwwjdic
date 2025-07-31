/**
 * 
 */
package org.nick.wwwjdic.utils;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;
import androidx.annotation.NonNull;

public class IntentSpan extends ClickableSpan {
    private final Context context;
    private final Intent intent;

    public IntentSpan(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    @Override
    public void onClick(@NonNull View widget) {
        context.startActivity(intent);
    }

}
