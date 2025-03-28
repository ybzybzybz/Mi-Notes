package net.micode.notes.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.ResourceParser;

/**
 * 2x尺寸笔记小工具的具体实现类
 * 继承自NoteWidgetProvider基类，实现2x大小笔记小工具的特有功能
 */
public class NoteWidgetProvider_2x extends NoteWidgetProvider {

    /**
     * 当小工具需要更新时调用
     * @param context 上下文环境
     * @param appWidgetManager 小工具管理器
     * @param appWidgetIds 需要更新的一组小工具ID
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 调用父类的update方法完成基础更新操作
        super.update(context, appWidgetManager, appWidgetIds);
    }

    /**
     * 获取2x小工具的布局资源ID
     * @return 返回2x小工具的布局文件ID (R.layout.widget_2x)
     */
    @Override
    protected int getLayoutId() {
        return R.layout.widget_2x;
    }

    /**
     * 根据背景ID获取对应的2x小工具背景资源
     * @param bgId 背景ID
     * @return 对应的2x小工具背景资源ID
     */
    @Override
    protected int getBgResourceId(int bgId) {
        // 通过ResourceParser工具类获取2x尺寸对应的背景资源
        return ResourceParser.WidgetBgResources.getWidget2xBgResource(bgId);
    }

    /**
     * 获取当前小工具的类型
     * @return 返回2x小工具的类型常量(Notes.TYPE_WIDGET_2X)
     */
    @Override
    protected int getWidgetType() {
        return Notes.TYPE_WIDGET_2X;
    }
}
