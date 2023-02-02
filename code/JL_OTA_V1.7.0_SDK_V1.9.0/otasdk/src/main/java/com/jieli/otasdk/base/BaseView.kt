package com.jieli.otasdk.base

/**
 * 展示窗口基类
 *
 * @author zqjasonZhong
 * @date 2019/12/30
 */
interface BaseView<T : BasePresenter>{

    fun setPresenter(presenter: T)
}