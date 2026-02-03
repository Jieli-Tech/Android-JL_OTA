package com.jieli.otasdk.ui.dialog

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.jieli.jl_bt_ota.constant.BluetoothConstant
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.FragmentDialogSeekBarBinding
import com.jieli.otasdk.util.gone
import com.jieli.otasdk.util.show

/**
 * @ClassName: DialogSeekBar
 * @Description: 滑动条弹窗
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/10/21 10:19
 */
class DialogSeekBar private constructor(builder: Builder) : CommonDialog(builder) {

    companion object {
        const val MAX = BluetoothConstant.BLE_MTU_MAX + 3
        const val MIN = BluetoothConstant.BLE_MTU_MIN + 3
    }

    private lateinit var binding: FragmentDialogSeekBarBinding

    override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentDialogSeekBarBinding.inflate(inflater, container, false).also {
            binding = it
            return it.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    fun getProgress(): Int = binding.seekBar.progress

    private fun initUI() {
        if (builder !is Builder) return
        binding.tvDialogTitle.apply {
            if (builder.titleStyle == null) gone() else show()
            builder.titleStyle?.let {
                text = it.text
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.text_color else it.color
                    )
                )
                textSize = (if (it.size == 0) 18 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                it.onClick?.let { listener ->
                    setOnClickListener { view ->
                        listener.onClick(this@DialogSeekBar, view)
                    }
                }
            }
        }
        binding.btnCancel.apply {
            if (builder.cancelBtnStyle == null) gone() else show()
            builder.cancelBtnStyle?.let {
                text = it.text.ifEmpty { getString(R.string.cancel) }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (it.color == 0) R.color.black_66000000 else it.color
                    )
                )
                textSize = (if (it.size == 0) 18 else it.size).toFloat()
                typeface =
                    if (it.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(Typeface.NORMAL)
                gravity = it.gravity
                setOnClickListener { view ->
                    it.onClick?.onClick(this@DialogSeekBar, view)
                }
            }
        }
        binding.btnConfirm.apply {
            if (builder.sureBtnStyle == null) gone() else show()
            builder.sureBtnStyle?.let { sureBtn ->
                text = sureBtn.text.ifEmpty { getString(R.string.confirm) }
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (sureBtn.color == 0) R.color.blue_558CFF else sureBtn.color
                    )
                )
                textSize = (if (sureBtn.size == 0) 18 else sureBtn.size).toFloat()
                typeface =
                    if (sureBtn.isBold) Typeface.DEFAULT_BOLD else Typeface.defaultFromStyle(
                        Typeface.NORMAL
                    )
                gravity = sureBtn.gravity
                setOnClickListener { view ->
                    sureBtn.onClick?.onClick(this@DialogSeekBar, view)
                }
            }
        }
        binding.seekBar.apply {
            builder.progressStyle.let { progressStyle ->
                max = progressStyle.max.toFloat()
                min = progressStyle.min.toFloat()
                setProgress(progressStyle.progress.toFloat())
                binding.tvMax.text = progressStyle.max.toString()
                binding.tvMin.text = progressStyle.min.toString()
            }
        }

        if (builder.cancelBtnStyle != null && builder.sureBtnStyle != null) {
            binding.viewHorizontalLine.show()
            binding.viewLine.show()
            binding.btnCancel.show()
            binding.btnConfirm.show()
        } else if (builder.cancelBtnStyle == null && builder.sureBtnStyle == null) {
            binding.viewHorizontalLine.gone()
            binding.viewLine.gone()
            binding.btnCancel.gone()
            binding.btnConfirm.gone()
        } else {
            binding.viewHorizontalLine.show()
            binding.viewLine.gone()
            if (builder.cancelBtnStyle != null) {
                binding.btnConfirm.gone()
                binding.btnCancel.show()
            }
            if (builder.sureBtnStyle != null) {
                binding.btnCancel.gone()
                binding.btnConfirm.show()
            }
        }
    }

    data class ProgressStyle(
        var progress: Int = MIN,
        var max: Int = MAX,
        var min: Int = MIN
    )

    class Builder : CommonDialog.Builder() {
        var titleStyle: TextStyle? = null
        var cancelBtnStyle: ButtonStyle? = null
        var sureBtnStyle: ButtonStyle? = null
        var progressStyle: ProgressStyle = ProgressStyle()

        init {
            cancelable = false
        }

        fun title(text: String): Builder {
            if (titleStyle == null) {
                titleStyle = TextStyle(text, isBold = true)
            } else {
                titleStyle?.text = text
            }
            return this
        }

        fun cancelBtn(
            text: String = "",
            color: Int = 0,
            isBold: Boolean = true,
            listener: OnViewClick? = null
        ): Builder {
            if (cancelBtnStyle == null) {
                cancelBtnStyle = ButtonStyle(text)
            } else {
                cancelBtnStyle?.text = text
            }
            cancelBtnStyle?.let {
                it.color = color
                it.isBold = isBold
                it.onClick = listener
            }
            return this
        }

        fun confirmBtn(
            text: String = "",
            color: Int = 0,
            isBold: Boolean = true,
            listener: OnViewClick? = null
        ): Builder {
            if (sureBtnStyle == null) {
                sureBtnStyle = ButtonStyle(text)
            } else {
                sureBtnStyle?.text = text
            }
            sureBtnStyle?.let {
                it.color = color
                it.isBold = isBold
                it.onClick = listener
            }
            return this
        }

        fun progress(progress: Int, min: Int = MIN, max: Int = MAX): Builder {
            progressStyle.let {
                it.progress = progress
                it.min = min
                it.max = max
            }
            return this
        }

        override fun build(): DialogSeekBar = DialogSeekBar(this)

    }
}
