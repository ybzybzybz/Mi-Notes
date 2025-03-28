package net.micode.notes.ui;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * 笔记列表适配器，继承自CursorAdapter
 * 用于将数据库中的笔记数据绑定到列表视图
 */
public class NotesListAdapter extends CursorAdapter {
    private static final String TAG = "NotesListAdapter";
    private Context mContext;                   // 上下文对象
    private HashMap<Integer, Boolean> mSelectedIndex; // 记录选中项的position和选中状态
    private int mNotesCount;                   // 笔记总数
    private boolean mChoiceMode;               // 是否处于选择模式

    /**
     * 小部件属性类
     */
    public static class AppWidgetAttribute {
        public int widgetId;    // 小部件ID
        public int widgetType;  // 小部件类型
    };

    /**
     * 构造函数
     */
    public NotesListAdapter(Context context) {
        super(context, null); // 初始cursor为null，稍后通过changeCursor设置
        mSelectedIndex = new HashMap<Integer, Boolean>();
        mContext = context;
        mNotesCount = 0;
    }

    /**
     * 创建新视图
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new NotesListItem(context); // 创建一个新的NotesListItem视图
    }

    /**
     * 绑定数据到视图
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof NotesListItem) {
            // 从cursor创建NoteItemData并绑定到NotesListItem
            NoteItemData itemData = new NoteItemData(context, cursor);
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    /**
     * 设置选中项
     * @param position 列表位置
     * @param checked 是否选中
     */
    public void setCheckedItem(final int position, final boolean checked) {
        mSelectedIndex.put(position, checked);
        notifyDataSetChanged(); // 数据变化通知更新视图
    }

    /**
     * 是否处于选择模式
     */
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    /**
     * 设置选择模式
     */
    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear(); // 清除之前的选择状态
        mChoiceMode = mode;
    }

    /**
     * 全选/取消全选
     */
    public void selectAll(boolean checked) {
        Cursor cursor = getCursor();
        // 遍历所有笔记项设置选中状态
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    /**
     * 获取选中项的ID集合
     */
    public HashSet<Long> getSelectedItemIds() {
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Long id = getItemId(position);
                if (id == Notes.ID_ROOT_FOLDER) {
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    itemSet.add(id); // 添加选中项的ID到集合
                }
            }
        }
        return itemSet;
    }

    /**
     * 获取选中项关联的小部件属性集合
     */
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Cursor c = (Cursor) getItem(position);
                if (c != null) {
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    NoteItemData item = new NoteItemData(mContext, c);
                    widget.widgetId = item.getWidgetId();
                    widget.widgetType = item.getWidgetType();
                    itemSet.add(widget);
                    // 注意：这里不能关闭cursor，只有适配器可以关闭它
                } else {
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    /**
     * 获取选中项数量
     */
    public int getSelectedCount() {
        Collection<Boolean> values = mSelectedIndex.values();
        if (null == values) {
            return 0;
        }
        Iterator<Boolean> iter = values.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (true == iter.next()) {
                count++; // 统计选中项数量
            }
        }
        return count;
    }

    /**
     * 是否全部选中
     */
    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    /**
     * 检查指定位置是否被选中
     */
    public boolean isSelectedItem(final int position) {
        if (null == mSelectedIndex.get(position)) {
            return false;
        }
        return mSelectedIndex.get(position);
    }

    /**
     * 内容变化回调
     */
    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount(); // 重新计算笔记数量
    }

    /**
     * 切换Cursor时调用
     */
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        calcNotesCount(); // 重新计算笔记数量
    }

    /**
     * 计算笔记数量
     */
    private void calcNotesCount() {
        mNotesCount = 0;
        // 遍历cursor统计笔记数量
        for (int i = 0; i < getCount(); i++) {
            Cursor c = (Cursor) getItem(i);
            if (c != null) {
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    mNotesCount++; // 只统计笔记类型，不包含文件夹
                }
            } else {
                Log.e(TAG, "Invalid cursor");
                return;
            }
        }
    }
}
