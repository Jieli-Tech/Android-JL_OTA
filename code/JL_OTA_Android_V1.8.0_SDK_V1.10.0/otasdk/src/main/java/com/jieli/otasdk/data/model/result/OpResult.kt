package com.jieli.otasdk.data.model.result

/**
 * OpResult
 * @author zqjasonZhong
 * @since 2025/4/10
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 操作结果
 */
open class OpResult<T>(
    var op: Int = 0,
    var code: Int = -1,
    var message: String = "",
    var data: T? = null
) {

    fun isSuccess(): Boolean = code == 0

    override fun toString(): String {
        return "OpResult(op=$op, code=$code, message='$message', data=$data)"
    }
}