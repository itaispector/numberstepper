package ezy.ui.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import ezy.library.numberstepper.R;


public class NumberStepper extends LinearLayout {

    private double mStep = 1.0, mValue = 0.0;
    private double mMaxValue = 0.0, mMinValue = 0.0;
    ImageView btnLeft, btnRight;
    EditText txtValue;
    OnValueChangedListener mOnValueChanged;

    public NumberStepper(Context context) {
        this(context, null);
    }

    public NumberStepper(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NumberStepper(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberStepper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(double step, double min, double max, double value) {

        if (min == max) {
            txtValue.setFocusable(false);
            txtValue.setFocusableInTouchMode(false);
            mMinValue = max;
            mMaxValue = max;
        } else {
            txtValue.setFocusable(true);
            txtValue.setFocusableInTouchMode(true);
            mMinValue = Math.min(min, max);
            mMaxValue = Math.max(min, max);
        }

        mStep = Math.max(step, 1.0);
        if (mStep != 1.0) {
            mMinValue = normalize(mMinValue);
            mMaxValue = normalize(mMaxValue);
        }

        mValue = Integer.MAX_VALUE;
        setValue(value, false);
    }


    private static int dp2px(DisplayMetrics dm, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberStepper);

        int buttonSize = dp2px(Resources.getSystem().getDisplayMetrics(), 35);

        try {
            buttonSize = (int) a.getDimension(R.styleable.NumberStepper_nsButtonSize, buttonSize);
            mStep = a.getFloat(R.styleable.NumberStepper_nsStep, 1.f);
            mValue = a.getFloat(R.styleable.NumberStepper_nsValue, 0.f);
            mMinValue = a.getFloat(R.styleable.NumberStepper_nsMinValue, Float.MIN_VALUE);
            mMaxValue = a.getFloat(R.styleable.NumberStepper_nsMaxValue, Float.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            a.recycle();
        }
        setOrientation(HORIZONTAL);
        setFocusableInTouchMode(true);

        btnLeft = new ImageView(context, null, R.attr.nsStyleLeft);
        btnRight = new ImageView(context, null, R.attr.nsStyleRight);
        txtValue = new EditText(context, null, R.attr.nsStyleValue);
        txtValue.setFocusableInTouchMode(true);
        txtValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        txtValue.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        txtValue.setImeOptions(EditorInfo.IME_ACTION_DONE);
        txtValue.setSelectAllOnFocus(true);

        init(mStep, mMinValue, mMaxValue, mValue);

        addView(btnLeft, new LayoutParams(buttonSize, buttonSize));
        addView(txtValue, new LayoutParams(buttonSize, buttonSize, 1));
        addView(btnRight, new LayoutParams(buttonSize, buttonSize));


        btnLeft.setOnClickListener(onClick);
        btnRight.setOnClickListener(onClick);
        txtValue.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mKeyboardObserver.register(v);
                } else {
                    syncValue();
                    mKeyboardObserver.unregister();
                }
            }
        });
        txtValue.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {

            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                mKeyboardObserver.unregister();
            }
        });
        mKeyboardObserver.listen(new OnKeyboardVisibleListener() {
            @Override
            public void onVisibleChanged(boolean visible) {
                if (!visible) {
                    syncValue();
                }
            }
        });

    }

    public void setOnValueChangedListener(OnValueChangedListener onValueChanged) {
        mOnValueChanged = onValueChanged;
    }

    private void syncValue() {
        setValue(editableValue(), true);
    }

    private double editableValue() {
        Editable value = txtValue.getText();
        return value.length() == 0 ? 0.0 : Double.valueOf(value.toString());
    }

    public void notifyValueChanged() {
        if (mOnValueChanged != null) {
            mOnValueChanged.onValueChanged(this, mValue);
        }
    }
    public double getValue() {
        if (txtValue.hasFocus()) {
            txtValue.clearFocus();
        }
        return mValue;
    }

    public void setValue(double value) {
        setValue(value, true);
    }

    public void setValue(double value, boolean notifyValueChanged) {
        if (value == mValue) {
            return;
        }
        double valid = Math.min(Math.max(normalize(value), mMinValue), mMaxValue);
        txtValue.setText(String.valueOf(valid));
        if (valid == mValue) { // valid != value
            return;
        }
        mValue = valid;
        btnLeft.setEnabled(mValue != mMinValue);
        btnRight.setEnabled(mValue != mMaxValue);
        if (notifyValueChanged && mOnValueChanged != null) {
            mOnValueChanged.onValueChanged(this, mValue);
        }
    }

    private double normalize(double value) {
        return value - value % mStep;
    }

    OnClickListener onClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            double value = editableValue();
            setValue(btnLeft == v ? (value - mStep) : (value + mStep), true);
        }
    };


    KeyboardObserver mKeyboardObserver = new KeyboardObserver();

    private class KeyboardObserver implements ViewTreeObserver.OnGlobalLayoutListener {
        private View root;
        private OnKeyboardVisibleListener listener;

        public KeyboardObserver listen(OnKeyboardVisibleListener l) {
            listener = l;
            return this;
        }
        private boolean isKeyboardVisible(View root) {
            final int softKeyboardHeight = 100;
            Rect rect = new Rect();
            root.getWindowVisibleDisplayFrame(rect);
            DisplayMetrics dm = root.getResources().getDisplayMetrics();
            int diff = root.getBottom() - rect.bottom;
            return diff > softKeyboardHeight * dm.density;
        }
        @Override
        public void onGlobalLayout() {
            if (listener != null) {
                listener.onVisibleChanged(isKeyboardVisible(root));
            }
        }

        public void register(View v) {
            unregister();
            root = v.getRootView();
            root.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        public void unregister() {
            if (root != null) {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                root = null;
            }
        }
    }

    interface OnKeyboardVisibleListener {
        void onVisibleChanged(boolean visible);
    }

    public interface OnValueChangedListener {
        void onValueChanged(NumberStepper view, double value);
    }
}


