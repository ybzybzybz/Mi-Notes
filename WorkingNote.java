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
  * 表示当前正在编辑或查看的便签的模型类。
  * 处理便签的加载、保存、属性更新，并通过监听器通知相关组件状态变化。
  */
 public class WorkingNote {
     // 当前便签的数据对象
     private Note mNote;
     // 便签的数据库ID
     private long mNoteId;
     // 便签的文本内容
     private String mContent;
     // 便签模式（普通/清单模式）
     private int mMode;
 
     // 提醒时间戳（0表示未设置）
     private long mAlertDate;
     // 最后修改时间
     private long mModifiedDate;
     // 背景颜色资源ID
     private int mBgColorId;
     // 关联的小部件ID
     private int mWidgetId;
     // 小部件类型
     private int mWidgetType;
     // 所属文件夹ID
     private long mFolderId;
     // 上下文引用
     private Context mContext;
     // 日志标签
     private static final String TAG = "WorkingNote";
     // 标记是否已删除
     private boolean mIsDeleted;
     // 设置变化监听器
     private NoteSettingChangedListener mNoteSettingStatusListener;
 
     // 数据表查询字段
     public static final String[] DATA_PROJECTION = new String[] {
             DataColumns.ID,
             DataColumns.CONTENT,
             DataColumns.MIME_TYPE,
             DataColumns.DATA1,
             DataColumns.DATA2,
             DataColumns.DATA3,
             DataColumns.DATA4,
     };
 
     // 便签表查询字段
     public static final String[] NOTE_PROJECTION = new String[] {
             NoteColumns.PARENT_ID,
             NoteColumns.ALERTED_DATE,
             NoteColumns.BG_COLOR_ID,
             NoteColumns.WIDGET_ID,
             NoteColumns.WIDGET_TYPE,
             NoteColumns.MODIFIED_DATE
     };
 
     // 数据表字段索引
     private static final int DATA_ID_COLUMN = 0;
     private static final int DATA_CONTENT_COLUMN = 1;
     private static final int DATA_MIME_TYPE_COLUMN = 2;
     private static final int DATA_MODE_COLUMN = 3;
 
     // 便签表字段索引
     private static final int NOTE_PARENT_ID_COLUMN = 0;
     private static final int NOTE_ALERTED_DATE_COLUMN = 1;
     private static final int NOTE_BG_COLOR_ID_COLUMN = 2;
     private static final int NOTE_WIDGET_ID_COLUMN = 3;
     private static final int NOTE_WIDGET_TYPE_COLUMN = 4;
     private static final int NOTE_MODIFIED_DATE_COLUMN = 5;
 
     /**
      * 新建便签的构造函数
      * @param context 上下文
      * @param folderId 所属文件夹ID
      */
     private WorkingNote(Context context, long folderId) {
         mContext = context;
         mAlertDate = 0;
         mModifiedDate = System.currentTimeMillis();
         mFolderId = folderId;
         mNote = new Note(); // 初始化数据对象
         mNoteId = 0; // 新便签ID为0，保存时生成
         mIsDeleted = false;
         mMode = 0; // 默认模式
         mWidgetType = Notes.TYPE_WIDGET_INVALIDE; // 无效小部件类型
     }
 
     /**
      * 加载现有便签的构造函数
      * @param context 上下文
      * @param noteId 便签ID
      * @param folderId 文件夹ID（暂未使用）
      */
     private WorkingNote(Context context, long noteId, long folderId) {
         mContext = context;
         mNoteId = noteId;
         mFolderId = folderId;
         mIsDeleted = false;
         mNote = new Note();
         loadNote(); // 从数据库加载数据
     }
 
     /**
      * 从数据库加载便签基本信息
      */
     private void loadNote() {
         // 通过ContentProvider查询便签表
         Cursor cursor = mContext.getContentResolver().query(
                 ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mNoteId), 
                 NOTE_PROJECTION, null, null, null);
 
         if (cursor != null) {
             if (cursor.moveToFirst()) {
                 // 解析查询结果
                 mFolderId = cursor.getLong(NOTE_PARENT_ID_COLUMN);
                 mBgColorId = cursor.getInt(NOTE_BG_COLOR_ID_COLUMN);
                 mWidgetId = cursor.getInt(NOTE_WIDGET_ID_COLUMN);
                 mWidgetType = cursor.getInt(NOTE_WIDGET_TYPE_COLUMN);
                 mAlertDate = cursor.getLong(NOTE_ALERTED_DATE_COLUMN);
                 mModifiedDate = cursor.getLong(NOTE_MODIFIED_DATE_COLUMN);
             }
             cursor.close();
         } else {
             Log.e(TAG, "无法找到ID为" + mNoteId + "的便签");
             throw new IllegalArgumentException("找不到ID为" + mNoteId + "的便签");
         }
         loadNoteData(); // 加载详细内容数据
     }
 
     /**
      * 加载便签的详细数据（内容、类型等）
      */
     private void loadNoteData() {
         // 查询数据表
         Cursor cursor = mContext.getContentResolver().query(
                 Notes.CONTENT_DATA_URI, 
                 DATA_PROJECTION,
                 DataColumns.NOTE_ID + "=?", 
                 new String[] { String.valueOf(mNoteId) }, 
                 null);
 
         if (cursor != null) {
             if (cursor.moveToFirst()) {
                 do {
                     String type = cursor.getString(DATA_MIME_TYPE_COLUMN);
                     if (DataConstants.NOTE.equals(type)) {
                         // 普通便签内容
                         mContent = cursor.getString(DATA_CONTENT_COLUMN);
                         mMode = cursor.getInt(DATA_MODE_COLUMN);
                         mNote.setTextDataId(cursor.getLong(DATA_ID_COLUMN));
                     } else if (DataConstants.CALL_NOTE.equals(type)) {
                         // 通话记录类型便签
                         mNote.setCallDataId(cursor.getLong(DATA_ID_COLUMN));
                     } else {
                         Log.d(TAG, "未知便签类型：" + type);
                     }
                 } while (cursor.moveToNext());
             }
             cursor.close();
         } else {
             Log.e(TAG, "找不到ID为" + mNoteId + "的便签数据");
             throw new IllegalArgumentException("找不到便签数据");
         }
     }
 
     /**
      * 创建新的空便签
      * @param context 上下文
      * @param folderId 文件夹ID
      * @param widgetId 关联小部件ID
      * @param widgetType 小部件类型
      * @param defaultBgColorId 默认背景颜色
      * @return 新创建的WorkingNote实例
      */
     public static WorkingNote createEmptyNote(Context context, long folderId, 
             int widgetId, int widgetType, int defaultBgColorId) {
         WorkingNote note = new WorkingNote(context, folderId);
         note.setBgColorId(defaultBgColorId);
         note.setWidgetId(widgetId);
         note.setWidgetType(widgetType);
         return note;
     }
 
     /**
      * 加载指定ID的便签
      * @param context 上下文
      * @param id 便签ID
      * @return WorkingNote实例
      */
     public static WorkingNote load(Context context, long id) {
         return new WorkingNote(context, id, 0);
     }
 
     /**
      * 保存便签到数据库（线程安全）
      * @return 是否保存成功
      */
     public synchronized boolean saveNote() {
         if (isWorthSaving()) {
             if (!existInDatabase()) {
                 // 新便签生成ID
                 if ((mNoteId = Note.getNewNoteId(mContext, mFolderId)) == 0) {
                     Log.e(TAG, "创建新便签失败，ID：" + mNoteId);
                     return false;
                 }
             }
 
             // 同步数据到数据库
             mNote.syncNote(mContext, mNoteId);
 
             // 通知小部件更新
             if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                     && mWidgetType != Notes.TYPE_WIDGET_INVALIDE
                     && mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onWidgetChanged();
             }
             return true;
         } else {
             return false; // 无需保存
         }
     }
 
     // 判断便签是否已存在于数据库
     public boolean existInDatabase() {
         return mNoteId > 0;
     }
 
     // 判断当前便签是否需要保存
     private boolean isWorthSaving() {
         return !mIsDeleted 
                 && ((!existInDatabase() && !TextUtils.isEmpty(mContent))
                 || (existInDatabase() && mNote.isLocalModified());
     }
 
     // 设置监听器
     public void setOnSettingStatusChangedListener(NoteSettingChangedListener l) {
         mNoteSettingStatusListener = l;
     }
 
     // 设置提醒时间
     public void setAlertDate(long date, boolean set) {
         if (date != mAlertDate) {
             mAlertDate = date;
             mNote.setNoteValue(NoteColumns.ALERTED_DATE, String.valueOf(mAlertDate));
         }
         if (mNoteSettingStatusListener != null) {
             mNoteSettingStatusListener.onClockAlertChanged(date, set);
         }
     }
 
     // 标记删除状态
     public void markDeleted(boolean mark) {
         mIsDeleted = mark;
         if (mWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                 && mWidgetType != Notes.TYPE_WIDGET_INVALIDE 
                 && mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onWidgetChanged();
         }
     }
 
     // 设置背景颜色
     public void setBgColorId(int id) {
         if (id != mBgColorId) {
             mBgColorId = id;
             if (mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onBackgroundColorChanged();
             }
             mNote.setNoteValue(NoteColumns.BG_COLOR_ID, String.valueOf(id));
         }
     }
 
     // 切换清单模式
     public void setCheckListMode(int mode) {
         if (mMode != mode) {
             if (mNoteSettingStatusListener != null) {
                 mNoteSettingStatusListener.onCheckListModeChanged(mMode, mode);
             }
             mMode = mode;
             mNote.setTextData(TextNote.MODE, String.valueOf(mMode));
         }
     }
 
     // 设置小部件类型
     public void setWidgetType(int type) {
         if (type != mWidgetType) {
             mWidgetType = type;
             mNote.setNoteValue(NoteColumns.WIDGET_TYPE, String.valueOf(mWidgetType));
         }
     }
 
     // 设置小部件ID
     public void setWidgetId(int id) {
         if (id != mWidgetId) {
             mWidgetId = id;
             mNote.setNoteValue(NoteColumns.WIDGET_ID, String.valueOf(mWidgetId));
         }
     }
 
     // 更新便签内容
     public void setWorkingText(String text) {
         if (!TextUtils.equals(mContent, text)) {
             mContent = text;
             mNote.setTextData(DataColumns.CONTENT, mContent);
         }
     }
 
     // 转换为通话记录便签
     public void convertToCallNote(String phoneNumber, long callDate) {
         mNote.setCallData(CallNote.CALL_DATE, String.valueOf(callDate));
         mNote.setCallData(CallNote.PHONE_NUMBER, phoneNumber);
         mNote.setNoteValue(NoteColumns.PARENT_ID, 
                 String.valueOf(Notes.ID_CALL_RECORD_FOLDER)); // 设置父文件夹为通话记录
     }
 
     // 是否设置了提醒
     public boolean hasClockAlert() {
         return mAlertDate > 0;
     }
 
     // 以下为属性获取方法
     public String getContent() { return mContent; }
     public long getAlertDate() { return mAlertDate; }
     public long getModifiedDate() { return mModifiedDate; }
     public int getBgColorResId() { return NoteBgResources.getNoteBgResource(mBgColorId); }
     public int getBgColorId() { return mBgColorId; }
     public int getTitleBgResId() { return NoteBgResources.getNoteTitleBgResource(mBgColorId); }
     public int getCheckListMode() { return mMode; }
     public long getNoteId() { return mNoteId; }
     public long getFolderId() { return mFolderId; }
     public int getWidgetId() { return mWidgetId; }
     public int getWidgetType() { return mWidgetType; }
 
     /**
      * 便签设置变化监听器接口
      */
     public interface NoteSettingChangedListener {
         void onBackgroundColorChanged(); // 背景颜色变化
         void onClockAlertChanged(long date, boolean set); // 提醒设置变化
         void onWidgetChanged(); // 小部件需要更新
         void onCheckListModeChanged(int oldMode, int newMode); // 清单模式切换
     }
 }
 //主要功能说明：
//数据管理：封装便签的数据库操作，包括新建、加载、保存便签，处理与ContentProvider的交互。

//状态跟踪：跟踪便签的修改状态、删除状态、提醒时间、背景颜色等属性。

//模式切换：支持普通文本模式和清单模式的切换。

//小部件集成：管理与小部件相关的ID和类型，通知小部件更新内容。

//事件通知：通过监听器模式通知UI组件关于便签设置的变化，如颜色变化、提醒设置等。

//类型转换：支持将普通便签转换为通话记录便签，更新相关字段。