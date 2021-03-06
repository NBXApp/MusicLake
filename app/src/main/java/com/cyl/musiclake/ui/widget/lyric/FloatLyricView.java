package com.cyl.musiclake.ui.widget.lyric;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cyl.musiclake.MusicApp;
import com.cyl.musiclake.R;
import com.cyl.musiclake.common.NavigationHelper;
import com.cyl.musiclake.player.FloatLyricViewManager;
import com.cyl.musiclake.player.MusicPlayerService;
import com.cyl.musiclake.utils.LogUtil;
import com.cyl.musiclake.utils.SPUtils;
import com.cyl.musiclake.utils.ToastUtils;
import com.rtugeek.android.colorseekbar.ColorSeekBar;

import net.steamcrafted.materialiconlib.MaterialDrawableBuilder;
import net.steamcrafted.materialiconlib.MaterialIconView;

import java.lang.reflect.Field;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * 桌面歌词View
 */
public class FloatLyricView extends FrameLayout implements View.OnClickListener {

    /**
     * 记录小悬浮窗的宽度
     */
    public int viewWidth;

    /**
     * 记录小悬浮窗的高度
     */
    public int viewHeight;

    /**
     * 记录系统状态栏的高度
     */
    private static int statusBarHeight;

    /**
     * 用于更新小悬浮窗的位置
     */
    private WindowManager windowManager;

    /**
     * 小悬浮窗的参数
     */
    private WindowManager.LayoutParams mParams;

    /**
     * 记录当前手指位置在屏幕上的横坐标值
     */
    private float xInScreen;

    /**
     * 记录当前手指位置在屏幕上的纵坐标值
     */
    private float yInScreen;

    /**
     * 记录手指按下时在屏幕上的横坐标的值
     */
    private float xDownInScreen;

    /**
     * 记录手指按下时在屏幕上的纵坐标的值
     */
    private float yDownInScreen;

    /**
     * 记录手指按下时在小悬浮窗的View上的横坐标的值
     */
    private float xInView;

    /**
     * 记录手指按下时在小悬浮窗的View上的纵坐标的值
     */
    private float yInView;
    private float mFontSize;
    private int mFontColor;
    private boolean mMovement;
    private boolean isHiddenSettings;

    public LyricTextView mLyricText;
    public TextView mTitle;
    public SeekBar mSizeSeekBar;
    public ColorSeekBar mColorSeekBar;
    private MaterialIconView mLockButton, mPreButton, mNextButton, mPlayButton, mSettingsButton;
    private ImageButton mCloseButton;
    private ImageButton mMusicButton;
    private LinearLayout mSettingLinearLayout;
    private LinearLayout mRelLyricView;
    private LinearLayout mLinLyricView;
    private FrameLayout mFrameBackground;
    private View mRootView;
    private boolean mIsLock;
    private UnLockNotify mNotify;


    public FloatLyricView(Context context) {
        super(context);
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mNotify = new UnLockNotify();

        mRootView = LayoutInflater.from(context).inflate(R.layout.float_lyric_view, this);
        FrameLayout view = findViewById(R.id.small_window_layout);
        viewWidth = view.getLayoutParams().width;
        viewHeight = view.getLayoutParams().height;
        mMovement = true;
        isHiddenSettings = true;

        mTitle = findViewById(R.id.music_title);
        mSizeSeekBar = findViewById(R.id.sb_size);
        mColorSeekBar = findViewById(R.id.sb_color);
        mLyricText = findViewById(R.id.lyric);
        mCloseButton = findViewById(R.id.btn_close);
        mMusicButton = findViewById(R.id.music_app);
        mLockButton = findViewById(R.id.btn_lock);
        mPreButton = findViewById(R.id.btn_previous);
        mPlayButton = findViewById(R.id.btn_play);
        mNextButton = findViewById(R.id.btn_next);
        mSettingsButton = findViewById(R.id.btn_settings);
        mSettingLinearLayout = findViewById(R.id.ll_settings);
        mRelLyricView = findViewById(R.id.rl_layout);
        mLinLyricView = findViewById(R.id.ll_layout);
        mFrameBackground = findViewById(R.id.small_bg);

        mCloseButton.setOnClickListener(this);
        mMusicButton.setOnClickListener(this);
        mLockButton.setOnClickListener(this);
        mPreButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mSettingsButton.setOnClickListener(this);

        mFontSize = SPUtils.getFontSize();
        mIsLock = SPUtils.getAnyByKey(SPUtils.SP_KEY_FLOAT_LYRIC_LOCK, false);
        mLyricText.setFontSizeScale(mFontSize);
        mSizeSeekBar.setProgress((int) mFontSize);

        mFontColor = SPUtils.getFontColor();
        mLyricText.setFontColorScale(mFontColor);
        mColorSeekBar.setColorBarPosition(mFontColor);

        setPlayStatus(MusicPlayerService.getInstance().isPlaying());

        mSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                LogUtil.e("TEST", progress + "---" + fromUser);
                mLyricText.setFontSizeScale(progress);
                SPUtils.saveFontSize(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mColorSeekBar.setOnColorChangeListener((colorBarPosition, alphaBarPosition, color) -> {
            mLyricText.setFontColorScale(color);
            SPUtils.saveFontColor(color);
        });

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
                xInView = event.getX();
                yInView = event.getY();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY() - getStatusBarHeight();
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                // 手指移动的时候更新小悬浮窗的位置
                updateViewPosition();
                break;
            case MotionEvent.ACTION_UP:
                // 如果手指离开屏幕时，xDownInScreen和xInScreen相等，且yDownInScreen和yInScreen相等，则视为触发了单击事件。
                if (xDownInScreen == xInScreen && yDownInScreen == yInScreen && mMovement) {
                    toggleLyricView();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 将小悬浮窗的参数传入，用于更新小悬浮窗的位置。
     *
     * @param params 小悬浮窗的参数
     */
    public void setParams(WindowManager.LayoutParams params) {
        mParams = params;
    }

    /**
     * 更新小悬浮窗在屏幕中的位置。
     */
    private void updateViewPosition() {
        if (!mMovement) return;
        mParams.x = (int) (xInScreen - xInView);
        mParams.y = (int) (yInScreen - yInView);
        windowManager.updateViewLayout(this, mParams);
    }

    /**
     * toggle背景
     */
    private void toggleLyricView() {
        if (mRootView != null) {
            if (mRelLyricView.getVisibility() == View.INVISIBLE) {
                mRelLyricView.setVisibility(View.VISIBLE);
                mLinLyricView.setVisibility(View.VISIBLE);
                mFrameBackground.setVisibility(View.VISIBLE);
            } else {
                if (!isHiddenSettings) {
                    isHiddenSettings = true;
                    updateSettingStatus(isHiddenSettings);
                }
                mLinLyricView.setVisibility(View.INVISIBLE);
                mRelLyricView.setVisibility(View.INVISIBLE);
                mFrameBackground.setVisibility(View.INVISIBLE);
            }
        }
    }


    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_app:
                Intent intent = NavigationHelper.INSTANCE.getNowPlayingIntent(getContext());
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
                break;
            case R.id.btn_close:
                MusicPlayerService.getInstance().showDesktopLyric(false);
                break;
            case R.id.btn_lock:
                mMovement = !mMovement;
                if (mMovement) {
                    mLockButton.setIcon(MaterialDrawableBuilder.IconValue.LOCK_OPEN);
                } else {
                    saveLock(true,true);
                    mLockButton.setIcon(MaterialDrawableBuilder.IconValue.LOCK);
                }
                break;
            case R.id.btn_previous:
                MusicPlayerService.getInstance().prev();
                break;
            case R.id.btn_play:
                MusicPlayerService.getInstance().playPause();
                setPlayStatus(MusicPlayerService.getInstance().isPlaying());
                break;
            case R.id.btn_next:
                MusicPlayerService.getInstance().next(false);
                break;
            case R.id.btn_settings:
                isHiddenSettings = !isHiddenSettings;
                updateSettingStatus(isHiddenSettings);
                break;
        }
    }

    public void setPlayStatus(boolean isPlaying) {
        if (isPlaying) {
            mPlayButton.setIcon(MaterialDrawableBuilder.IconValue.PAUSE);
        } else {
            mPlayButton.setIcon(MaterialDrawableBuilder.IconValue.PLAY);
        }
    }

    public void updateSettingStatus(boolean isHidden) {
        if (isHidden) {
            mSettingLinearLayout.setVisibility(GONE);
        } else {
            mSettingLinearLayout.setVisibility(VISIBLE);
        }
    }


    public void saveLock(boolean lock, boolean toast) {
        mIsLock = lock;
        SPUtils.putAnyCommit(SPUtils.SP_KEY_FLOAT_LYRIC_LOCK, mIsLock);
        if (toast) {
            ToastUtils.show(MusicApp.getAppContext(), !mIsLock ? R.string.float_unlock : R.string.float_lock);
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        if (params != null) {
            if (lock) {
                //锁定后点击通知栏解锁
                mNotify.notifyToUnlock();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            } else {
                mMovement = true;
                mNotify.cancel();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            toggleLyricView();
            windowManager.updateViewLayout(this, params);
        }
    }


    private static class UnLockNotify {
        private static final String UNLOCK_NOTIFICATION_CHANNEL_ID = "unlock_notification";
        private static final int UNLOCK_NOTIFICATION_ID = 2;
        private Context mContext;
        private NotificationManager mNotificationManager;

        UnLockNotify() {
            mContext = MusicApp.getAppContext();
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initNotificationChanel();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void initNotificationChanel() {
            NotificationChannel notificationChannel = new NotificationChannel(UNLOCK_NOTIFICATION_CHANNEL_ID, mContext.getString(R.string.unlock_notification), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setDescription(mContext.getString(R.string.unlock_notification_description));
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        void notifyToUnlock() {
            Notification notification = new NotificationCompat.Builder(mContext, UNLOCK_NOTIFICATION_CHANNEL_ID)
                    .setContentText(mContext.getString(R.string.float_lock))
                    .setContentTitle(mContext.getString(R.string.click_to_unlock))
                    .setShowWhen(false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setTicker(mContext.getString(R.string.float_lock_ticker))
                    .setContentIntent(buildPendingIntent())
                    .setSmallIcon(R.drawable.ic_music)
                    .build();
            mNotificationManager.notify(UNLOCK_NOTIFICATION_ID, notification);
        }

        void cancel() {
            mNotificationManager.cancel(UNLOCK_NOTIFICATION_ID);
        }

        PendingIntent buildPendingIntent() {
            Intent intent = new Intent(MusicPlayerService.SERVICE_CMD);
            intent.putExtra(MusicPlayerService.CMD_NAME, MusicPlayerService.UNLOCK_DESKTOP_LYRIC);
            intent.setComponent(new ComponentName(mContext, MusicPlayerService.class));
            return PendingIntent.getService(mContext, 0x1123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

}