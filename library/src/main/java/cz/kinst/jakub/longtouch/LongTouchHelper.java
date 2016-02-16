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
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;


public class LongTouchHelper {
	public static final long DEFAULT_SHOW_ANIMATION_DURATION = 500;
	public static final long DEFAULT_HIDE_ANIMATION_DURATION = 300;
	public static final long BLUR_ANIMATION_DURATION = 400;
	public static final int DEFAULT_LONG_PRESS_DELAY_MILLIS = 200;
	public static final int DEFAULT_BLUR_RADIUS = 5;

	private Map<View, Boolean> mPopupVisible = new HashMap<>();
	private Context mContext;
	private Map<View, ContentViewProvider> mPopupContentProviders = new HashMap<>();
	private Map<View, View> mPopupContentViews = new HashMap<>();
	private int mDownX, mDownY;
	private boolean mBlurEnabled = true;
	private float mBlurRadius = DEFAULT_BLUR_RADIUS;
	private boolean mHapticFeedbackEnabled = true;
	private int mLongPressDelay = DEFAULT_LONG_PRESS_DELAY_MILLIS;
	private FrameLayout mContainer;
	private boolean mRevealEffectEnabled = true;
	private Handler mHandler = new Handler();
	private long mShowAnimationDuration = DEFAULT_SHOW_ANIMATION_DURATION;
	private long mHideAnimationDuration = DEFAULT_HIDE_ANIMATION_DURATION;
	private int mUpX, mUpY;
	private PopupAnimationProvider mPopupAnimationProvider = new PopupAnimationProvider() {

		@Override
		public Animator getShowAnimator(View content) {
			Animator defaultAnimator = null;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isRevealEffectEnabled()) {
				int finalRadius = Math.max(content.getWidth(), content.getHeight());
				Animator anim = ViewAnimationUtils.createCircularReveal(content, mDownX, mDownY, 0, finalRadius);
				defaultAnimator = anim;
			}

			AnimatorSet animatorSet = new AnimatorSet();
			content.setPivotX(mDownX);
			content.setPivotY(mDownY);
			ObjectAnimator scaleXanimator = ObjectAnimator.ofFloat(content, "scaleX", 0.3f, 1f);
			scaleXanimator.setInterpolator(new OvershootInterpolator());

			ObjectAnimator scaleYanimator = ObjectAnimator.ofFloat(content, "scaleY", 0.3f, 1f);
			scaleYanimator.setInterpolator(new OvershootInterpolator());

			ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(content, "alpha", 1f);
			alphaAnimator.setDuration(0);

			if(defaultAnimator != null)
				animatorSet.playTogether(defaultAnimator, scaleXanimator, scaleYanimator, alphaAnimator);
			else
				animatorSet.playTogether(scaleXanimator, scaleYanimator, alphaAnimator);

			return animatorSet;
		}


		@Override
		public Animator getHideAnimator(View content) {

			Animator defaultAnimator = null;
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isRevealEffectEnabled()) {
				int finalRadius = Math.max(content.getWidth(), content.getHeight());
				Animator anim = ViewAnimationUtils.createCircularReveal(content, getDownX(), getDownY(), finalRadius, 0);
				anim.setDuration(DEFAULT_SHOW_ANIMATION_DURATION);
				defaultAnimator = anim;
			}
			content.setPivotX(getDownX());
			content.setPivotY(getDownY());
			AnimatorSet animatorSet = new AnimatorSet();
			ObjectAnimator scaleXanimator = ObjectAnimator.ofFloat(content, "scaleX", 1f, 0.5f);
			ObjectAnimator scaleYanimator = ObjectAnimator.ofFloat(content, "scaleY", 1f, 0.5f);
			ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(content, "alpha", 1f, 0);


			if(defaultAnimator != null)
				animatorSet.playTogether(defaultAnimator, scaleXanimator, scaleYanimator, alphaAnimator);
			else
				animatorSet.playTogether(scaleXanimator, scaleYanimator, alphaAnimator);
			return animatorSet;
		}
	};


	public interface PopupAnimationProvider {
		Animator getShowAnimator(View popup);
		Animator getHideAnimator(View popup);
	}


	public interface ContentViewProvider {
		View getPopupContentView();
        void onTouch(MotionEvent event);
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
						if(event.getAction() == MotionEvent.ACTION_UP) {
							mUpX = Math.round(event.getRawX() - getContainerOffsetX());
							mUpY = Math.round(event.getRawY() - getContainerOffsetY());
							hideAll();
						}
                        sendMotionEventToVisiblePopupViews(event);
						return true;
					} else return false;
				}
			});
		}

	}


	public LongTouchHelper addViewPopup(final View target, ContentViewProvider contentViewProvider) {
		if(mContext == null)
			mContext = target.getContext();

		mPopupContentProviders.put(target, contentViewProvider);
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
                sendMotionEventToVisiblePopupViews(event);
				if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_HOVER_EXIT) {
					mHandler.removeCallbacks(mRunnable);
					if(isPopupVisible(v)) {
						mUpX = Math.round(event.getRawX() - getContainerOffsetX());
						mUpY = Math.round(event.getRawY() - getContainerOffsetY());
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
					mDownX = Math.round(event.getRawX() - getContainerOffsetX());
					mDownY = Math.round(event.getRawY() - getContainerOffsetY());
					mHandler.postDelayed(mRunnable, getLongPressDelay());
				}
				return false;
			}
		});
		return this;
	}


	public void removeViewPopup(View target) {
		mPopupContentProviders.remove(target);
		mPopupContentViews.remove(target);
		mPopupVisible.remove(target);
	}


	public long getShowAnimationDuration() {
		return mShowAnimationDuration;
	}


	public void setShowAnimationDuration(long showAnimationDuration) {
		mShowAnimationDuration = showAnimationDuration;
	}


	public long getHideAnimationDuration() {
		return mHideAnimationDuration;
	}


	public void setHideAnimationDuration(long hideAnimationDuration) {
		mHideAnimationDuration = hideAnimationDuration;
	}


	public int getDownX() {
		return mDownX;
	}


	public int getDownY() {
		return mDownY;
	}


	public int getUpX() {
		return mUpX;
	}


	public int getUpY() {
		return mUpY;
	}

    private void sendMotionEventToVisiblePopupViews(MotionEvent event) {
        for (Map.Entry<View, Boolean> entry : mPopupVisible.entrySet()) {
            if (entry.getValue()) {
                mPopupContentProviders.get(entry.getKey()).onTouch(event);
            }
        }
    }


	private int getContainerOffsetY() {
		int[] location = new int[2];
		mContainer.getLocationInWindow(location);
		return location[1];
	}


	private int getContainerOffsetX() {
		int[] location = new int[2];
		mContainer.getLocationInWindow(location);
		return location[0];
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
		View popupContent = mPopupContentProviders.get(target).getPopupContentView();
		final View popupView = LayoutInflater.from(target.getContext()).inflate(R.layout.popup_content, null);
		((FrameLayout) popupView.findViewById(R.id.popup_content)).addView(popupContent);
		mPopupContentViews.put(target, popupView);

		if(isBlurEnabled()) {
			((ImageView) popupView.findViewById(R.id.blur_container)).setImageBitmap(BlurUtility.getBlurredViewBitmap(getViewToBlur(), getBlurRadius()));
			popupView.findViewById(R.id.blur_container).setAlpha(0);
			popupView.findViewById(R.id.blur_container).animate().alpha(1f).setDuration(BLUR_ANIMATION_DURATION).start();
		}
		mContainer.addView(popupView);
		popupView.setFocusable(true);
		mPopupVisible.put(target, true);

		popupView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
				v.removeOnLayoutChangeListener(this);
				Animator animator = getPopupAnimationProvider().getShowAnimator(popupView.findViewById(R.id.popup_content));
				animator.setDuration(getShowAnimationDuration());
				animator.start();
			}
		});
		if(isHapticFeedbackEnabled())
			target.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	}


	private View getViewToBlur() {
		return mContainer;
	}


	private void hide(final View target) {
		if(!isPopupVisible(target)) return;
		final View popupView = mPopupContentViews.get(target);

		if(isBlurEnabled())
			popupView.findViewById(R.id.blur_container).animate().alpha(0f).setDuration(BLUR_ANIMATION_DURATION).start();

		Animator animator = getPopupAnimationProvider().getHideAnimator(popupView.findViewById(R.id.popup_content));
		animator.setDuration(getHideAnimationDuration());
		animator.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}


			@Override
			public void onAnimationEnd(Animator animation) {
				((FrameLayout) popupView.findViewById(R.id.popup_content)).removeAllViews();
				getViewToBlur().destroyDrawingCache();
				mContainer.removeView(popupView);
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
