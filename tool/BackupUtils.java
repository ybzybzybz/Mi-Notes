// 工具类，用于将笔记备份为文本文件
public class BackupUtils {
    private static final String TAG = "BackupUtils";
    private static BackupUtils sInstance;

    // 获取 BackupUtils 的单例实例
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    // 状态常量，表示备份或恢复的不同状态
    public static final int STATE_SD_CARD_UNMOUONTED = 0;       // SD 卡未挂载
    public static final int STATE_BACKUP_FILE_NOT_EXIST = 1;    // 备份文件不存在
    public static final int STATE_DATA_DESTROIED = 2;           // 数据损坏或格式异常
    public static final int STATE_SYSTEM_ERROR = 3;             // 系统错误
    public static final int STATE_SUCCESS = 4;                  // 成功

    private TextExport mTextExport;

    private BackupUtils(Context context) {
        mTextExport = new TextExport(context); // 初始化文本导出类
    }

    // 检查外部存储是否可用（SD 卡是否挂载）
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    // 导出笔记为文本
    public int exportToText() {
        return mTextExport.exportToText();
    }

    // 获取导出文本文件的文件名
    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    // 获取导出文本文件的目录
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    // 内部类，处理文本导出功能
    private static class TextExport {
        // 笔记字段：ID、修改时间、预览文本、类型
        private static final String[] NOTE_PROJECTION = {
            NoteColumns.ID,
            NoteColumns.MODIFIED_DATE,
            NoteColumns.SNIPPET,
            NoteColumns.TYPE
        };

        private static final int NOTE_COLUMN_ID = 0;
        private static final int NOTE_COLUMN_MODIFIED_DATE = 1;
        private static final int NOTE_COLUMN_SNIPPET = 2;

        // 数据字段：内容、类型、通话时间、号码等
        private static final String[] DATA_PROJECTION = {
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
        };

        private static final int DATA_COLUMN_CONTENT = 0;
        private static final int DATA_COLUMN_MIME_TYPE = 1;
        private static final int DATA_COLUMN_CALL_DATE = 2;
        private static final int DATA_COLUMN_PHONE_NUMBER = 4;

        // 文本导出格式的字符串数组（如文件夹名格式、时间格式等）
        private final String[] TEXT_FORMAT;
        private static final int FORMAT_FOLDER_NAME = 0;
        private static final int FORMAT_NOTE_DATE = 1;
        private static final int FORMAT_NOTE_CONTENT = 2;

        private Context mContext;
        private String mFileName;
        private String mFileDirectory;

        public TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }

        // 获取格式字符串
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        // 导出指定文件夹下的所有笔记
        private void exportFolderToText(String folderId, PrintStream ps) {
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[] { folderId }, null);

            if (notesCursor != null) {
                if (notesCursor.moveToFirst()) {
                    do {
                        // 写入笔记最后修改时间
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                            mContext.getString(R.string.format_datetime_mdhm),
                            notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));

                        // 写入笔记内容
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                notesCursor.close();
            }
        }

        // 导出指定 ID 的笔记内容
        private void exportNoteToText(String noteId, PrintStream ps) {
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[] { noteId }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // 通话记录笔记
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), phoneNumber));
                            }
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                    DateFormat.format(mContext.getString(R.string.format_datetime_mdhm), callDate)));

                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            // 普通文本笔记
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                dataCursor.close();
            }

            // 每个笔记后写一行空行分隔
            try {
                ps.write(new byte[] { Character.LINE_SEPARATOR, Character.LETTER_NUMBER });
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        // 开始导出整个笔记本为文本
        public int exportToText() {
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }

            // 导出所有文件夹及其笔记
            Cursor folderCursor = mContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND " +
                NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR " +
                NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER,
                null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // 写入文件夹名
                        String folderName = "";
                        if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        if (!TextUtils.isEmpty(folderName)) {
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                        }

                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
            }

            // 导出根目录下的笔记（未归类的笔记）
            Cursor noteCursor = mContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID + "=0",
                null, null);

            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                            mContext.getString(R.string.format_datetime_mdhm),
                            noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));

                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                noteCursor.close();
            }

            ps.close();
            return STATE_SUCCESS;
        }

        // 获取文本输出流（指向导出文件）
        private PrintStream getExportToTextPrintStream() {
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);

            try {
                FileOutputStream fos = new FileOutputStream(file);
                return new PrintStream(fos);
            } catch (FileNotFoundException | NullPointerException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    // 生成 SD 卡上的导出文件
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory());
        sb.append(context.getString(filePathResId));

        File filedir = new File(sb.toString());

        sb.append(context.getString(fileNameFormatResId,
                DateFormat.format(context.getString(R.string.format_date_ymd), System.currentTimeMillis())));

        File file = new File(sb.toString());

        try {
            if (!filedir.exists()) {
                filedir.mkdir(); // 创建目录
            }
            if (!file.exists()) {
                file.createNewFile(); // 创建文件
            }
            return file;
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
