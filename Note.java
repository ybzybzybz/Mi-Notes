/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import java.util.ArrayList;//导入所需要的包


public class Note {    
    private ContentValues mNoteDiffValues;// 用于存储笔记的变更数据
    private NoteData mNoteData;// 笔记的具体数据，封装在 NoteData 类中
    private static final String TAG = "Note";//日志标签
    /**
     * Create a new note id for adding a new note to databases
     */
    public static synchronized long getNewNoteId(Context context, long folderId) {
        // Create a new note in the database
        ContentValues values = new ContentValues();// 创建 ContentValues 存储新笔记的初始数据
        long createdTime = System.currentTimeMillis();// 获取当前时间戳
        values.put(NoteColumns.CREATED_DATE, createdTime); // 记录创建时间
        values.put(NoteColumns.MODIFIED_DATE, createdTime);// 记录修改时间
        values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);// 设置笔记类型
        values.put(NoteColumns.LOCAL_MODIFIED, 1);// 标记笔记为本地已修改
        values.put(NoteColumns.PARENT_ID, folderId);// 设置笔记所属的文件夹 ID
        Uri uri = context.getContentResolver().insert(Notes.CONTENT_NOTE_URI, values);   // 通过内容解析器向数据库插入新的笔记，并返回对应的 URI

        long noteId = 0;// 解析 URI，获取新创建的笔记 ID
        try {
            noteId = Long.valueOf(uri.getPathSegments().get(1));// 获取 URI 路径中的笔记 ID
        } catch (NumberFormatException e) {
            Log.e(TAG, "Get note id error :" + e.toString());
            noteId = 0;
        }
        if (noteId == -1) {
            throw new IllegalStateException("Wrong note id:" + noteId);
        }// 检查笔记 ID 是否有效
        return noteId;
    }

    public Note() {
        mNoteDiffValues = new ContentValues();// 初始化存储变更数据的对象
        mNoteData = new NoteData();// 初始化 NoteData 以存储笔记的具体内容
    }// Note 类的构造函数，初始化笔记对象

    public void setNoteValue(String key, String value) {
        mNoteDiffValues.put(key, value); // 更新键值对
        mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);// 标记笔记为本地已修改
        mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());// 更新修改时间
    }

    public void setTextData(String key, String value) {
        mNoteData.setTextData(key, value);
    }

    public void setTextDataId(long id) {
        mNoteData.setTextDataId(id);
    }//设置文本数据ID

    public long getTextDataId() {
        return mNoteData.mTextDataId;
    }//获取文本数据ID

    public void setCallDataId(long id) {
        mNoteData.setCallDataId(id);
    }//设置通话数据 ID

    public void setCallData(String key, String value) {
        mNoteData.setCallData(key, value);
    }//设置通话数据

    public boolean isLocalModified() {
        return mNoteDiffValues.size() > 0 || mNoteData.isLocalModified();
    }
    public boolean syncNote(Context context, long noteId) {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Wrong note id:" + noteId);
        }

        if (!isLocalModified()) {
            return true;
        }//如果笔记有本地修改，返回 true，否则返回 false


        /**
         * In theory, once data changed, the note should be updated on {@link NoteColumns#LOCAL_MODIFIED} and
         * {@link NoteColumns#MODIFIED_DATE}. For data safety, though update note fails, we also update the
         * note data info
         */
        if (context.getContentResolver().update(
                ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), mNoteDiffValues, null,
                null) == 0) {
            Log.e(TAG, "Update note error, should not happen");
            // Do not return, fall through
        }
        mNoteDiffValues.clear();
          /**
         * 理论上，一旦数据发生变化，应该更新：
         * - NoteColumns#LOCAL_MODIFIED（标记为本地修改）
         * - NoteColumns#MODIFIED_DATE（更新时间戳）
         * 
         * 但为了数据安全，即使更新失败，也会继续尝试更新笔记数据
         */

        if (mNoteData.isLocalModified()
                && (mNoteData.pushIntoContentResolver(context, noteId) == null)) {
            return false;
        } // 如果笔记数据被本地修改，则尝试同步笔记数据

        return true;
    }

    private class NoteData {
        private long mTextDataId;  // 文本数据 ID

        private ContentValues mTextDataValues; // 存储文本数据变更

        private long mCallDataId;// 通话数据 ID

        private ContentValues mCallDataValues;// 存储通话数据变更

        private static final String TAG = "NoteData";    // 日志标签

        public NoteData() {
            mTextDataValues = new ContentValues();
            mCallDataValues = new ContentValues();
            mTextDataId = 0;
            mCallDataId = 0;
        } /**
        * 构造函数，初始化文本数据和通话数据的 ContentValues，并将数据 ID 设为 0
        */

        boolean isLocalModified() {
            return mTextDataValues.size() > 0 || mCallDataValues.size() > 0;
        }/**
     * 判断文本数据或通话数据是否被修改
     * @return 如果数据有修改，则返回 true，否则返回 false
     */

        void setTextDataId(long id) {
            if(id <= 0) {
                throw new IllegalArgumentException("Text data id should larger than 0");
            }
            mTextDataId = id;
        }/**
     * 设置文本数据 ID
     * @param id 文本数据 ID，必须大于 0
     */

        void setCallDataId(long id) {
            if (id <= 0) {
                throw new IllegalArgumentException("Call data id should larger than 0");
            }
            mCallDataId = id;
        } /**
        * 设置通话数据 ID
        * @param id 通话数据 ID，必须大于 0
        */

        void setCallData(String key, String value) {
            mCallDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }/**
     * 设置通话数据
     * @param key 数据字段名
     * @param value 数据值
     */

        void setTextData(String key, String value) {
            mTextDataValues.put(key, value);
            mNoteDiffValues.put(NoteColumns.LOCAL_MODIFIED, 1);
            mNoteDiffValues.put(NoteColumns.MODIFIED_DATE, System.currentTimeMillis());
        }  /**
        * 设置文本数据
        * @param key 数据字段名
        * @param value 数据值
        */

        Uri pushIntoContentResolver(Context context, long noteId) {
            /**
             * Check for safety
             */
            if (noteId <= 0) {
                throw new IllegalArgumentException("Wrong note id:" + noteId);
            }  /**
            * 将数据同步到数据库
            * @param context 上下文对象
            * @param noteId 需要同步的笔记 ID
            * @return 同步成功返回对应的 URI，否则返回 null
            */

            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            ContentProviderOperation.Builder builder = null;// 存储需要执行的数据库操作

            if(mTextDataValues.size() > 0) {  // 处理文本数据
                mTextDataValues.put(DataColumns.NOTE_ID, noteId);// 关联笔记 ID
                if (mTextDataId == 0) {// 如果文本数据 ID 为空，说明是新数据，需要插入
                    mTextDataValues.put(DataColumns.MIME_TYPE, TextNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mTextDataValues);
                    try {
                        setTextDataId(Long.valueOf(uri.getPathSegments().get(1))); // 解析插入的 ID
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new text data fail with noteId" + noteId);
                        mTextDataValues.clear();
                        return null;
                    }
                } else {// 如果已有数据 ID，执行更新操作
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mTextDataId));
                    builder.withValues(mTextDataValues);
                    operationList.add(builder.build());
                }
                mTextDataValues.clear();// 清空变更记录
            }

            if(mCallDataValues.size() > 0) {
                mCallDataValues.put(DataColumns.NOTE_ID, noteId);// 关联笔记 ID
                if (mCallDataId == 0) {// 如果通话数据 ID 为空，说明是新数据，需要插入
                    mCallDataValues.put(DataColumns.MIME_TYPE, CallNote.CONTENT_ITEM_TYPE);
                    Uri uri = context.getContentResolver().insert(Notes.CONTENT_DATA_URI,
                            mCallDataValues);
                    try {
                        setCallDataId(Long.valueOf(uri.getPathSegments().get(1)));// 解析插入的 ID
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Insert new call data fail with noteId" + noteId);
                        mCallDataValues.clear();
                        return null;
                    }
                } else {// 如果已有数据 ID，执行更新操作
                    builder = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mCallDataId));
                    builder.withValues(mCallDataValues);
                    operationList.add(builder.build());
                }
                mCallDataValues.clear(); // 清空变更记录
            }

            if (operationList.size() > 0) {
                try {
                    ContentProviderResult[] results = context.getContentResolver().applyBatch(
                            Notes.AUTHORITY, operationList);// 关联笔记 ID
                    return (results == null || results.length == 0 || results[0] == null) ? null//// 如果通话数据 ID 为空，说明是新数据，需要插入
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
        }//
        // 批量执行数据库操作
    }
}
//这段代码的主要作用是：创建新便签管理便签的文本和通话数据标记本地修改同步数据到数据库，这意味着它在小米便签中负责便签内容的存储、修改和同步，确保便签数据能够正确保存，并且在应用中被正确管理。







