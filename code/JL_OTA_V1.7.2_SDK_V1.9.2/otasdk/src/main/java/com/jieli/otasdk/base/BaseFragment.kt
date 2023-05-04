package com.jieli.otasdk.base

import com.jieli.component.base.Jl_BaseFragment

/**
 *  create Data:2019-07-24
 *  create by:chensenhua
 *
 **/
open class BaseFragment : Jl_BaseFragment() {

    open fun isValidFragment(): Boolean {
        return isAdded && !isDetached
    }
}