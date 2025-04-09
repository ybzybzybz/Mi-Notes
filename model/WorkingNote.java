package net.micode.notes.model;

import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.tool.ResourceParser.NoteBgResources;

/**
 * 工作笔记类，用于管理当前正在编辑的笔记
 */
public class WorkingNote {
    // 笔记对象
    private Note mNote;
    // 笔记ID
    private long mNoteId;
    // 笔记内容
    private String mContent;
    // 笔记模式（普通模式或清单模式）
    private int mMode;

    private long mAlertDate;       // 提醒日期
    private long mModifiedDate;    // 修改日期
    private int mBgColorId;       // 背景颜色ID
    private int mWidgetId;       // 小部件ID
    private int mWidgetType;     // 小部件类型
    private long mFolderId;     // 文件夹ID
    private Context mContext;  // 上下文对象

    private static final String TAG = "WorkingNote"; // 日志标签
    private boolean mIsDeleted; // 是否已删除标记
    private NoteSettingChangedListener mNoteSettingStatusListener; // 笔记设置变更监听器

    // 数据表查询列
    public static final String[] DATA_PROJECTION = new String[] {
            DataColumns.ID,
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    // 笔记表查询列
    public static final String[] NOTE_PROJECTION = new String[] {
            NoteColumns.PARENT_ID,
            NoteColumns.ALERTED_DATE,
            NoteColumns.BG_COLOR_ID,
            NoteColumns.WIDGET_ID,
            NoteColumns.WIDGET_TYPE,
            NoteColumns.MODIFIED_DATE
    };

    // 数据表列索引
    private static final int DATA_ID_COLUMN = 0;
    private static final int DATA_CONTENT_COLUMN = 1;
    private static final int DATA_MIME_TYPE_COLUMN = 2;
    private static final int DATA_MODE_COLUMN = 3;

    // 笔记表列索引
    private static final int NOTE_PARENT_ID_COLUMN = 0;
    private static final int NOTE_ALERTED_DATE_COLUMN = 1;
    private static final int NOTE_BG_COLOR_ID_COLUMN = 2;
    private static final int NOTE_WIDGET_ID_COLUMN = 3;
    private static final int NOTE_WIDGET_TYPE_COLUMN = 4;
    private static final int NOTE_MODIFIED_DATE_COLUMN = 5;

    /**
     * 新建笔记构造函数
     * @param context 上下文对象
     * @param folderId 文件夹ID
     */
    private WorkingNote(Context context, long folderId) {
        mContext = context;
        mAlertDate = 0;
        mModifiedDate = System.currentTimeMillis();
        mFolderId = folderId;
        mNote = new Note();
        mNoteId = 0;
        mIsDeleted = false;
        mMode = 0;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
    }

    /**
     * 现有笔记构造函数
     * @param context 上下文对象
     * @param noteId 笔记ID
     * @param folderId 文件夹ID
     */
    private WorkingNote(Context context, long noteId, long folderId) {
        mContext = context;
        mNoteId = noteId;
        mFolderId = folderId;
        mIsDeleted = false;
        mNote = new Note();
        loadNote();
    }

    /**
     * 加载笔记基本信息
     */
    private void loadNote() {
        Cursor cursor = mContext.getContentResolver().query(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), NOTE_PROJECTION, null,
                null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
            }
            cursor.close();
        } else {
            Log.e(TAG, "找不到ID为:" + mNoteId + "的笔记");
            throw new IllegalArgumentException("无法找到ID为 " + mNoteId + " 的笔记");
        }
        loadNoteData();
    }

    /**
     * 加载笔记数据内容
     */
    private void loadNoteData() {
        Cursor cursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI, DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?", new String[] {
                    String.valueOf(mNoteId)
                }, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                    if (DataConstants.NOTE.equals(type)) {
                        mContent = cursor.getString(DATA_CONTENT_COLUMN);
                        mMode = cursor.getInt(DATA_MODE_COLUMN);
                        mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else if (DataConstants.CALL_NOTE.equals(type)) {
                        mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                    } else {
                        Log.d(TAG, "错误的笔记类型:" + type);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            Log.e(TAG, "找不到ID为:" + mNoteId + "的笔记数据");
            throw new IllegalArgumentException("无法找到ID为 " + mNoteId + " 的笔记数据");
        }
    }

    /**
     * 创建空笔记
     * @param context 上下文对象
     * @param folderId 文件夹ID
     * @param widgetId 小部件ID
     * @param widgetType 小部件类型
     * @param defaultBgColorId 默认背景颜色ID
     * @return 工作笔记对象
     */
    public static WorkingNote createEmptyNote(Context context, long folderId, int widgetId,
            int widgetType, int defaultBgColorId) {
        WorkingNote note = new WorkingNote(context, folderId);
        note.setBgColorId(defaultBgColorId);
        note.setWidgetId(widgetId);
        note.setWidgetType(widgetType);
        return note;
    }

    /**
     * 加载现有笔记
     * @param context 上下文对象
     * @param id 笔记ID
     * @return 工作笔记对象
     */
    public static WorkingNote load(Context context, long id) {
        return new WorkingNote(context, id, 0);
    }

    /**
     * 保存笔记
     * @return 是否保存成功
     */
    public synchronized boolean saveNote() {
        if (isWorthSaving()) {
            if (!existInDatabase()) {
                if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                    Log.e(TAG, "创建新笔记失败，ID:" + mNoteId);
                    return false;
                }
            }

            mNote.syncNote(mContext, mNoteId);

            /**
             * 如果存在该笔记的小部件，则更新小部件内容
             */
            if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                    && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                    && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查笔记是否已存在于数据库中
     * @return 是否存在
     */
    public boolean existInDatabase() {
        return mNoteId > 0;
    }

    /**
     * 检查笔记是否值得保存
     * @return 是否值得保存
     */
    private boolean isWorthSaving() {
        if (mIsDeleted || (!existInDatabase() && TextUtils.isEmpty(mContent))
                || (existInDatabase() && !mNote.isLocalModified())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 设置笔记设置变更监听器
     * @param l 监听器对象
     */
    public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
        mNoteSettingStatusListener = l;
    }

    /**
     * 设置提醒日期
     * @param date 提醒日期
     * @param set 是否设置
     */
    public void setAlertDate(long date, boolean set) {
        if (date != mAlertDate) {
            mAlertDate = date;
            mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
        }
        if (mNoteSettingStatusListener != null) {
            mNoteSettingStatusListener.onClockAlertChanged(date, set);
        }
    }

    /**
     * 标记笔记为已删除
     * @param mark 是否删除
     */
    public void markDeleted(boolean mark) {
        mIsDeleted = mark;
        if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                && mWidgetType != Notes.TYPE_WIDGET_INVALIDE && mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onWidgetChanged();
        }
    }

    /**
     * 设置背景颜色ID
     * @param id 颜色ID
     */
    public void setBgColorId(int id) {
        if (id != mBgColorId) {
            mBgColorId = id;
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onBackgroundColorChanged();
            }
            mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
        }
    }

    /**
     * 设置清单模式
     * @param mode 模式
     */
    public void setCheckListMode(int mode) {
        if (mMode != mode) {
            if (mNoteSettingStatusListener != null) {
                mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
            }
            mMode = mode;
            mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
        }
    }

    /**
     * 设置小部件类型
     * @param type 类型
     */
    public void setWidgetType(int type) {
        if (type != mWidgetType) {
            mWidgetType = type;
            mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
        }
    }

    /**
     * 设置小部件ID
     * @param id ID
     */
    public void setWidgetId(int id) {
        if (id != mWidgetId) {
            mWidgetId = id;
            mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
        }
    }

    /**
     * 设置工作文本内容
     * @param text 文本内容
     */
    public void setWorkingText(String text) {
        if (!TextUtils.equals(mContent, text)) {
            mContent = text;
            mNote.setTextData(DataColumns.CONTENT, mContent);
        }
    }

    /**
     * 将笔记转换为通话笔记
     * @param phoneNumber 电话号码
     * @param callDate 通话日期
     */
    public void convertToCallNote(String phoneNumber, long callDate) {
        mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
        mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
        mNote.setNoteValue(NoteColumns.PARENT_ID, String.valueOf(Notes.ID_CALL_RECORD_FOLDER));
    }

    /**
     * 检查是否有提醒
     * @return 是否有提醒
     */
    public boolean hasClockAlert() {
        return (mAlertDate > 0 ? true : false);
    }

    // 以下是一系列获取方法
    public String getContent() {
        return mContent;
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getBgColorResId() {
        return NoteBgResources.getNoteBgResource(mBgColorId);
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public int getTitleBgResId() {
        return NoteBgResources.getNoteTitleBgResource(mBgColorId);
    }

    public int getCheckListMode() {
        return mMode;
    }

    public long getNoteId() {
        return mNoteId;
    }

    public long getFolderId() {
        return mFolderId;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public int getWidgetType() {
        return mWidgetType;
    }

    /**
     * 笔记设置变更监听器接口
     */
    public interface NoteSettingChangedListener {
        /**
         * 当笔记背景颜色改变时调用
         */
        void onBackgroundColorChanged();

        /**
         * 当用户设置闹钟提醒时调用
         * @param date 提醒日期
         * @param set 是否设置
         */
        void onClockAlertChanged(long date, boolean set);

        /**
         * 当用户从小部件创建笔记时调用
         */
        void onWidgetChanged();

        /**
         * 当在清单模式和普通模式之间切换时调用
         * @param oldMode 切换前的模式
         * @param newMode 切换后的模式
         */
        void onCheckListModeChanged(int oldMode, int newMode);
    }
}
