package com.reteno;

import androidx.fragment.app.Fragment;

public abstract class BaseFragment extends Fragment {

    public Reteno getReteno() {
        return ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
    }
}
