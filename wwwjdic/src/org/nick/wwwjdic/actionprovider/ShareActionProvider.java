/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// backport to 2.x using ActionBarSherlock by Nikolay Elenkov

package org.nick.wwwjdic.actionprovider;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.actionbarsherlock.internal.view.menu.MenuWrapper;
import com.actionbarsherlock.view.ActionProvider;
import com.actionbarsherlock.view.SubMenu;

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
                .onPrepareSubMenu((android.view.SubMenu) ((MenuWrapper) subMenu)
                        .unwrap());
    }

    public void setShareIntent(Intent shareIntent) {
        icsActionProvider.setShareIntent(shareIntent);
    }

    public void setShareHistoryFileName(String filename) {
        icsActionProvider.setShareHistoryFileName(filename);
    }

}
