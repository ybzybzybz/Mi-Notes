package net.micode.notes.model;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.Notes.TextNote;

import java.util.ArrayList;

/**
 * 笔记实体类，用于管理笔记的创建、更新和同步操作
 */
public class Note {
    private ContentValues mNoteDiffValues; // 存储笔记差异值的ContentValues
    private NoteData mNoteData;           // 笔记数据对象
    private static final String TAG = "Note"; // 日志标签

    /**
     * 创建一个新的笔记ID，用于向数据库添加新笔记
     * @param context 上下文对象
     * @param folderId 文件夹ID
     * @return 新创建的笔记ID
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // 在数据库中创建一个新笔记
        ContentValues values = new ContentValues();
        long createdTime = System.currentTimeMillis();
        values.put(NoteColumns.CREATED_DATE, createdTime);    // 设置创建时间
        values.put(NoteColumns.MODIFIED_DATE, createdTime);   // 设置修改时间
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);       // 设置笔记类型
        values.put(NoteColumns.LOCAL_MODIFIED, 1);            // 标记为本地已修改
        values.put(NoteColumns.PARENT_ID, folderId);          // 设置父文件夹ID
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);

        long noteId = 0;
        try {
            // 从URI中获取新创建的笔记ID
            noteId = Long.valueOf(uri.getPathSegments().get(1));
        } catch (NumberFormatException e) {
            Log.e(TAG, "获取笔记ID错误:" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            throw new IllegalStateException("错误的笔记ID:" + noteId);
        }
        return noteId;
    }

    public Note() {
        mNoteDiffValues = new ContentValues();
        mNoteData = new NoteData();
    }

    /**
     * 设置笔记值
     * @param key 键
     * @param value 值
     */
    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value);
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);       // 标记为本地已修改
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
    }

    /**
     * 设置文本数据
     * @param key 键
     * @param value 值
     */
    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value);
    }

    /**
     * 设置文本数据ID
     * @param id 文本数据ID
     */
    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id);
    }

    /**
     * 获取文本数据ID
     * @return 文本数据ID
     */
    public long getTextDataId() {
        return mNoteData.mTextDataId;
    }

    /**
     * 设置通话数据ID
     * @param id 通话数据ID
     */
    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id);
    }

    /**
     * 设置通话数据
     * @param key 键
     * @param value 值
     */
    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value);
    }

    /**
     * 检查笔记是否在本地被修改过
     * @return 如果被修改过返回true，否则返回false
     */
    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified();
    }

    /**
     * 同步笔记到数据库
     * @param context 上下文对象
     * @param noteId 笔记ID
     * @return 同步是否成功
     */
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("错误的笔记ID:" + noteId);
        }

        if (!isLocalModified()) {
            return true;
        }

        /**
         * 理论上，一旦数据改变，笔记应该在{@link NoteColumns#LOCAL_MODIFIED}和
         * {@link NoteColumns#MODIFIED_DATE}上更新。为了数据安全，即使更新笔记失败，
         * 我们也会更新笔记数据信息
         */
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "更新笔记错误，不应该发生");
            // 不返回，继续执行
        }
        mNoteDiffValues.clear();

        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        }

        return true;
    }

    /**
     * 内部类，用于管理笔记数据
     */
    private class NoteData {
        private long mTextDataId;               // 文本数据ID
        private ContentValues mTextDataValues;  // 文本数据值
        private long mCallDataId;              // 通话数据ID
        private ContentValues mCallDataValues; // 通话数据值
        private static final String TAG = "NoteData"; // 日志标签

        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        }

        /**
         * 检查数据是否在本地被修改过
         * @return 如果被修改过返回true，否则返回false
         */
        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0;
        }

        /**
         * 设置文本数据ID
         * @param id 文本数据ID
         */
        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("文本数据ID应该大于0");
            }
            mTextDataId = id;
        }

        /**
         * 设置通话数据ID
         * @param id 通话数据ID
         */
        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("通话数据ID应该大于0");
            }
            mCallDataId = id;
        }

        /**
         * 设置通话数据
         * @param key 键
         * @param value 值
         */
        void setCallData(String key, String value) {
            mCallDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);       // 标记为本地已修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        /**
         * 设置文本数据
         * @param key 键
         * @param value 值
         */
        void setTextData(String key, String value) {
            mTextDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);       // 标记为本地已修改
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis()); // 更新修改时间
        }

        /**
         * 将数据推送到ContentResolver
         * @param context 上下文对象
         * @param noteId 笔记ID
         * @return 操作结果的URI，如果失败返回null
         */
        Uri pushIntoContentResolver(Context context, long noteId) {
            // 安全检查
            if (noteId <= 0) {
                throw new IllegalArgumentException("错误的笔记ID:" + noteId);
            }

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;

            // 处理文本数据
            if(mTextDataValues.size() > 0) {
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mTextDataId == 0) {
                    // 如果是新文本数据，则插入
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "插入新文本数据失败，笔记ID:" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {
                    // 如果是现有文本数据，则更新
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    operationList.add(builder.build());
                }
                mTextDataValues.clear();
            }

            // 处理通话数据
            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);
                if (mCallDataId == 0) {
                    // 如果是新通话数据，则插入
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "插入新通话数据失败，笔记ID:" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {
                    // 如果是现有通话数据，则更新
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    operationList.add(builder.build());
                }
                mCallDataValues.clear();
            }

            // 执行批量操作
            if (operationList.size() > 0) {
                try {
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);
                    return (results == null || results.length == 0 || results[0] == null) ? null
                            : ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    return null;
                }
            }
            return null;
        }
    }
}
