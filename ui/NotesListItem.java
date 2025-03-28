package net.micode.notes.ui;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

/**
 * 笔记列表项视图，继承自LinearLayout
 * 用于显示单个笔记或文件夹的UI元素
 */
public class NotesListItem extends LinearLayout {
    private ImageView mAlert;       // 提醒图标
    private TextView mTitle;        // 标题/内容预览
    private TextView mTime;         // 修改时间
    private TextView mCallName;     // 通话记录联系人姓名
    private NoteItemData mItemData; // 绑定的数据项
    private CheckBox mCheckBox;     // 选择框(用于多选模式)

    public NotesListItem(Context context) {
        super(context);
        // 加载布局文件
        inflate(context, R.layout.note_item, this);
        // 初始化视图组件
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 绑定数据到视图
     * @param context 上下文
     * @param data 笔记数据项
     * @param choiceMode 是否处于选择模式
     * @param checked 是否被选中
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 处理选择框可见性
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            mCheckBox.setVisibility(View.GONE);
        }

        mItemData = data; // 保存数据引用
        
        // 根据数据类型设置不同显示样式
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 通话记录文件夹
            mCallName.setVisibility(View.GONE);
            mAlert.setVisibility(View.VISIBLE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount()));
            mAlert.setImageResource(R.drawable.call_record);
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 通话记录笔记
            mCallName.setVisibility(View.VISIBLE);
            mCallName.setText(data.getCallName()); // 显示联系人姓名
            mTitle.setTextAppearance(context,R.style.TextAppearanceSecondaryItem);
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet())); // 格式化内容预览
            // 设置提醒图标
            if (data.hasAlert()) {
                mAlert.setImageResource(R.drawable.clock);
                mAlert.setVisibility(View.VISIBLE);
            } else {
                mAlert.setVisibility(View.GONE);
            }
        } else {
            // 普通笔记或文件夹
            mCallName.setVisibility(View.GONE);
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem);

            if (data.getType() == Notes.TYPE_FOLDER) {
                // 文件夹显示
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                                data.getNotesCount()));
                mAlert.setVisibility(View.GONE);
            } else {
                // 普通笔记显示
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet()));
                // 设置提醒图标
                if (data.hasAlert()) {
                    mAlert.setImageResource(R.drawable.clock);
                    mAlert.setVisibility(View.VISIBLE);
                } else {
                    mAlert.setVisibility(View.GONE);
                }
            }
        }
        // 设置相对时间显示
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 设置背景
        setBackground(data);
    }

    /**
     * 根据数据项设置背景
     * @param data 笔记数据项
     */
    private void setBackground(NoteItemData data) {
        int id = data.getBgColorId();
        if (data.getType() == Notes.TYPE_NOTE) {
            // 根据笔记在列表中的位置设置不同背景
            if (data.isSingle() || data.isOneFollowingFolder()) {
                // 单一项或文件夹后的唯一项
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            } else if (data.isLast()) {
                // 最后一项
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                // 第一项或文件夹后的多项
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                // 普通中间项
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            // 文件夹背景
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }

    /**
     * 获取绑定的数据项
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}
