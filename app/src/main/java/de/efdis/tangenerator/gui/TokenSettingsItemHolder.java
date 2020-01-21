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

import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.efdis.tangenerator.R;

public class TokenSettingsItemHolder extends RecyclerView.ViewHolder {
    private View tokenSettingsItem;

    public TokenSettingsItemHolder(View tokenSettingsItem) {
        super(tokenSettingsItem);
        this.tokenSettingsItem = tokenSettingsItem;
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return tokenSettingsItem.findViewById(id);
    }

    public void setListener(final TokenSettingsItemListener listener, final int itemPosition) {
        ImageButton tokenDescriptionButton = findViewById(R.id.tokenDescriptionButton);
        tokenDescriptionButton.setOnClickListener(listener == null ? null : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onChangeTokenDescriptionButtonClick(itemPosition);
            }
        });

        Switch protectUsageSwitch = findViewById(R.id.protectUsageSwitch);
        protectUsageSwitch.setOnCheckedChangeListener(listener == null ? null : new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onProtectUsageCheckedChange(itemPosition, isChecked);
            }
        });

        ImageButton deleteTokenButton = findViewById(R.id.deleteTokenButton);
        deleteTokenButton.setOnClickListener(listener == null ? null : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteButtonClick(itemPosition);
            }
        });
    }

    public void setSerialNumber(String serialNumber) {
        TextView v = findViewById(R.id.serialNumber);
        v.setText(serialNumber);
    }

    public void setTokenDescription(String tokenDescription) {
        TextView v = findViewById(R.id.tokenDescription);
        v.setText(tokenDescription);
    }

    private static String getFormattedDate(Date date) {
        if (date == null) {
            return null;
        }

        DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
        return format.format(date);
    }

    public void setActiveSince(Date activeSince) {
        TextView v = findViewById(R.id.statusActiveSinceDate);
        v.setText(getFormattedDate(activeSince));
    }

    public void setLastUsed(Date lastUsed) {
        TextView v = findViewById(R.id.statusLastUsedDate);
        if (lastUsed == null) {
            v.setText(R.string.last_used_never);
        } else {
            v.setText(getFormattedDate(lastUsed));
        }
    }

    public void setProtectUsage(boolean protectUsage) {
        TextView v = findViewById(R.id.protectUsageDescription);
        v.setText(protectUsage
                ? R.string.do_protect_usage_description
                : R.string.dont_protect_usage_description);

        Switch s = findViewById(R.id.protectUsageSwitch);
        s.setChecked(protectUsage);
    }

    public void setHasValidKey(boolean hasValidKey) {
        TextView label = findViewById(R.id.statusInvalidatedLabel);
        TextView description = findViewById(R.id.statusInvalidatedDescription);
        if (hasValidKey) {
            label.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
        } else {
            label.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
        }
    }

    public void setIsExhausted(boolean isExhausted) {
        TextView label = findViewById(R.id.statusExhaustedLabel);
        TextView description = findViewById(R.id.statusExhaustedDescription);
        if (isExhausted) {
            label.setVisibility(View.VISIBLE);
            description.setVisibility(View.VISIBLE);
        } else {
            label.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
        }
    }


    public interface TokenSettingsItemListener {
        void onChangeTokenDescriptionButtonClick(int itemPosition);
        void onProtectUsageCheckedChange(int itemPosition, boolean isChecked);
        void onDeleteButtonClick(int itemPosition);
    }

}
