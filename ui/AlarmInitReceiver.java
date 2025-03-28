package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

/**
 * 闹钟初始化接收器
 * 用于在设备启动后重新设置所有未触发的笔记提醒
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    // 查询笔记所需的列
    private static final String[] PROJECTION = new String[]{
            NoteColumns.ID,            // 笔记ID
            NoteColumns.ALERTED_DATE   // 提醒时间
    };

    // 列索引常量
    private static final int COLUMN_ID = 0;           // ID列索引
    private static final int COLUMN_ALERTED_DATE = 1;  // 提醒时间列索引

    /**
     * 接收广播时触发的方法
     * @param context 上下文环境
     * @param intent 接收到的Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        long currentDate = System.currentTimeMillis(); // 获取当前时间
        
        // 查询所有未触发且未过期的笔记提醒
        Cursor c = context.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[]{String.valueOf(currentDate)},
                null);

        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    // 获取提醒时间
                    long alertDate = c.getLong(COLUMN_ALERTED_DATE);
                    
                    // 创建触发提醒的Intent
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    // 设置笔记URI
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, c.getLong(COLUMN_ID)));
                    
                    // 创建PendingIntent
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, sender, 0);
                    
                    // 获取AlarmManager服务
                    AlarmManager alarmManager = (AlarmManager) context
                            .getSystemService(Context.ALARM_SERVICE);
                    
                    // 设置闹钟
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alertDate, pendingIntent);
                } while (c.moveToNext()); // 遍历所有符合条件的笔记
            }
            c.close(); // 关闭Cursor
        }
    }
}
