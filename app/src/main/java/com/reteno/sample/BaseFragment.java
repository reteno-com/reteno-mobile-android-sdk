package com.reteno.sample;

import androidx.fragment.app.Fragment;

import com.reteno.core.Reteno;

public abstract class BaseFragment extends Fragment {

    public Reteno getReteno() {
        return ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
    }
}
