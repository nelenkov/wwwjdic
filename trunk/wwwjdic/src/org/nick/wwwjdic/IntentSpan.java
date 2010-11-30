/**
 * 
 */
package org.nick.wwwjdic;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;

public class IntentSpan extends ClickableSpan {
    private Context context;
    private Intent intent;

    public IntentSpan(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    @Override
    public void onClick(View widget) {
        context.startActivity(intent);
    }

}
