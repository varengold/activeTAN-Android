package de.efdis.tangenerator.gui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.efdis.tangenerator.R;

public class InstructionPrimaryUseFragment extends AbstractInstructionCardFragment {


    public InstructionPrimaryUseFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_instruction_primary_use, container, false);
    }

}
