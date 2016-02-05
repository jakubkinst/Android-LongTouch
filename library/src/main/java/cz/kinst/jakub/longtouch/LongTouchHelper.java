package cz.kinst.jakub.longtouch;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;


public class LongTouchHelper {
	public static final long DEFAULT_ANIMATION_DURATION = 500;
	public static final long BLUR_ANIMATION_DURATION = 400;
	public static final int DEFAULT_LONG_PRESS_DELAY_MILLIS = 200;
	public static final int DEFAULT_BLUR_RADIUS = 5;

	private Map<View, Boolean> mPopupVisible = new HashMap<>();
	private Context mContext;
	private Map<View, View> mPopupContents = new HashMap<>();
	private int mOriginX;
	private int mOriginY;
	private boolean mBlurEnabled = true;
	private float mBlurRadius = DEFAULT_BLUR_RADIUS;
	private boolean mHapticFeedbackEnabled = true;
	private int mLongPressDelay = DEFAULT_LONG_PRESS_DELAY_MILLIS;
	private FrameLayout mContainer;
	private boolean mRevealEffectEnabled = true;
	private PopupAnimationProvider mPopupAnimationProvider = new PopupAnimationProvider() {

		@Override
		public Animator getShowAnimator(View content) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isRevealEffectEnabled()) {
				int finalRadius = Math.max(content.getWidth(), content.getHeight());
				Animator anim = ViewAnimationUtils.createCircularReveal(content, mOriginX, mOriginY - getStatusBarHeight(), 0, finalRadius);
				anim.setDuration(DEFAULT_ANIMATION_DURATION);
				return anim;
			} else {
				content.setPivotX(mOriginX);
				content.setPivotY(mOriginY - getStatusBarHeight());
				AnimatorSet animatorSet = new AnimatorSet();
				animatorSet.setDuration(DEFAULT_ANIMATION_DURATION);
				animatorSet.playTogether(ObjectAnimator.ofFloat(content, "scaleX", 0f, 1f), ObjectAnimator.ofFloat(content, "scaleY", 0f, 1f));
				return animatorSet;
			}
		}


		@Override
		public Animator getHideAnimator(View content) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isRevealEffectEnabled()) {
				int finalRadius = Math.max(content.getWidth(), content.getHeight());
				Animator anim = ViewAnimationUtils.createCircularReveal(content, mOriginX, mOriginY - getStatusBarHeight(), finalRadius, 0);
				anim.setDuration(DEFAULT_ANIMATION_DURATION);
				return anim;
			} else {
				content.setPivotX(mOriginX);
				content.setPivotY(mOriginY - getStatusBarHeight());
				AnimatorSet animatorSet = new AnimatorSet();
				animatorSet.setDuration(DEFAULT_ANIMATION_DURATION);
				animatorSet.playTogether(ObjectAnimator.ofFloat(content, "scaleX", 1f, 0f), ObjectAnimator.ofFloat(content, "scaleY", 1f, 0f));
				return animatorSet;
			}
		}
	};
	private Handler mHandler = new Handler();


	public interface PopupAnimationProvider {
		Animator getShowAnimator(View popup);
		Animator getHideAnimator(View popup);
	}


	public static LongTouchHelper setup(FrameLayout container) {
		LongTouchHelper helper = new LongTouchHelper();
		helper.initialize(container);
		return helper;
	}


	private LongTouchHelper() {
	}


	public boolean isRevealEffectEnabled() {
		return mRevealEffectEnabled;
	}


	public void setRevealEffectEnabled(boolean revealEffectEnabled) {
		mRevealEffectEnabled = revealEffectEnabled;
	}


	public int getLongPressDelay() {
		return mLongPressDelay;
	}


	public void setLongPressDelay(int longPressDelay) {
		mLongPressDelay = longPressDelay;
	}


	public boolean isBlurEnabled() {
		return mBlurEnabled;
	}


	public void setBlurEnabled(boolean blurEnabled) {
		this.mBlurEnabled = blurEnabled;
	}


	public float getBlurRadius() {
		return mBlurRadius;
	}


	public void setBlurRadius(float blurRadius) {
		this.mBlurRadius = blurRadius;
	}


	public boolean isHapticFeedbackEnabled() {
		return mHapticFeedbackEnabled;
	}


	public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
		mHapticFeedbackEnabled = hapticFeedbackEnabled;
	}


	public PopupAnimationProvider getPopupAnimationProvider() {
		return mPopupAnimationProvider;
	}


	public void setPopupAnimationProvider(PopupAnimationProvider popupAnimationProvider) {
		mPopupAnimationProvider = popupAnimationProvider;
	}


	public boolean isPopupVisible(View target) {
		return mPopupVisible.containsKey(target) && mPopupVisible.get(target);
	}


	public boolean isAnyPopupVisible() {
		for(Boolean visible : mPopupVisible.values()) {
			if(visible) return true;
		}
		return false;
	}


	public void protectTouchOnViews(View... views) {
		for(View view : views) {
			view.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if(isAnyPopupVisible()) {
						if(event.getAction() == MotionEvent.ACTION_UP)
							hideAll();
						return true;
					} else return false;
				}
			});
		}

	}


	public LongTouchHelper addViewPopup(final View target, View contentView) {
		if(mContext == null)
			mContext = target.getContext();

		View popupContent = LayoutInflater.from(target.getContext()).inflate(R.layout.popup_content, null);
		((FrameLayout) popupContent.findViewById(R.id.popup_content)).addView(contentView);
		mPopupContents.put(target, popupContent);
		target.setOnTouchListener(new View.OnTouchListener() {
			private Runnable mRunnable = new Runnable() {
				@Override
				public void run() {
					show(target);
				}
			};


			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				int action = event.getActionMasked();
				if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_HOVER_EXIT) {
					mHandler.removeCallbacks(mRunnable);
					if(isPopupVisible(v)) {
						hide(target);
						v.setEnabled(false);
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								v.setEnabled(true);
							}
						});
					}
				}
				if(action == MotionEvent.ACTION_CANCEL) {
					mHandler.removeCallbacks(mRunnable);
				}
				if(action == MotionEvent.ACTION_DOWN) {
					mOriginX = Math.round(event.getRawX());
					mOriginY = Math.round(event.getRawY());
					mHandler.postDelayed(mRunnable, getLongPressDelay());
				}
				return false;
			}
		});
		return this;
	}


	public void removeViewPopup(View target) {
		mPopupContents.remove(target);
		mPopupVisible.remove(target);
	}


	private int getStatusBarHeight() {
		int[] location = new int[2];
		mContainer.getLocationInWindow(location);
		return location[1];
	}


	private void hideAll() {
		for(View view : mPopupVisible.keySet()) {
			hide(view);
		}
	}


	private void initialize(FrameLayout container) {
		mContainer = container;
	}


	private void show(View target) {
		if(isPopupVisible(target)) return;
		final View popupContent = mPopupContents.get(target);
		mContainer.addView(popupContent);
		popupContent.setFocusable(true);
		mPopupVisible.put(target, true);

		popupContent.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				v.removeOnLayoutChangeListener(this);
				getPopupAnimationProvider().getShowAnimator(popupContent.findViewById(R.id.popup_content)).start();
			}
		});
		if(isHapticFeedbackEnabled())
			target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);


		if(isBlurEnabled()) {
			((ImageView) popupContent.findViewById(R.id.blur_container)).setImageBitmap(BlurUtility.getBlurredViewBitmap(getViewToBlur(), getBlurRadius()));
			popupContent.findViewById(R.id.blur_container).setAlpha(0);
			popupContent.findViewById(R.id.blur_container).animate().alpha(1f).setDuration(BLUR_ANIMATION_DURATION).start();
		}
	}


	private View getViewToBlur() {
		return mContainer;
	}


	private void hide(final View target) {
		if(!isPopupVisible(target)) return;
		final View popupContent = mPopupContents.get(target);

		if(isBlurEnabled())
			popupContent.findViewById(R.id.blur_container).animate().alpha(0f).setDuration(BLUR_ANIMATION_DURATION).start();

		Animator animator = getPopupAnimationProvider().getHideAnimator(popupContent.findViewById(R.id.popup_content));
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}


			@Override
			public void onAnimationEnd(Animator animation) {
				getViewToBlur().destroyDrawingCache();
				mContainer.removeView(popupContent);
				mPopupVisible.put(target, false);
			}


			@Override
			public void onAnimationCancel(Animator animation) {

			}


			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		});
		animator.start();
	}
}
