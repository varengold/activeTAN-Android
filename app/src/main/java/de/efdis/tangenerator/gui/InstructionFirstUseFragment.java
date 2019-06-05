package de.efdis.tangenerator.gui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.efdis.tangenerator.R;

public class InstructionFirstUseFragment extends AbstractInstructionCardFragment {


    public InstructionFirstUseFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_instruction_first_use, container, false);
    }

}
