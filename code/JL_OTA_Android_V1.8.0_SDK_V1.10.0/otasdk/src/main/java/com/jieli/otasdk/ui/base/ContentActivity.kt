package com.jieli.otasdk.ui.base;

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import com.jieli.component.utils.SystemUtil
import com.jieli.otasdk.R
import com.jieli.otasdk.databinding.ActivityContentBinding

/**
 * @author zqjasonZhong
 * @since 2025/1/13
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通用界面
 */
class ContentActivity : BaseActivity() {
    companion object{
        const val KEY_CLASS_PATH = "class_path"
        const val KEY_BUNDLE = "bundle"

        private const val MIN_START_ACTIVITY_INTERVAL = 500L
        private val fastClickLimit = HashMap<String, Long>()

        fun startContentActivity(
            context: Context,
            fragmentClazzPath: String?,
            bundle: Bundle? = null,
            launcher: ActivityResultLauncher<Intent>? = null
        ) {
            if (null == fragmentClazzPath) return
            if (fastStart(fragmentClazzPath)) return
            val intent = Intent(context, ContentActivity::class.java).also {
                it.putExtra(KEY_CLASS_PATH, fragmentClazzPath)
                bundle?.let { data ->
                    it.putExtra(KEY_BUNDLE, data)
                }
            }
            if (null == launcher) {
                context.startActivity(intent)
            } else {
                launcher.launch(intent)
            }
        }

        private fun fastStart(fragmentClazzPath: String): Boolean {
            val startTime = fastClickLimit[fragmentClazzPath]
            val currentTime = System.currentTimeMillis()
            if (null == startTime) {
                fastClickLimit[fragmentClazzPath] = currentTime
                return false
            }
            if (currentTime - startTime < MIN_START_ACTIVITY_INTERVAL) {
                return true
            }
            fastClickLimit[fragmentClazzPath] = currentTime
            return false
        }
    }

    private lateinit var binding: ActivityContentBinding
    private var fragmentClazzPath: String? = null
    var customExit: (() -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemUtil.setImmersiveStateBar(window, true)
        super.onCreate(savedInstanceState)
        binding = ActivityContentBinding.inflate(layoutInflater)
        setContentView(binding.root);
        val fragmentClazzPath = intent?.getStringExtra(KEY_CLASS_PATH)
        if (null == fragmentClazzPath) {
            finish()
            return
        }
        val bundle = intent?.getBundleExtra(KEY_BUNDLE)
        this.fragmentClazzPath = fragmentClazzPath
        replaceFragment(R.id.main, fragmentClazzPath, bundle, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentClazzPath?.let {
            fastClickLimit.remove(it)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        customExit?.let { method ->
            if (method()) {
                return
            }
        }
        super.onBackPressed()
    }
}