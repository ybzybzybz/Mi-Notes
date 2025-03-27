package net.micode.notes.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.RemoteViews;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NoteEditActivity;
import net.micode.notes.ui.NotesListActivity;

/**
 * 笔记小工具的抽象基类提供者
 * 处理笔记小工具的基本功能，包括更新和删除
 */
public abstract class NoteWidgetProvider extends AppWidgetProvider {
    // 用于从数据库查询笔记信息的字段
    public static final String[] PROJECTION = new String[]{
            NoteColumns.ID,           // 笔记ID
            NoteColumns.BG_COLOR_ID,  // 背景颜色ID
            NoteColumns.SNIPPET       // 内容摘要
    };

    // 字段索引
    public static final int COLUMN_ID = 0;          // ID列索引
    public static final int COLUMN_BG_COLOR_ID = 1; // 背景颜色列索引
    public static final int COLUMN_SNIPPET = 2;     // 内容摘要列索引

    private static final String TAG = "NoteWidgetProvider"; // 日志标签

    /**
     * 当小工具被删除时调用
     * 清除与被删除小工具关联的笔记中的widget ID
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        for (int i = 0; i < appWidgetIds.length; i++) {
            context.getContentResolver().update(Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.WIDGET_ID + "=?",
                    new String[]{String.valueOf(appWidgetIds[i])});
        }
    }

    /**
     * 获取指定widget ID关联的笔记信息
     * @param context 上下文
     * @param widgetId 小工具ID
     * @return 包含笔记信息的Cursor对象
     */
    private Cursor getNoteWidgetInfo(Context context, int widgetId) {
        return context.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.WIDGET_ID + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[]{String.valueOf(widgetId), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);
    }

    /**
     * 更新小工具内容
     * @param context 上下文
     * @param appWidgetManager 小工具管理器
     * @param appWidgetIds 需要更新的小工具ID数组
     */
    protected void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, false);
    }

    /**
     * 实际执行小工具更新的方法
     * @param context 上下文
     * @param appWidgetManager 小工具管理器
     * @param appWidgetIds 需要更新的小工具ID数组
     * @param privacyMode 是否处于隐私模式
     */
    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
            boolean privacyMode) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            if (appWidgetIds[i] != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int bgId = ResourceParser.getDefaultBgId(context); // 默认背景ID
                String snippet = ""; // 内容摘要
                Intent intent = new Intent(context, NoteEditActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_ID, appWidgetIds[i]);
                intent.putExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, getWidgetType());

                // 查询数据库获取笔记信息
                Cursor c = getNoteWidgetInfo(context, appWidgetIds[i]);
                if (c != null && c.moveToFirst()) {
                    if (c.getCount() > 1) {
                        Log.e(TAG, "发现多个笔记使用相同的小工具ID:" + appWidgetIds[i]);
                        c.close();
                        return;
                    }
                    snippet = c.getString(COLUMN_SNIPPET); // 获取内容摘要
                    bgId = c.getInt(COLUMN_BG_COLOR_ID);  // 获取背景颜色
                    intent.putExtra(Intent.EXTRA_UID, c.getLong(COLUMN_ID)); // 设置笔记ID
                    intent.setAction(Intent.ACTION_VIEW); // 设置为查看模式
                } else {
                    // 如果没有找到关联的笔记
                    snippet = context.getResources().getString(R.string.widget_havenot_content);
                    intent.setAction(Intent.ACTION_INSERT_OR_EDIT); // 设置为新建/编辑模式
                }

                if (c != null) {
                    c.close();
                }

                // 创建RemoteViews对象
                RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
                rv.setImageViewResource(R.id.widget_bg_image, getBgResourceId(bgId)); // 设置背景
                intent.putExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, bgId); // 传递背景ID

                // 创建PendingIntent
                PendingIntent pendingIntent = null;
                if (privacyMode) {
                    // 隐私模式下显示提示信息
                    rv.setTextViewText(R.id.widget_text,
                            context.getString(R.string.widget_under_visit_mode));
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], new Intent(
                            context, NotesListActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
                } else {
                    // 正常模式下显示笔记内容
                    rv.setTextViewText(R.id.widget_text, snippet);
                    pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                }

                // 设置点击事件
                rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
                // 更新小工具
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
            }
        }
    }

    /**
     * 获取背景资源ID (由子类实现)
     * @param bgId 背景ID
     * @return 对应的资源ID
     */
    protected abstract int getBgResourceId(int bgId);

    /**
     * 获取布局ID (由子类实现)
     * @return 布局资源ID
     */
    protected abstract int getLayoutId();

    /**
     * 获取小工具类型 (由子类实现)
     * @return 小工具类型
     */
    protected abstract int getWidgetType();
}
