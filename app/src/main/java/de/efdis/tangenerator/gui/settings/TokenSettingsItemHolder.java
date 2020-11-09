/*
 * Copyright (c) 2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

package de.efdis.tangenerator.gui.settings;

import android.view.View;
import android.widget.CompoundButton;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ItemTokenSettingsBinding;

public class TokenSettingsItemHolder extends RecyclerView.ViewHolder {
    private ItemTokenSettingsBinding binding;

    public TokenSettingsItemHolder(View tokenSettingsItem) {
        super(tokenSettingsItem);
        binding = ItemTokenSettingsBinding.bind(tokenSettingsItem);
    }

    public void setListener(final TokenSettingsItemListener listener, final int itemPosition) {
        binding.tokenDescriptionButton.setOnClickListener(listener == null ? null : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onChangeTokenDescriptionButtonClick(itemPosition);
            }
        });

        binding.protectUsageSwitch.setOnCheckedChangeListener(listener == null ? null : new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                listener.onProtectUsageCheckedChange(itemPosition, isChecked);
            }
        });

        binding.deleteTokenButton.setOnClickListener(listener == null ? null : new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteButtonClick(itemPosition);
            }
        });
    }

    public void setSerialNumber(String serialNumber) {
        binding.serialNumber.setText(serialNumber);
    }

    public void setTokenDescription(String tokenDescription) {
        binding.tokenDescription.setText(tokenDescription);
    }

    private static String getFormattedDate(Date date) {
        if (date == null) {
            return null;
        }

        DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
        return format.format(date);
    }

    public void setActiveSince(Date activeSince) {
        binding.statusActiveSinceDate.setText(getFormattedDate(activeSince));
    }

    public void setLastUsed(Date lastUsed) {
        if (lastUsed == null) {
            binding.statusLastUsedDate.setText(R.string.last_used_never);
        } else {
            binding.statusLastUsedDate.setText(getFormattedDate(lastUsed));
        }
    }

    public void setProtectUsage(boolean protectUsage) {
        binding.protectUsageDescription.setText(protectUsage
                ? R.string.do_protect_usage_description
                : R.string.dont_protect_usage_description);

        binding.protectUsageSwitch.setChecked(protectUsage);
    }

    public void setHasValidKey(boolean hasValidKey) {
        if (hasValidKey) {
            binding.statusInvalidatedLabel.setVisibility(View.GONE);
            binding.statusInvalidatedDescription.setVisibility(View.GONE);
        } else {
            binding.statusInvalidatedLabel.setVisibility(View.VISIBLE);
            binding.statusInvalidatedDescription.setVisibility(View.VISIBLE);
        }
    }

    public void setIsExhausted(boolean isExhausted) {
        if (isExhausted) {
            binding.statusExhaustedLabel.setVisibility(View.VISIBLE);
            binding.statusExhaustedDescription.setVisibility(View.VISIBLE);
        } else {
            binding.statusExhaustedLabel.setVisibility(View.GONE);
            binding.statusExhaustedDescription.setVisibility(View.GONE);
        }
    }


    public interface TokenSettingsItemListener {
        void onChangeTokenDescriptionButtonClick(int itemPosition);
        void onProtectUsageCheckedChange(int itemPosition, boolean isChecked);
        void onDeleteButtonClick(int itemPosition);
    }

}
