package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;

/**
 * 闹钟提醒活动界面
 * 处理笔记提醒触发时的显示和交互逻辑
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {
    private long mNoteId; // 当前提醒关联的笔记ID
    private String mSnippet; // 笔记内容摘要
    private static final int SNIPPET_PREW_MAX_LEN = 60; // 摘要最大显示长度
    MediaPlayer mPlayer; // 媒体播放器，用于播放提醒音

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 设置无标题栏

        // 获取窗口并设置标志
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED); // 锁屏时也能显示

        // 如果屏幕关闭，则唤醒屏幕
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 从Intent获取数据
        Intent intent = getIntent();

        try {
            // 解析笔记ID和内容
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            // 截取过长内容
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0,
                    SNIPPET_PREW_MAX_LEN) + getResources().getString(R.string.notelist_string_info)
                    : mSnippet;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }

        mPlayer = new MediaPlayer(); // 初始化媒体播放器
        // 检查笔记是否仍然存在
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog(); // 显示提醒对话框
            playAlarmSound(); // 播放提醒音
        } else {
            finish(); // 笔记不存在则直接结束
        }
    }

    /**
     * 检查屏幕是否亮着
     */
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    /**
     * 播放提醒音
     */
    private void playAlarmSound() {
        // 获取默认闹钟铃声
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // 检查静音模式设置
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 设置音频流类型
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        
        try {
            mPlayer.setDataSource(this, url);
            mPlayer.prepare();
            mPlayer.setLooping(true); // 设置循环播放
            mPlayer.start();
        } catch (IllegalArgumentException | SecurityException | 
                IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示操作对话框
     */
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.app_name); // 设置标题为应用名称
        dialog.setMessage(mSnippet); // 显示笔记摘要
        dialog.setPositiveButton(R.string.notealert_ok, this); // 确定按钮
        
        // 如果屏幕亮着，显示进入笔记按钮
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        
        dialog.show().setOnDismissListener(this); // 设置对话框关闭监听
    }

    /**
     * 对话框按钮点击事件处理
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                // 点击进入笔记按钮，跳转到笔记编辑界面
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    /**
     * 对话框关闭事件处理
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound(); // 停止铃声
        finish(); // 结束活动
    }

    /**
     * 停止播放提醒音
     */
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
}
