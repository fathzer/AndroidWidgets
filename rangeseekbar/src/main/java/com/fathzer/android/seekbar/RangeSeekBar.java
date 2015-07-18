package com.fathzer.android.seekbar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

/**
 * Widget that lets users select a minimum and maximum value on a given range.
 * <br>
 * Improved {@link MotionEvent} handling for smoother use, anti-aliased painting for improved aesthetics.
 * @author Jean-Marc Astesana
 * Based on an original work of
 * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
 * @author Peter Sinnott (psinnott@gmail.com)
 * @author Thomas Barrasso (tbarrasso@sevenplusandroid.org)
 *
 */
public class RangeSeekBar extends ImageView {
	private static final String MAX = "MAX";
	private static final String MIN = "MIN";
	private static final String SUPER = "SUPER";
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Bitmap thumbImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
	private final Bitmap thumbPressedImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_pressed);
	private final float thumbWidth = thumbImage.getWidth();
	private final float thumbHalfWidth = 0.5f * thumbWidth;
	private final float thumbHalfHeight = 0.5f * thumbImage.getHeight();
	private final float lineHeight = 0.3f * thumbHalfHeight;
	private final float padding = thumbHalfWidth;
	private int absoluteMinValue, absoluteMaxValue;
	private int normalizedMinValue, normalizedMaxValue;
	private Thumb pressedThumb = null;
	private boolean notifyWhileDragging = false;
	private OnRangeSeekBarChangeListener listener;

	/**
	 * Default color of a {@link RangeSeekBar}, #FF33B5E5. This is also known as
	 * "Ice Cream Sandwich" blue.
	 */
	public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);

	/**
	 * An invalid pointer id.
	 */
	public static final int INVALID_POINTER_ID = 255;

	// Localized constants from MotionEvent for compatibility
	// with API < 8 "Froyo".
	public static final int ACTION_POINTER_UP = 0x6, ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;

	private float mDownMotionX;
	private int mActivePointerId = INVALID_POINTER_ID;

	private int mScaledTouchSlop;
	private boolean mIsDragging;

	public RangeSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(0,100);
	}

	/**
	 * Creates a new RangeSeekBar.
	 *
	 * @param absoluteMinValue
	 *          The minimum value of the selectable range.
	 * @param absoluteMaxValue
	 *          The maximum value of the selectable range.
	 * @param context The context
	 * @throws IllegalArgumentException
	 *           Will be thrown if min/max value type is not one of Long, Double,
	 *           Integer, Float, Short, Byte or BigDecimal.
	 */
	public RangeSeekBar(int absoluteMinValue, int absoluteMaxValue, Context context) {
		super(context);
		init(absoluteMinValue, absoluteMaxValue);
	}

	private void init(int absoluteMinValue, int absoluteMaxValue) {
		this.absoluteMinValue = absoluteMinValue;
		this.absoluteMaxValue = absoluteMaxValue;
		normalizedMinValue = absoluteMinValue;
		normalizedMaxValue = absoluteMaxValue;

		// make RangeSeekBar focusable. This solves focus handling issues in case
		// EditText widgets are being used along with the RangeSeekBar within
		// ScollViews.
		setFocusable(true);
		setFocusableInTouchMode(true);
		mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	public boolean isNotifyWhileDragging() {
		return notifyWhileDragging;
	}

	/**
	 * Should the widget notify the listener callback while the user is still
	 * dragging a thumb? Default is false.
	 * @param flag true to activate notification while dragging
	 */
	public void setNotifyWhileDragging(boolean flag) {
		this.notifyWhileDragging = flag;
	}

	/**
	 * Returns the absolute minimum value of the range that has been set at
	 * construction time.
	 *
	 * @return The absolute minimum value of the range.
	 */
	public int getAbsoluteMinValue() {
		return absoluteMinValue;
	}

	/**
	 * Returns the absolute maximum value of the range that has been set at
	 * construction time.
	 *
	 * @return The absolute maximum value of the range.
	 */
	public int getAbsoluteMaxValue() {
		return absoluteMaxValue;
	}

	/**
	 * Returns the currently selected min value.
	 *
	 * @return The currently selected min value.
	 */
	public int getSelectedMinValue() {
		return normalizedMinValue;
	}

	/**
	 * Sets the currently selected minimum value. The widget will be invalidated
	 * and redrawn.
	 *
	 * @param value
	 *          The Number value to set the minimum value to. Will be clamped to
	 *          given absolute minimum/maximum range.
	 */
	public void setSelectedMinValue(int value) {
		if (value<getAbsoluteMinValue()) {
			throw new IllegalArgumentException("Value ("+value+") can't be less than "+getAbsoluteMinValue());
		}
		normalizedMinValue = value;
		if (value>normalizedMaxValue) {
			normalizedMaxValue = value;
		}
		invalidate();
	}

	/**
	 * Returns the currently selected max value.
	 *
	 * @return The currently selected max value.
	 */
	public int getSelectedMaxValue() {
		return normalizedMaxValue;
	}

	/**
	 * Sets the currently selected maximum value. The widget will be invalidated
	 * and redrawn.
	 *
	 * @param value
	 *          The Number value to set the maximum value to. Will be clamped to
	 *          given absolute minimum/maximum range.
	 */
	public void setSelectedMaxValue(int value) {
		if (value>getAbsoluteMaxValue()) {
			throw new IllegalArgumentException("Value ("+value+") can't be more than "+getAbsoluteMaxValue());
		}
		normalizedMaxValue = value;
		if (value<normalizedMinValue) {
			normalizedMinValue = value;
		}
		invalidate();
	}

	/**
	 * Registers given listener callback to notify about changed selected values.
	 *
	 * @param listener
	 *          The listener to notify about changed selected values.
	 */
	public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
		this.listener = listener;
	}

	/**
	 * Handles thumb selection and movement.
	 * <br>Notifies listener callback on certain events.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}

		final int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				return doActionDown(event);
			case MotionEvent.ACTION_MOVE:
				doActionMove(event);
				break;
			case MotionEvent.ACTION_UP:
				doActionUp(event);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				doPointerDown(event);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				doPointerUp(event);
				break;
			case MotionEvent.ACTION_CANCEL:
				doActionCancel();
				break;
			default :
		}
		return true;
	}

	private void doActionCancel() {
		if (mIsDragging) {
			onStopTrackingTouch();
			setPressed(false);
		}
		invalidate();
	}

	private void doPointerUp(MotionEvent event) {
		onSecondaryPointerUp(event);
		invalidate();
	}

	private void doPointerDown(MotionEvent event) {
		final int index = event.getPointerCount() - 1;
		// final int index = ev.getActionIndex();
		mDownMotionX = event.getX(index);
		mActivePointerId = event.getPointerId(index);
		invalidate();
	}

	private void doActionUp(MotionEvent event) {
		if (mIsDragging) {
			trackTouchEvent(event);
			onStopTrackingTouch();
			setPressed(false);
		} else {
			// Touch up when we never crossed the touch slop threshold
			// should be interpreted as a tap-seek to that location.
			onStartTrackingTouch();
			trackTouchEvent(event);
			onStopTrackingTouch();
		}
		pressedThumb = null;
		invalidate();
		if (listener != null) {
			listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
		}
	}

	private void doActionMove(MotionEvent event) {
		if (pressedThumb != null) {
			if (mIsDragging) {
				trackTouchEvent(event);
			} else {
				// Scroll to follow the motion event
				int pointerIndex = event.findPointerIndex(mActivePointerId);
				final float x = event.getX(pointerIndex);
				if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
					setPressed(true);
					invalidate();
					onStartTrackingTouch();
					trackTouchEvent(event);
					attemptClaimDrag();
				}
			}
			if (notifyWhileDragging && listener != null) {
				listener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue());
			}
		}
	}

	private boolean doActionDown(MotionEvent event) {
		boolean result = true;
		// Remember where the motion event started
		mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
		int pointerIndex = event.findPointerIndex(mActivePointerId);
		mDownMotionX = event.getX(pointerIndex);
		pressedThumb = evalPressedThumb(mDownMotionX);
		// Only handle thumb presses.
		if (pressedThumb == null) {
			result = super.onTouchEvent(event);
		} else {
			setPressed(true);
			invalidate();
			onStartTrackingTouch();
			trackTouchEvent(event);
			attemptClaimDrag();
		}
		return result;
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose
			// a new active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mDownMotionX = ev.getX(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
		}
	}

	private void trackTouchEvent(MotionEvent event) {
		final int pointerIndex = event.findPointerIndex(mActivePointerId);
		final float x = event.getX(pointerIndex);

		if (Thumb.MIN.equals(pressedThumb)) {
			setSelectedMinValue(screenToValue(x));
		} else if (Thumb.MAX.equals(pressedThumb)) {
			setSelectedMaxValue(screenToValue(x));
		}
	}

	/**
	 * Tries to claim the user's drag motion, and requests disallowing any
	 * ancestors from stealing events in the drag.
	 */
	private void attemptClaimDrag() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}

	/**
	 * This is called when the user has started touching this widget.
	 */
	void onStartTrackingTouch() {
		mIsDragging = true;
	}

	/**
	 * This is called when the user either releases his touch or the touch is
	 * canceled.
	 */
	void onStopTrackingTouch() {
		mIsDragging = false;
	}

	/**
	 * Ensures correct size of the widget.
	 */
	@Override
	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = 200;
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
			width = MeasureSpec.getSize(widthMeasureSpec);
		}
		int height = thumbImage.getHeight();
		if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
			height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
		}
		setMeasuredDimension(width, height);
	}

	/**
	 * Draws the widget on the given canvas.
	 */
	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// draw seek bar background line
		final RectF rect = new RectF(padding, 0.5f * (getHeight() - lineHeight), getWidth() - padding,
				0.5f * (getHeight() + lineHeight));
		paint.setStyle(Style.FILL);
		paint.setColor(Color.GRAY);
		paint.setAntiAlias(true);
		canvas.drawRect(rect, paint);

		// draw seek bar active range line
		rect.left = normalizedToScreen(normalizedMinValue);
		rect.right = normalizedToScreen(normalizedMaxValue);

		// orange color
		paint.setColor(DEFAULT_COLOR);
		canvas.drawRect(rect, paint);

		// draw minimum thumb
		drawThumb(normalizedToScreen(normalizedMinValue), Thumb.MIN.equals(pressedThumb), canvas);

		// draw maximum thumb
		drawThumb(normalizedToScreen(normalizedMaxValue), Thumb.MAX.equals(pressedThumb), canvas);
	}

	/**
	 * Overridden to save instance state when device orientation changes. This
	 * method is called automatically if you assign an id to the RangeSeekBar
	 * widget using the {@link #setId(int)} method. Other members of this class
	 * than the normalized min and max values don't need to be saved.
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		final Bundle bundle = new Bundle();
		bundle.putParcelable(SUPER, super.onSaveInstanceState());
		bundle.putInt(MIN, normalizedMinValue);
		bundle.putInt(MAX, normalizedMaxValue);
		return bundle;
	}

	/**
	 * Overridden to restore instance state when device orientation changes. This
	 * method is called automatically if you assign an id to the RangeSeekBar
	 * widget using the {@link #setId(int)} method.
	 */
	@Override
	protected void onRestoreInstanceState(Parcelable parcel) {
		final Bundle bundle = (Bundle) parcel;
		super.onRestoreInstanceState(bundle.getParcelable(SUPER));
		normalizedMinValue = bundle.getInt(MIN);
		normalizedMaxValue = bundle.getInt(MAX);
	}

	/**
	 * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
	 *
	 * @param screenCoord
	 *          The x-coordinate in screen space where to draw the image.
	 * @param pressed
	 *          Is the thumb currently in "pressed" state?
	 * @param canvas
	 *          The canvas to draw upon.
	 */
	private void drawThumb(float screenCoord, boolean pressed, Canvas canvas) {
		canvas.drawBitmap(pressed ? thumbPressedImage : thumbImage, screenCoord - thumbHalfWidth,
				(float) ((0.5f * getHeight()) - thumbHalfHeight), paint);
	}

	/**
	 * Decides which (if any) thumb is touched by the given x-coordinate.
	 *
	 * @param touchX
	 *          The x-coordinate of a touch event in screen space.
	 * @return The pressed thumb or null if none has been touched.
	 */
	private Thumb evalPressedThumb(float touchX) {
		Thumb result = null;
		boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue);
		boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue);
		if (minThumbPressed && maxThumbPressed) {
			// if both thumbs are pressed (they lie on top of each other), choose the
			// one with more room to drag. this avoids "stalling" the thumbs in a
			// corner, not being able to drag them apart anymore.
			result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
		} else if (minThumbPressed) {
			result = Thumb.MIN;
		} else if (maxThumbPressed) {
			result = Thumb.MAX;
		}
		return result;
	}

	/**
	 * Decides if given x-coordinate in screen space needs to be interpreted as
	 * "within" the normalized thumb x-coordinate.
	 *
	 * @param touchX
	 *          The x-coordinate in screen space to check.
	 * @param normalizedThumbValue
	 *          The normalized x-coordinate of the thumb to check.
	 * @return true if x-coordinate is in thumb range, false otherwise.
	 */
	private boolean isInThumbRange(float touchX, int normalizedThumbValue) {
		return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth;
	}

	/**
	 * Converts a value into screen space.
	 *
	 * @param value
	 *          The normalized value to convert.
	 * @return The converted value in screen space.
	 */
	private float normalizedToScreen(int value) {
		return (float) (padding + (value-getAbsoluteMinValue()) * (getWidth() - 2 * padding) / (getAbsoluteMaxValue()-getAbsoluteMinValue()));
	}

	/**
	 * Converts screen space x-coordinates into value.
	 *
	 * @param screenCoord
	 *          The x-coordinate in screen space to convert.
	 * @return The value.
	 */
	private int screenToValue(float screenCoord) {
		int width = getWidth();
		if (screenCoord<padding) {
			screenCoord = padding;
		} else if (screenCoord>width-padding) {
			screenCoord = width-padding;
		}
		if (width <= 2 * padding) {
			// prevent division by zero, simply return 0.
			return getAbsoluteMinValue();
		} else {
			return getAbsoluteMinValue() + (int)((getAbsoluteMaxValue()-getAbsoluteMinValue())* (screenCoord - padding) / (width - 2 * padding));
		}
	}

	/**
	 * Callback listener interface to notify about changed range values.
	 */
	public interface OnRangeSeekBarChangeListener {
		void onRangeSeekBarValuesChanged(RangeSeekBar bar, int minValue, int maxValue);
	}

	/**
	 * Thumb constants (min and max).
	 */
	private static enum Thumb {
		MIN, MAX
	}
}