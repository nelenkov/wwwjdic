package org.nick.wwwjdic.actionprovider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.SubMenu;
import android.view.View;
import android.view.ActionProvider;

@SuppressLint("NewApi")
public class ShareActionProvider extends ActionProvider {

    private android.widget.ShareActionProvider icsActionProvider;

    public ShareActionProvider(Context context) {
        super(context);
        icsActionProvider = new android.widget.ShareActionProvider(context);
    }

    @Override
    public View onCreateActionView() {
        return icsActionProvider.onCreateActionView();
    }

    @Override
    public boolean hasSubMenu() {
        return icsActionProvider.hasSubMenu();
    }

    @Override
    public void onPrepareSubMenu(final SubMenu subMenu) {
        icsActionProvider
                .onPrepareSubMenu(subMenu);
    }

    public void setShareIntent(Intent shareIntent) {
        icsActionProvider.setShareIntent(shareIntent);
    }

    public void setShareHistoryFileName(String filename) {
        icsActionProvider.setShareHistoryFileName(filename);
    }

}
