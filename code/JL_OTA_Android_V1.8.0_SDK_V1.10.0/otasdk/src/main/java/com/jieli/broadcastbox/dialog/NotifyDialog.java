package com.jieli.broadcastbox.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.otasdk.R;
import com.jieli.otasdk.ui.base.BaseDialogFragment;

/**
 * Des:
 * author: Bob
 * date: 2022/12/12
 * Copyright: Jieli Technology
 * Modify date:
 * Modified by:
 */
public final class NotifyDialog extends BaseDialogFragment implements View.OnClickListener,
        DialogInterface.OnKeyListener {
    private final String tag = getClass().getSimpleName();
    private Button btnPositive;
    private Button btnNegative;
    private Button btnNeutral;
    private View.OnClickListener positiveButtonListener = null;
    private View.OnClickListener negativeButtonListener = null;
    private View.OnClickListener neutralButtonListener = null;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener = null;
    private DialogInterface.OnDismissListener onDismissListener;
    private DialogInterface.OnCancelListener onCancelListener;
    private CharSequence message = null;
    private CharSequence checkBoxText = null;
    private CharSequence negativeButtonText = null;
    private CharSequence positiveButtonText = null;
    private CharSequence neutralButtonText = null;
    private int imageResId = 0;
    private boolean hasProgressBar = false;
    private boolean hasHorizontalProgressBar = false;
    private boolean cancelable = false;
    private int contentGravity;

    @Override
    public void onStart() {
        super.onStart();
        if(getDialog() == null) return;
        Window window = getDialog().getWindow();
        if(window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));// 设置背景透明
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.5f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(windowParams);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getDialog() == null || getDialog().getWindow() == null) return;
        final WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();

        params.width = 100;
        params.height = 50;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.width = displayMetrics.heightPixels * 4 / 5;
            params.height = displayMetrics.heightPixels * 3 / 4;
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            int w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 146, getResources().getDisplayMetrics());
            int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
            params.width = displayMetrics.widthPixels * 3 / 5;
            params.height = displayMetrics.widthPixels * 2 / 5;
        }
        JL_Log.e(tag, "params.width=" + params.width + ", params.height=" + params.height);
        params.gravity = Gravity.CENTER;
        getDialog().getWindow().setAttributes(params);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notify_dialog, container, false);
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        setCancelable(cancelable);

        TextView messageView = view.findViewById(R.id.tv_content);
        messageView.setMovementMethod(ScrollingMovementMethod.getInstance());
        ProgressBar progressBar = view.findViewById(R.id.pb_circle_bar);
        ProgressBar horizontalProgressBar = view.findViewById(R.id.pb_horizon_bar);
        CheckBox checkBox = view.findViewById(R.id.checkbox);
        ImageView imageView = view.findViewById(R.id.iv_image);

        btnPositive = view.findViewById(R.id.tv_right);
        btnNegative = view.findViewById(R.id.tv_left);
        btnNeutral = view.findViewById(R.id.tv_middle);

        View dividerView = view.findViewById(R.id.divider_id);
        View lineView = view.findViewById(R.id.line_id);

        setupCheckBox(checkBox);
        setupProgressBar(progressBar, horizontalProgressBar);
        setupImageView(imageView, imageResId);
        setupContent(messageView, message);
        setupNeutralButton(btnNeutral, lineView, dividerView);
        setupButton(btnPositive, positiveButtonText);
        setupButton(btnNegative, negativeButtonText);
        getDialog().setOnKeyListener(this);
        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (onCancelListener != null) {
                onCancelListener.onCancel(dialog);
                dismiss();
                return true;
            }
        }
        return false;
    }

    private void setupImageView(ImageView imageView, int imageResId) {
        if (imageView == null) return;
        if (imageResId != 0) {
            imageView.setImageResource(imageResId);
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    private void setupButton(Button button, CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            button.setText(text);
            button.setOnClickListener(this);
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.GONE);
        }
    }

    private void setupContent(TextView messageView, CharSequence message) {
        if (!TextUtils.isEmpty(message)) {
            messageView.setText(message);
            messageView.setVisibility(View.VISIBLE);
            messageView.setGravity(contentGravity);
        } else {
            messageView.setVisibility(View.GONE);
        }
    }

    private void setupCheckBox(CheckBox checkBox) {
        if (!TextUtils.isEmpty(checkBoxText) && onCheckedChangeListener != null) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setText(checkBoxText);
            checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
        } else {
            checkBox.setVisibility(View.GONE);
        }
    }

    private void setupNeutralButton(Button neutralButton, View lineView, View dividerView) {
        if (!TextUtils.isEmpty(neutralButtonText)) {
            lineView.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.GONE);
            neutralButton.setText(neutralButtonText);
            neutralButton.setOnClickListener(this);
            neutralButton.setVisibility(View.VISIBLE);
        } else {
            neutralButton.setVisibility(View.GONE);
            JL_Log.e(tag, "hasProgressBar " + hasProgressBar + ", " + hasHorizontalProgressBar + ", " + imageResId);
            if (hasProgressBar || hasHorizontalProgressBar || imageResId != 0) {
                lineView.setVisibility(View.GONE);
                dividerView.setVisibility(View.GONE);
                JL_Log.e(tag, "33333333");
            } else {
                JL_Log.e(tag, "444444444444");
                lineView.setVisibility(View.VISIBLE);
                dividerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupProgressBar(ProgressBar circleProgressBar, ProgressBar horizontalProgressBar) {
        if (hasProgressBar || hasHorizontalProgressBar) {
            if (hasProgressBar) {
                circleProgressBar.setVisibility(View.VISIBLE);
                horizontalProgressBar.setVisibility(View.GONE);
            } else {
                circleProgressBar.setVisibility(View.GONE);
                horizontalProgressBar.setVisibility(View.VISIBLE);
            }
        } else {
            circleProgressBar.setVisibility(View.GONE);
            horizontalProgressBar.setVisibility(View.GONE);
        }
    }

    public void setProgressBarVisibility(int visibility) {
        if (getView() != null) {
            (getView().findViewById(R.id.progressBar)).setVisibility(visibility);
        }
    }

    @Override
    public void onClick(View v) {
        dismiss();
        if (v == btnPositive) {
            if (positiveButtonListener != null) {
                positiveButtonListener.onClick(v);
            }
        } else if (v == btnNegative) {
            if (negativeButtonListener != null) {
                negativeButtonListener.onClick(v);
            }
        } else if (v == btnNeutral) {
            if (neutralButtonListener != null) {
                neutralButtonListener.onClick(v);
            }
        }
    }

    public static class Builder {
        private final Context mContext;
        private CharSequence mTitle = null;
        private CharSequence mMessage = null;
        private CharSequence mPositiveButton = null;
        private View.OnClickListener  mPositiveButtonListener = null;
        private CharSequence negativeButton = null;
        private View.OnClickListener mNegativeButtonListener = null;
        private CharSequence mNeutralButtonText = null;
        private View.OnClickListener mNeutralButtonListener = null;
        private CharSequence mCheckBoxText = null;
        private CompoundButton.OnCheckedChangeListener mCheckedBoxChangeListener = null;
        private boolean hasProgressBar = false;
        private boolean hasHorizontalProgressBar = false;
        private boolean mCancelable = false;
        private int mContentGravity = Gravity.CENTER;
        private int imageResId = 0;
        private DialogInterface.OnDismissListener mOnDismissListener = null;
        private DialogInterface.OnCancelListener mOnCancelListener;
        public Builder(Context context) {
            this.mContext = context;
        }

        public NotifyDialog create() {
            final NotifyDialog dialog = new NotifyDialog();
            dialog.message = mMessage;
            dialog.hasProgressBar = hasProgressBar;
            dialog.cancelable = mCancelable;
            dialog.hasHorizontalProgressBar = hasHorizontalProgressBar;
            dialog.positiveButtonListener = mPositiveButtonListener;
            dialog.negativeButtonListener = mNegativeButtonListener;
            dialog.onCheckedChangeListener = mCheckedBoxChangeListener;
            dialog.neutralButtonListener = mNeutralButtonListener;
            dialog.onDismissListener = mOnDismissListener;
            dialog.onCancelListener = mOnCancelListener;
            dialog.negativeButtonText = negativeButton;
            dialog.neutralButtonText = mNeutralButtonText;
            dialog.positiveButtonText = mPositiveButton;
            dialog.checkBoxText = mCheckBoxText;
            dialog.contentGravity = mContentGravity;
            dialog.imageResId = imageResId;
            return dialog;
        }

        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setTitle(@StringRes int titleId) {
            mTitle = mContext.getText(titleId);
            return this;
        }

        public Builder setMessage(CharSequence message) {
            mMessage = message;
            return this;
        }

        public Builder setMessage(@StringRes int messageId) {
            mMessage = mContext.getText(messageId);
            return this;
        }

        public Builder setImageResource(int resId) {
            imageResId = resId;
            return this;
        }

        public Builder setPositiveButton(@StringRes int textId, final View.OnClickListener listener) {
            mPositiveButton = mContext.getText(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, final View.OnClickListener listener) {
            mPositiveButton = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(@StringRes int textId, final View.OnClickListener listener) {
            negativeButton = mContext.getText(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, final View.OnClickListener listener) {
            negativeButton = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(@StringRes int textId, final View.OnClickListener listener) {
            mNeutralButtonText = mContext.getText(textId);
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setNeutralButton(CharSequence text, final View.OnClickListener listener) {
            mNeutralButtonText = text;
            mNeutralButtonListener = listener;
            return this;
        }

        public Builder setCheckBoxButton(CharSequence text,
                                                CompoundButton.OnCheckedChangeListener listener) {
            mCheckBoxText = text;
            mCheckedBoxChangeListener = listener;
            return this;
        }

        public Builder setCheckBoxButton(@StringRes int textId,
                                                CompoundButton.OnCheckedChangeListener listener) {
            mCheckBoxText = mContext.getText(textId);
            mCheckedBoxChangeListener = listener;
            return this;
        }

        public Builder enableProgressBar(boolean enable) {
            hasProgressBar = enable;
            return this;
        }

        private Builder enableHorizonProgressBar(boolean enable) {
            hasHorizontalProgressBar = enable;
            return this;
        }

        public Builder setContentGravity(int gravity) {
            mContentGravity = gravity;
            return this;
        }

        public Builder setDismissListener(DialogInterface.OnDismissListener listener) {
            mOnDismissListener = listener;
            return this;
        }

        public Builder setCancelListener(DialogInterface.OnCancelListener listener) {
            mOnCancelListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }
    }
}
