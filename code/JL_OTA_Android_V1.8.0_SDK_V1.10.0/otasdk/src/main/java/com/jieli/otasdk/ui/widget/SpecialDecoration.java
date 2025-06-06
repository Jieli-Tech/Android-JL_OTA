package com.jieli.otasdk.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.jieli.component.utils.ValueUtil;

import org.jetbrains.annotations.NotNull;

/**
 * create Data:2019-08-20
 * create by:chensenhua
 **/
public class SpecialDecoration extends RecyclerView.ItemDecoration {
    private static final int[] attrs = new int[]{16843284};
    private Drawable mDivider;
    private Drawable mDivider2;
    private int orientation;
    private int dividerHeight = 1;

    public SpecialDecoration(Context context, int orientation) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs);
        Drawable drawable = typedArray.getDrawable(0);
        this.init(orientation, drawable, drawable.getIntrinsicHeight());
        typedArray.recycle();
    }

    public SpecialDecoration(Context context, int orientation, int color, int height) {
        Drawable drawable = new ColorDrawable(color);
        this.init(orientation, drawable, height);
    }

    public SpecialDecoration(Context context, int orientation, Drawable drawable) {
        this.init(orientation, drawable, drawable.getIntrinsicHeight());
    }

    private void init(int orientation, Drawable drawable, int height) {
        this.mDivider = drawable;
        this.orientation = orientation;
        this.dividerHeight = height;
        mDivider2 = new ColorDrawable(Color.WHITE);
    }

    @Override
    public void onDraw(@NotNull Canvas c, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
        if (RecyclerView.HORIZONTAL == this.orientation) {
            this.drawHDeraction(c, parent, state);
        } else {
            this.drawVDeraction(c, parent, state);
        }

    }

    private void drawHDeraction(Canvas c, RecyclerView parent, RecyclerView.State state) {



        int left = parent.getPaddingLeft() + ValueUtil.dp2px(parent.getContext(), 16);
        int right = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; ++i) {
            View child = parent.getChildAt(i);

            int childPos = parent.getChildAdapterPosition(child);
            if (childPos < state.getItemCount() - 1) {
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                int top = child.getBottom() + layoutParams.bottomMargin;
                int bottom = top + this.dividerHeight;
                this.mDivider2.setBounds(0, top, ValueUtil.dp2px(parent.getContext(), 16), bottom);
                this.mDivider2.draw(c);

                this.mDivider.setBounds(left, top, right, bottom);
                this.mDivider.draw(c);
            }
        }



    }


    private void drawVDeraction(Canvas c, RecyclerView parent, RecyclerView.State state) {

        int top = parent.getPaddingTop();
        int bottom = parent.getHeight() - parent.getPaddingBottom();
        int childCount = parent.getChildCount();


        for (int i = 0; i < childCount; ++i) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            int left = child.getRight() + layoutParams.rightMargin;
            int right = left + this.dividerHeight;
            this.mDivider.setBounds(left, top, right, bottom);
            this.mDivider.draw(c);
        }


     }

    public void getItemOffsets(@NotNull Rect outRect, @NotNull View view, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {


        if (mDivider == null) {
            outRect.set(0, 0, 0, 0);
            return;
        }


        if (RecyclerView.HORIZONTAL == this.orientation) {
//            outRect.set(0, 0, 0, this.dividerHeight);
            int lastPosition = state.getItemCount() - 1;
            int position = parent.getChildAdapterPosition(view);
             if (position < lastPosition) {
                outRect.set(0, 0, 0, this.dividerHeight);
            } else {
                outRect.set(0, 0, 0, 0);
            }

        } else {
              outRect.set(0, 0, this.dividerHeight, 0);
        }


    }
}
