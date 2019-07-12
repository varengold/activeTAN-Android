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

import android.app.Activity;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

public class TokenSettingsAdapter
        extends RecyclerView.Adapter<TokenSettingsItemHolder>
        implements TokenSettingsItemHolder.TokenSettingsItemListener {
    private List<BankingToken> data;
    private final TokenSettingsListener listener;

    public TokenSettingsAdapter(List<BankingToken> data, TokenSettingsListener listener) {
        this.data = data;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @NonNull
    @Override
    public TokenSettingsItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_token_settings, parent, false);

        return new TokenSettingsItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TokenSettingsItemHolder holder, int position) {
        BankingToken bankingToken = data.get(position);

        // Disable possibly existing listener
        holder.setListener(null, position);

        holder.setSerialNumber(bankingToken.getFormattedSerialNumber());
        holder.setTokenDescription(bankingToken.getDisplayName());
        holder.setProtectUsage(bankingToken.confirmDeviceCredentialsToUse);
        holder.setActiveSince(bankingToken.createdOn);
        holder.setLastUsed(bankingToken.lastUsed);
        holder.setIsUsable(BankingTokenRepository.isUsable(bankingToken));

        holder.setListener(this, position);
    }

    /** For UI updates, notifications must be executed in the main thread */
    private <T> void doOnMainThread(final Consumer<T> consumer, final T arg) {
        // For UI updates, notifications must be executed in the main thread
        Handler handler = ((Activity) listener).getWindow().getDecorView().getHandler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                consumer.accept(arg);
            }
        });
    }


    public void updateItem(BankingToken token) {
        ListIterator<BankingToken> iterator = data.listIterator();
        for (int i = 0; iterator.hasNext(); i++) {
            if (token.id.equals(iterator.next().id)) {
                iterator.set(token);

                doOnMainThread(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer arg) {
                        notifyItemChanged(arg);
                    }
                }, i);

                break;
            }
        }
    }

    public void deleteItem(BankingToken token) {
        ListIterator<BankingToken> iterator = data.listIterator();
        for (int i = 0; iterator.hasNext(); i++) {
            if (token.id.equals(iterator.next().id)) {
                iterator.remove();

                doOnMainThread(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer arg) {
                        notifyItemRemoved(arg);
                    }
                }, i);

                break;
            }
        }
    }

    @Override
    public void onChangeTokenDescriptionButtonClick(int itemPosition) {
        BankingToken token = data.get(itemPosition);
        listener.onChangeTokenDescription(token);
    }

    @Override
    public void onProtectUsageCheckedChange(int itemPosition, boolean isChecked) {
        BankingToken token = data.get(itemPosition);
        listener.onChangeProtectUsage(token, isChecked);
    }

    @Override
    public void onDeleteButtonClick(int itemPosition) {
        BankingToken token = data.get(itemPosition);
        listener.onDeleteToken(token);
    }

    public interface TokenSettingsListener {
        void onChangeTokenDescription(BankingToken token);
        void onChangeProtectUsage(BankingToken token, boolean newValue);
        void onDeleteToken(BankingToken token);
    }
}
