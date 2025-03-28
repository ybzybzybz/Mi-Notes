package net.micode.notes.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

/**
 * 文件夹列表适配器
 * 用于在列表视图中显示笔记文件夹数据
 */
public class FoldersListAdapter extends CursorAdapter {
    // 查询字段投影
    public static final String[] PROJECTION = {
            NoteColumns.ID,       // 文件夹ID
            NoteColumns.SNIPPET   // 文件夹名称(使用SNIPPET字段存储)
    };

    // 列索引常量
    public static final int ID_COLUMN = 0;    // ID列索引
    public static final int NAME_COLUMN = 1;  // 名称列索引

    /**
     * 构造函数
     * @param context 上下文环境
     * @param c 数据游标
     */
    public FoldersListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    /**
     * 创建新视图
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new FolderListItem(context); // 创建新的文件夹列表项
    }

    /**
     * 绑定数据到视图
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof FolderListItem) {
            // 如果是根文件夹则使用特定名称，否则使用数据库中的名称
            String folderName = (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) 
                    ? context.getString(R.string.menu_move_parent_folder) 
                    : cursor.getString(NAME_COLUMN);
            ((FolderListItem) view).bind(folderName); // 绑定文件夹名称
        }
    }

    /**
     * 获取指定位置的文件夹名称
     * @param context 上下文
     * @param position 位置索引
     * @return 文件夹名称
     */
    public String getFolderName(Context context, int position) {
        Cursor cursor = (Cursor) getItem(position);
        return (cursor.getLong(ID_COLUMN) == Notes.ID_ROOT_FOLDER) 
                ? context.getString(R.string.menu_move_parent_folder) 
                : cursor.getString(NAME_COLUMN);
    }

    /**
     * 文件夹列表项自定义视图
     */
    private class FolderListItem extends LinearLayout {
        private TextView mName; // 文件夹名称文本视图

        public FolderListItem(Context context) {
            super(context);
            // 加载布局
            inflate(context, R.layout.folder_list_item, this);
            mName = (TextView) findViewById(R.id.tv_folder_name);
        }

        /**
         * 绑定文件夹名称
         * @param name 文件夹名称
         */
        public void bind(String name) {
            mName.setText(name); // 设置文件夹名称
        }
    }
}
