package net.micode.notes.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 闹钟提醒接收器
 * 负责接收闹钟触发广播并启动提醒活动界面
 */
public class AlarmReceiver extends BroadcastReceiver {

    /**
     * 当接收到闹钟触发广播时调用
     * @param context 上下文环境
     * @param intent 接收到的Intent，包含笔记数据URI
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 将Intent的目标类设置为AlarmAlertActivity
        intent.setClass(context, AlarmAlertActivity.class);
        
        // 添加NEW_TASK标志，因为从广播接收器启动Activity需要新任务栈
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 启动提醒活动界面
        context.startActivity(intent);
    }
}
