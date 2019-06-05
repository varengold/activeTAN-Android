/*
 * Copyright (c) 2019 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import de.efdis.tangenerator.R;

/**
 * This is the base class for all activities used in this app.
 *
 * For the main activity, a navigation drawer can be used to navigate to other activities. A menu
 * icon will be shown in the action bar.
 *
 * For other activities, a back arrow will be shown in the action bar. The back arrow navigates to
 * the parent activity, which has been defined in the manifest.
 */
public abstract class AppActivity
        extends AppCompatActivity {

    /** Utility method to apply HTML formatting to text. */
    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String html){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    public void setHtmlText(TextView textView, @StringRes int html) {
        setHtmlText(textView, getText(html).toString());
    }

    public static void setHtmlText(TextView textView, String html) {
        Spanned formatted = fromHtml(html);
        textView.setText(formatted);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onStart() {
        super.onStart();

        Toolbar actionBar = findViewById(R.id.actionBar);
        if (actionBar != null) {
            prepareActionBar(actionBar);
        }
    }

    private void prepareActionBar(Toolbar actionBar) {
        setSupportActionBar(actionBar);

        Intent parent = getParentActivityIntent();
        if (parent != null) {
            prepareBackArrow(actionBar, parent);
        } else {
            DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
            NavigationView navigationDrawer = findViewById(R.id.navigationDrawer);
            if (drawerLayout != null && navigationDrawer != null) {
                prepareNavigationDrawer(actionBar, drawerLayout, navigationDrawer);
            }
        }
    }

    private Drawable getTintedDrawable(Context context, int id) {
        Drawable drawable = getDrawable(id);

        TypedArray ta = context.obtainStyledAttributes(
                new int[]{android.R.attr.textColorPrimary});
        int textColorPrimary = ta.getColor(0, -1);
        ta.recycle();

        drawable.setTint(textColorPrimary);

        return drawable;
    }

    private void prepareNavigationDrawer(Toolbar actionBar, final DrawerLayout drawerLayout, final NavigationView navigationDrawer) {
        Drawable menuIcon = getTintedDrawable(
                actionBar.getContext(),
                io.material.R.drawable.ic_menu_black_24dp);

        actionBar.setNavigationIcon(menuIcon);

        actionBar.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
        );

        navigationDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                handleNavigationMenuItem(menuItem);
                drawerLayout.closeDrawers();
                return false;
            }
        });
    }

    private void prepareBackArrow(Toolbar actionBar, final Intent parent) {
        Drawable backArrow = getTintedDrawable(
                actionBar.getContext(),
                io.material.R.drawable.ic_arrow_back_black_24dp);

        actionBar.setNavigationIcon(backArrow);

        actionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getCallingActivity() != null && getCallingActivity().equals(parent.getComponent())) {
                    finish();
                } else {
                    startActivity(parent);
                }
            }
        });

    }

    private void handleNavigationMenuItem(@NonNull MenuItem menuItem) {
        Intent intent;

        switch (menuItem.getItemId()) {
            case R.id.itemSettings:
                intent = new Intent(this,
                        SettingsActivity.class);
                break;

            case R.id.itemInstruction:
                intent = new Intent(this,
                        InstructionActivity.class);
                break;

            case R.id.itemPrivacy:
                intent = new Intent(this,
                        PrivacyStatementActivity.class);
                break;

            case R.id.itemCopyright:
                intent = new Intent(this,
                        CopyrightActivity.class);
                break;

            case R.id.itemImprint:
                intent = new Intent(this,
                        ImprintActivity.class);
                break;

            default:
                // TODO implement action for this item
                Toast.makeText(AppActivity.this, menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                return;
        }

        startActivityForResult(intent, 0);
    }


}
