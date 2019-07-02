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

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.DataElementType;
import de.efdis.tangenerator.activetan.HHDuc;
import de.efdis.tangenerator.activetan.TanGenerator;
import de.efdis.tangenerator.activetan.VisualisationClass;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

public class VerifyTransactionDetailsActivity
        extends AppActivity
        implements SelectTokenDialogFragment.SelectTokenListener {

    public static final String EXTRA_RAW_HHDUC = "HHDuc";

    private static final Set<DataElementType> IBAN_TYPES = new HashSet<>(Arrays.asList(
            DataElementType.IBAN_OWN,
            DataElementType.IBAN_SENDER,
            DataElementType.IBAN_RECIPIENT,
            DataElementType.IBAN_PAYER));

    private byte[] rawHHDuc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_transaction_details);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            finish();
            return;
        }

        // Forbid screenshots to prevent leakage of transaction data or the generated TAN
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        rawHHDuc = extras.getByteArray(EXTRA_RAW_HHDUC);

        HHDuc hhduc;
        try {
            hhduc = HHDuc.parse(rawHHDuc);
        } catch (HHDuc.UnsupportedDataFormatException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());

            AlertDialog dialog;
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.unsupported_data_format_title);
                builder.setMessage(R.string.unsupported_data_format_message);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
                dialog = builder.create();
            }
            dialog.show();

            return;
        }

        TextView visualizationClass = findViewById(R.id.labelTransactionType);
        if (hhduc.getVisualisationClass() == null) {
            visualizationClass.setText(getString(R.string.synchronize_tan_generator));

            TextView instructionEnterTAN = findViewById(R.id.instructionEnterTAN);
            instructionEnterTAN.setText(R.string.enter_tan_and_atc_onlinebanking);
        } else {
            visualizationClass.setText(getString(hhduc.getVisualisationClass()));

            // Don't show ATC and TAN/ATC-Labels
            findViewById(R.id.atcContainer).setVisibility(View.GONE);
            findViewById(R.id.labelTAN).setVisibility(View.GONE);
        }

        List<DataElementType> dataElementTypes = hhduc.getDataElementTypes();

        // Do not show empty data elements
        for (Iterator<DataElementType> it = dataElementTypes.iterator(); it.hasNext(); ) {
            if ("".equals(hhduc.getDataElement(it.next()))) {
                it.remove();
            }
        }

        for (int i = 0; i < dataElementTypes.size(); i++) {
            DataElementType type = dataElementTypes.get(i);
            String value = hhduc.getDataElement(type);
            TextView label = (TextView) findViewByName("labelDataElement" + (i + 1));
            TextView content = (TextView) findViewByName("contentDataElement" + (i + 1));

            label.setText(getString(type));

            if (IBAN_TYPES.contains(type)) {
                StringBuilder iban = new StringBuilder();

                for (int blockStart = 0; blockStart < value.length(); blockStart += 4) {
                    if (blockStart > 0) {
                        iban.append(' ');
                    }
                    int blockEnd = Math.min(value.length(), blockStart + 4);
                    iban.append(value.substring(blockStart, blockEnd));
                }

                value = iban.toString();
            }

            if (DataElementType.Format.NUMERIC.equals(type.getFormat())) {
                // Make numbers respect the device's locale
                try {
                    BigDecimal number = new BigDecimal(value.replace(',', '.'));
                    NumberFormat format = NumberFormat.getInstance();
                    format.setMinimumFractionDigits(number.scale());
                    value = format.format(number);
                } catch (NumberFormatException e) {
                    Log.e(getClass().getSimpleName(),
                            "invalid transaction data format", e);
                }
            }

            content.setText(value);
        }

        TextView instructionTAN = findViewById(R.id.instructionVerifyData);
        if (dataElementTypes.isEmpty()) {
            instructionTAN.setText(R.string.verify_transaction_without_details);
        } else {
            instructionTAN.setText(R.string.verify_transaction_with_details);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Don't show back icon in the toolbar.
        setToolbarNavigationIcon(io.material.R.drawable.ic_close_black_24dp);
    }

    private View findViewByName(String name) {
        String packageName = getPackageName();
        int id = getResources().getIdentifier(name, "id", packageName);
        return findViewById(id);
    }

    private String getString(VisualisationClass visualisationClass) {
        String name = String.format(Locale.US, "VC%02d", visualisationClass.getId());
        return getString(name);
    }

    private String getString(DataElementType dataElementType) {
        String name = String.format(Locale.US, "DE%02d", dataElementType.getId());
        return getString(name);
    }

    private String getString(String name) {
        String packageName = getPackageName();
        int resourceId = getResources().getIdentifier(name, "string", packageName);
        return getString(resourceId);
    }

    public void onButtonValidate(View button) {
        // Select token in dialog and continue in onTokenReadyToUse...
        new SelectTokenDialogFragment().show(getSupportFragmentManager(), null);
    }

    private int computeTan(BankingToken token)
            throws HHDuc.UnsupportedDataFormatException, GeneralSecurityException {
        HHDuc hhduc = HHDuc.parse(rawHHDuc);

        BankingTokenRepository.incTransactionCounter(getApplicationContext(), token);
        return TanGenerator.generateTan(token, hhduc);
    }

    private String computeFormattedTan(BankingToken token)
            throws HHDuc.UnsupportedDataFormatException, GeneralSecurityException {
        int tan = computeTan(token);

        return TanGenerator.formatTAN(tan);
    }

    private String getFormattedTransactionCounter(BankingToken token) {
        DecimalFormat format = new DecimalFormat();
        format.setGroupingUsed(true);
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        format.setDecimalFormatSymbols(symbols);

        return format.format(token.transactionCounter);
    }

    @Override
    public void onTokenReadyToUse(BankingToken token) {
        View generatedTanContainer = findViewById(R.id.generatedTanContainer);
        TextView textTAN = findViewById(R.id.textTAN);
        TextView textATC = findViewById(R.id.textATC);

        TextView instructionTAN = findViewById(R.id.instructionVerifyData);
        Button validateButton = findViewById(R.id.button);

        String tan;
        try {
            tan = computeFormattedTan(token);
        } catch (GeneralSecurityException | HHDuc.UnsupportedDataFormatException e) {
            Log.e(getClass().getSimpleName(), "Cannot compute TAN", e);
            return;
        }

        setTitle(R.string.confirmed_transaction_details_title);

        textTAN.setText(tan);
        textATC.setText(getFormattedTransactionCounter(token));

        generatedTanContainer.setVisibility(View.VISIBLE);

        instructionTAN.setVisibility(View.INVISIBLE);
        validateButton.setVisibility(View.INVISIBLE);
    }
}
