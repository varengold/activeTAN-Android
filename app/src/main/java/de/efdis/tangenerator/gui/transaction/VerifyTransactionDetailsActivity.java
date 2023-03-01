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

package de.efdis.tangenerator.gui.transaction;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.DataElementType;
import de.efdis.tangenerator.activetan.HHDuc;
import de.efdis.tangenerator.activetan.TanGenerator;
import de.efdis.tangenerator.activetan.VisualisationClass;
import de.efdis.tangenerator.api.BankingAppApi;
import de.efdis.tangenerator.databinding.ActivityVerifyTransactionDetailsBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.ErrorDialogBuilder;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

public class VerifyTransactionDetailsActivity
        extends AppActivity
        implements SelectTokenDialogFragment.SelectTokenListener {

    /** Result code to indicate that this activity has been canceled but may be repeated. */
    public static final int RESULT_CODE_REPEAT = RESULT_FIRST_USER + 1;

    /** Result code to indicate that the user has declined the transaction. */
    public static final int RESULT_CODE_DECLINE_TRANSACTION = RESULT_FIRST_USER + 2;

    /** Input parameter with encoded challenge data in format {@link HHDuc}. */
    public static final String EXTRA_RAW_HHDUC = "HHDuc";

    /** (Optional) input parameter with allowed token ids as {@link String} array. */
    public static final String EXTRA_LIMIT_TOKEN_IDS = "LIMIT_TOKEN_IDS";

    /** Output parameter with transaction number as {@link String}. */
    public static final String EXTRA_TAN = "TAN";

    /** (Optional) output parameter with transaction counter as <code>int</code>. */
    public static final String EXTRA_ATC = "ATC";

    /** Output parameter with TAN generator's {@link BankingToken#id} used as {@link String}. */
    public static final String EXTRA_TAN_GENERATOR = "TAN_GENERATOR";

    private static final Set<DataElementType> IBAN_TYPES = new HashSet<>(Arrays.asList(
            DataElementType.IBAN_OWN,
            DataElementType.IBAN_SENDER,
            DataElementType.IBAN_RECIPIENT,
            DataElementType.IBAN_PAYER));

    @StringRes
    private static final int[] VISUALIZATION_CLASS_LABELS = new int[]{
            R.string.VC00, R.string.VC01, R.string.VC02, R.string.VC03, R.string.VC04,
            R.string.VC05, R.string.VC06, R.string.VC07, R.string.VC08, R.string.VC09,
            R.string.VC10, R.string.VC11, R.string.VC12, R.string.VC13, R.string.VC14,
            R.string.VC15, R.string.VC16, R.string.VC17, R.string.VC18, R.string.VC19,
            R.string.VC20, R.string.VC21, R.string.VC22, R.string.VC23, R.string.VC24,
            R.string.VC25, R.string.VC26, R.string.VC27, R.string.VC28, R.string.VC29,
            R.string.VC30, R.string.VC31, R.string.VC32, R.string.VC33, R.string.VC34,
            R.string.VC35, R.string.VC36, R.string.VC37, R.string.VC38, R.string.VC39,
            R.string.VC40, R.string.VC41, R.string.VC42, R.string.VC43, R.string.VC44,
            R.string.VC45, R.string.VC46, R.string.VC47, R.string.VC48, R.string.VC49,
            R.string.VC50, R.string.VC51, R.string.VC52, R.string.VC53, R.string.VC54,
            R.string.VC55, R.string.VC56, R.string.VC57, R.string.VC58, R.string.VC59,
            R.string.VC60, R.string.VC61, R.string.VC62, R.string.VC63, R.string.VC64,
            R.string.VC65, R.string.VC66, R.string.VC67, R.string.VC68, R.string.VC69,
            R.string.VC70, R.string.VC71, R.string.VC72, R.string.VC73, R.string.VC74,
            R.string.VC75, R.string.VC76, R.string.VC77, R.string.VC78, R.string.VC79,
            R.string.VC80, R.string.VC81,
    };
    static {
        if (VISUALIZATION_CLASS_LABELS[81] != R.string.VC81) {
            throw new AssertionError("index error in lookup table");
        }
    }

    @StringRes
    private static final int[] DATA_ELEMENT_LABELS = new int[]{
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            R.string.DE10, R.string.DE11, R.string.DE12, R.string.DE13, R.string.DE14,
            R.string.DE15, R.string.DE16, R.string.DE17, R.string.DE18, R.string.DE19,
            R.string.DE20, R.string.DE21, R.string.DE22, R.string.DE23, R.string.DE24,
            0, R.string.DE26, 0, 0, R.string.DE29,
            0, 0, R.string.DE32, R.string.DE33, 0,
            0, R.string.DE36, R.string.DE37, R.string.DE38, R.string.DE39,
            R.string.DE40, R.string.DE41, R.string.DE42, R.string.DE43, R.string.DE44,
            R.string.DE45, R.string.DE46, R.string.DE47, R.string.DE48, R.string.DE49,
            R.string.DE50, R.string.DE51, R.string.DE52, R.string.DE53, R.string.DE54,
            R.string.DE55, R.string.DE56, R.string.DE57, R.string.DE58, 0,
            0, R.string.DE61, R.string.DE62, R.string.DE63, R.string.DE64,
    };
    static {
        if (DATA_ELEMENT_LABELS[64] != R.string.DE64) {
            throw new AssertionError("index error in lookup table");
        }
    }

    /**
     * Flag, indicating whether this activity has been started from the {@link BankingAppApi}.
     * <p/>
     * If yes, the generated TAN will be returned to the caller instead of being displayed.
     */
    private boolean startedFromApi;

    /** Transaction data from the QR code */
    private byte[] rawHHDuc;

    /**
     * Flag, indicating whether the ATC shall be displayed together with the generated TAN.
     */
    private boolean displayAtc;

    /** The selected TAN generator for TAN computation */
    private BankingToken bankingToken;

    private ActivityVerifyTransactionDetailsBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyTransactionDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            // Illegal call
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Forbid screenshots to prevent leakage of transaction data or the generated TAN
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        startedFromApi = (new ComponentName(getApplicationContext(),
                BankingAppApi.class).equals(getCallingActivity()));

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
                builder.setOnDismissListener(dialog1 -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });
                dialog = builder.create();
            }
            dialog.show();

            return;
        }

        displayAtc = hhduc.isDisplayAtc();

        if (displayAtc) {
            binding.instructionEnterTAN.setText(R.string.enter_tan_and_atc_onlinebanking);
        } else {
            // Don't show ATC and TAN/ATC-Labels
            binding.atcContainer.setVisibility(View.GONE);
            binding.labelTAN.setVisibility(View.GONE);
        }

        if (startedFromApi) {
            // Visualize that we are inside the TAN app,
            // since the user might not have noticed the app change.
            binding.actionBar.setSubtitle(R.string.app_name);
        } else {
            // The ok button only confirms the transaction in this app. It will not release the
            // transaction in the banking app. Thus, the text can be simplified.
            binding.validateButton.setText(R.string.confirm_transaction_details);

            // The cancel button is only needed when started from the banking app.
            // Otherwise, the user may cancel the process easily by not entering the TAN in the
            // other device.
            binding.cancelButton.setVisibility(View.GONE);
        }

        if (hhduc.getVisualisationClass() == null) {
            binding.labelTransactionType.setText(getString(R.string.synchronize_tan_generator));
        } else {
            binding.labelTransactionType.setText(getString(hhduc.getVisualisationClass()));
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
            TextView label = Arrays.asList(binding.labelDataElement1, binding.labelDataElement2, binding.labelDataElement3).get(i);
            TextView content = Arrays.asList(binding.contentDataElement1, binding.contentDataElement2, binding.contentDataElement3).get(i);

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

        if (dataElementTypes.isEmpty()) {
            binding.instructionVerifyData.setText(R.string.verify_transaction_without_details);
        } else {
            binding.instructionVerifyData.setText(R.string.verify_transaction_with_details);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Don't show back icon in the toolbar.
        if (startedFromApi) {
            // If we cancel the process, we will return to the banking app where the QR code
            // will be shown.
            setToolbarNavigationIcon(R.drawable.ic_material_communication_qr_code);
        } else {
            setToolbarNavigationIcon(R.drawable.ic_material_navigation_close);
        }
    }

    private String getString(VisualisationClass visualisationClass) {
        return getString(VISUALIZATION_CLASS_LABELS[visualisationClass.getId()]);
    }

    private String getString(DataElementType dataElementType) {
        return getString(DATA_ELEMENT_LABELS[dataElementType.getId()]);
    }

    public void onButtonCancel(View button) {
        setResult(RESULT_CODE_DECLINE_TRANSACTION);
        finish();
    }

    public void onButtonValidate(View button) {
        // Select token in dialog and continue in onTokenSelected...
        SelectTokenDialogFragment dialog;
        if (getIntent().hasExtra(EXTRA_LIMIT_TOKEN_IDS)) {
            dialog = new SelectTokenDialogFragment(
                    getIntent().getStringArrayExtra(EXTRA_LIMIT_TOKEN_IDS));
        } else {
            dialog = new SelectTokenDialogFragment();
        }
        dialog.show(getSupportFragmentManager(), null);
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
        format.setGroupingUsed(false);
        return format.format(token.transactionCounter);
    }

    @Override
    public void onTokenSelected(BankingToken bankingToken) {
        this.bankingToken = bankingToken;

        try {
            if (!BankingTokenRepository.userMustAuthenticateToUse(bankingToken)) {
                onTokenReadyToUse();
                return;
            }
        } catch (KeyStoreException | InvalidKeyException e) {
            Log.e(getClass().getSimpleName(), "Invalid token selected", e);
        }

        // Confirm credentials
        authenticateUser(R.string.authorize_to_generate_tan,
                new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                onTokenReadyToUse();            }
        });
    }

    private void onTokenReadyToUse() {
        String tan;
        try {
            tan = computeFormattedTan(bankingToken);
        } catch (HHDuc.UnsupportedDataFormatException e) {
            Log.e(getClass().getSimpleName(), "Illegal QR code data, cannot compute TAN", e);
            return;
        } catch (GeneralSecurityException e) {
            Log.e(getClass().getSimpleName(), "Key store error, cannot compute TAN", e);
            onTokenError(e);
            return;
        }

        if (BankingTokenRepository.isExhausted(bankingToken)) {
            binding.exhaustedLabel.setText(R.string.exhausted_generator_label);
            binding.exhaustedDescription.setText(R.string.exhausted_generator_description);
        }

        if (startedFromApi) {
            // When called from the banking app, we do not show the TAN, but return it to the caller
            returnTan(tan);
        } else {
            showTan(tan);
        }
    }

    private void returnTan(String tan) {
        Intent result = new Intent();
        result.putExtra(EXTRA_TAN_GENERATOR, bankingToken.id);
        result.putExtra(EXTRA_TAN, tan);
        if (displayAtc) {
            result.putExtra(EXTRA_ATC, bankingToken.transactionCounter);
        }
        setResult(RESULT_OK, result);

        if (BankingTokenRepository.isSoonExhausted(bankingToken)
                || BankingTokenRepository.isExhausted(bankingToken)) {
            // We need to show a warning before returning to the banking app.
            // This warning would usually be displayed below the TAN,
            // but since we return to the banking app immediately it would be not visible.
            // This, we use a dialog instead.

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(binding.exhaustedLabel.getText());
            builder.setMessage(binding.exhaustedDescription.getText());
            builder.setIcon(binding.exhaustedIcon.getDrawable());

            builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss());

            builder.setOnDismissListener(
                    dialogInterface -> VerifyTransactionDetailsActivity.this.finish()
            );

            builder.show();

            return;
        }

        finish();
    }

    private void showTan(String tan) {
        setTitle(R.string.confirmed_transaction_details_title);

        binding.textTAN.setText(tan);
        binding.textATC.setText(getFormattedTransactionCounter(bankingToken));

        binding.generatedTanContainer.setVisibility(View.VISIBLE);

        binding.instructionVerifyData.setVisibility(View.GONE);
        binding.buttonContainer.setVisibility(View.GONE);

        if (BankingTokenRepository.isSoonExhausted(bankingToken)
                || BankingTokenRepository.isExhausted(bankingToken)) {
            binding.exhaustedGeneratorHintContainer.setVisibility(View.VISIBLE);
        }

        binding.scrollView.post(() -> {
            // Update layout, because the size of TAN/ATC views has changed
            binding.generatedTanContainer.requestLayout();
            binding.exhaustedGeneratorHintContainer.requestLayout();

            // Scroll to end of instructions on small screens.
            // Near the bottom is the TAN, which is to be entered in online banking.
            binding.scrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    private void onTokenError(Exception cause) {
        ErrorDialogBuilder builder = new ErrorDialogBuilder(this);
        builder.setTitle(R.string.token_failed_title);
        builder.setMessage(R.string.token_failed_details);
        builder.setError(cause);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    protected void onRestart() {
        if (startedFromApi) {
            /*
             * When called from the banking app, we want to verify that the TAN challenge is still
             * valid before restarting this activity. We return to the api activity, which will
             * recreate this activity if the TAN challenge is still valid.
             */
            setResult(RESULT_CODE_REPEAT);
            finish();

            // The app crashes if we don't call super.onRestart(), thus no return here.
        }

        super.onRestart();
    }
}
