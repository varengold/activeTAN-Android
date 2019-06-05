package de.efdis.tangenerator.gui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;

public abstract class AbstractInstructionCardFragment extends Fragment {

    private ImageView image;
    private View body;
    private Button toggleButton;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        body = getView().findViewById(R.id.cardBody);
        toggleButton = getView().findViewById(R.id.cardToggleButton);
        image = getView().findViewById(R.id.cardImage);

        if (toggleButton != null && body != null) {
            body.setVisibility(View.GONE);

            // Use the button to toggle visibility of the card's body
            toggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View cardToggleButton) {
                    toggleBodyVisibility();
                }
            });

            // Also allow to show the body by clicking anywhere at the card
            getView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View card) {
                    if (body.getVisibility() == View.GONE) {
                        toggleBodyVisibility();
                    }
                }
            });
        }
    }

    private void toggleBodyVisibility() {
        if (body.getVisibility() == View.GONE) {
            image.setVisibility(View.GONE);
            body.setVisibility(View.VISIBLE);
            toggleButton.setBackgroundResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
        } else {
            body.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            toggleButton.setBackgroundResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
        }
    }
}
