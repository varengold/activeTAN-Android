/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
 *
 * This file is part of the activeTAN app for Android.
 *
 * The activeTAN app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The activeTAN app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the activeTAN app.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.efdis.tangenerator.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.common.TextUtils;

public class WelcomeActivity
        extends AppActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView welcomeTitle = findViewById(R.id.welcomeTitle);
        TextUtils.setHtmlText(welcomeTitle, R.string.welcome_title_html);
    }

    public void onButtonStart(View button) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SKIP_WELCOME_ACTIVITY, true);
        startActivity(intent);
        finish();
        return;
    }
}
