package com.reteno.sample

import androidx.fragment.app.Fragment
import com.reteno.core.Reteno

abstract class BaseFragment : Fragment() {
    val reteno: Reteno
        get() = (requireActivity().application as SampleApp).getRetenoInstance()
}
