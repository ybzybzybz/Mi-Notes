public class DataUtils {
    public static final String TAG = "DataUtils";

    // 批量删除笔记
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset");
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            // 不允许删除系统根文件夹
            if(id == Notes.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root");
                continue;
            }
            // 构建删除操作
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            operationList.add(builder.build());
        }

        try {
            // 批量执行删除操作
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    // 将单个笔记移动到另一个文件夹
    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId, long desFolderId) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.PARENT_ID, desFolderId); // 设置新的父文件夹
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId); // 原始父文件夹记录
        values.put(NoteColumns.LOCAL_MODIFIED, 1); // 标记本地修改
        resolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id), values, null, null);
    }

    // 批量移动笔记到指定文件夹
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids, long folderId) {
        if (ids == null) {
            Log.d(TAG, "the ids is null");
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id));
            builder.withValue(NoteColumns.PARENT_ID, folderId); // 更新父文件夹ID
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1); // 标记为本地修改
            operationList.add(builder.build());
        }

        try {
            // 执行批量更新操作
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList);
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString());
                return false;
            }
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
        }
        return false;
    }

    // 获取用户创建的文件夹数量（不包括系统文件夹）
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { "COUNT(*)" },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);

        int count = 0;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                try {
                    count = cursor.getInt(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "get folder count failed:" + e.toString());
                } finally {
                    cursor.close();
                }
            }
        }
        return count;
    }

    // 判断笔记是否在数据库中可见（未被放入回收站）
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER,
                new String [] {String.valueOf(type)},
                null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // 判断某条笔记是否存在于数据库中
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // 判断某条数据是否存在于数据表中（data 表）
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // 检查文件夹名称是否已经存在（可见文件夹中）
    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER +
                " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER +
                " AND " + NoteColumns.SNIPPET + "=?",
                new String[] { name }, null);
        boolean exist = false;
        if(cursor != null) {
            if(cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // 获取指定文件夹中所有笔记绑定的小组件信息
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE },
                NoteColumns.PARENT_ID + "=?",
                new String[] { String.valueOf(folderId) },
                null);

        HashSet<AppWidgetAttribute> set = null;
        if (c != null) {
            if (c.moveToFirst()) {
                set = new HashSet<AppWidgetAttribute>();
                do {
                    try {
                        AppWidgetAttribute widget = new AppWidgetAttribute();
                        widget.widgetId = c.getInt(0);
                        widget.widgetType = c.getInt(1);
                        set.add(widget); // 加入结果集
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, e.toString());
                    }
                } while (c.moveToNext());
            }
            c.close();
        }
        return set;
    }

    // 根据笔记 ID 获取对应的通话号码
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String [] { CallNote.PHONE_NUMBER },
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String [] { String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE },
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                return cursor.getString(0);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Get call number fails " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    // 根据电话号码和通话日期查找对应的笔记 ID
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String [] { CallNote.NOTE_ID },
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                + CallNote.PHONE_NUMBER + ",?)",
                new String [] { String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber },
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    return cursor.getLong(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "Get call note id fails " + e.toString());
                }
            }
            cursor.close();
        }
        return 0;
    }

    // 根据笔记 ID 获取笔记的摘要内容（snippet）
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String [] { NoteColumns.SNIPPET },
                NoteColumns.ID + "=?",
                new String [] { String.valueOf(noteId)},
                null);

        if (cursor != null) {
            String snippet = "";
            if (cursor.moveToFirst()) {
                snippet = cursor.getString(0);
            }
            cursor.close();
            return snippet;
        }
        throw new IllegalArgumentException("Note is not found with id: " + noteId);
    }

    // 对摘要内容进行格式化（去除换行符，只保留第一行）
    public static String getFormattedSnippet(String snippet) {
        if (snippet != null) {
            snippet = snippet.trim(); // 去除首尾空格
            int index = snippet.indexOf('\n');
            if (index != -1) {
                snippet = snippet.substring(0, index); // 截取第一行
            }
        }
        return snippet;
    }
}
