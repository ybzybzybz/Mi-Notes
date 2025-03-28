package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 笔记编辑Activity，实现点击监听、笔记设置改变监听和文本变化监听
 */
public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {
    // 头部视图持有类
    private class HeadViewHolder {
        public TextView tvModified;       // 修改日期文本
        public ImageView ivAlertIcon;      // 提醒图标
        public TextView tvAlertDate;       // 提醒日期文本
        public ImageView ibSetBgColor;     // 设置背景颜色按钮
    }

    // 背景选择按钮与资源ID的映射
    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW);
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED);
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE);
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN);
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE);
    }

    // 背景选择选中状态与视图ID的映射
    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select);
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select);
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select);
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select);
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select);
    }

    // 字体大小按钮与资源ID的映射
    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();
    static {
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE);
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL);
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM);
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER);
    }

    // 字体大小选中状态与视图ID的映射
    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select);
    }

    private static final String TAG = "NoteEditActivity";  // 日志标签

    private HeadViewHolder mNoteHeaderHolder;      // 头部视图持有对象
    private View mHeadViewPanel;                  // 头部视图面板
    private View mNoteBgColorSelector;             // 背景颜色选择器
    private View mFontSizeSelector;                // 字体大小选择器
    private EditText mNoteEditor;                  // 笔记编辑器
    private View mNoteEditorPanel;                 // 笔记编辑面板
    private WorkingNote mWorkingNote;              // 当前工作笔记对象
    private SharedPreferences mSharedPrefs;        // 共享首选项
    private int mFontSizeId;                       // 当前字体大小ID
    private static final String PREFERENCE_FONT_SIZE = "pref_font_size"; // 字体大小首选项键
    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10; // 快捷图标标题最大长度

    // 复选框标记
    public static final String TAG_CHECKED = String.valueOf('\u221A');
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1');

    private LinearLayout mEditTextList;            // 编辑文本列表布局
    private String mUserQuery;                     // 用户查询字符串
    private Pattern mPattern;                      // 正则表达式模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.note_edit);  // 设置布局

        // 初始化Activity状态，如果失败则结束
        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }
        initResources();  // 初始化资源
    }

    /**
     * 当内存不足时Activity可能被杀死。一旦被杀死，当用户再次加载此Activity时，我们应该恢复之前的状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID));
            if (!initActivityState(intent)) {
                finish();
                return;
            }
            Log.d(TAG, "从被杀死的Activity恢复");
        }
    }

    /**
     * 初始化Activity状态
     * @param intent 启动Activity的Intent
     * @return 初始化是否成功
     */
    private boolean initActivityState(Intent intent) {
        mWorkingNote = null;
        // 处理查看笔记的Intent
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            mUserQuery = "";

            // 处理从搜索结果启动的情况
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
                mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }

            // 检查笔记是否存在于数据库中
            if (!DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE)) {
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            } else {
                mWorkingNote = WorkingNote.load(this, noteId);
                if (mWorkingNote == null) {
                    Log.e(TAG, "加载笔记失败，笔记ID:" + noteId);
                    finish();
                    return false;
                }
            }
            // 设置软键盘模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else if(TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            // 新建笔记
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE);
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this));

            // 解析通话记录笔记
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0 && phoneNumber != null) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.w(TAG, "通话记录号码为空");
                }
                long noteId = 0;
                // 根据电话号码和日期查找现有笔记
                if ((noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(),
                        phoneNumber, callDate)) > 0) {
                    mWorkingNote = WorkingNote.load(this, noteId);
                    if (mWorkingNote == null) {
                        Log.e(TAG, "加载通话笔记失败，笔记ID:" + noteId);
                        finish();
                        return false;
                    }
                } else {
                    // 创建新的通话记录笔记
                    mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId,
                            widgetType, bgResId);
                    mWorkingNote.convertToCallNote(phoneNumber, callDate);
                }
            } else {
                // 创建普通新笔记
                mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType,
                        bgResId);
            }

            // 设置软键盘模式
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            Log.e(TAG, "Intent未指定action，不支持");
            finish();
            return false;
        }
        mWorkingNote.setOnSettingStatusChangedListener(this);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initNoteScreen();  // 初始化笔记界面
    }

    /**
     * 初始化笔记界面
     */
    private void initNoteScreen() {
        // 设置编辑器文本外观
        mNoteEditor.setTextAppearance(this, TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId));
        
        // 根据笔记模式设置编辑器内容
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            switchToListMode(mWorkingNote.getContent());
        } else {
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            mNoteEditor.setSelection(mNoteEditor.getText().length());
        }
        
        // 初始化背景选择器状态
        for (Integer id : sBgSelectorSelectionMap.keySet()) {
            findViewById(sBgSelectorSelectionMap.get(id)).setVisibility(View.GONE);
        }
        
        // 设置头部和编辑面板背景
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置修改日期
        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));

        // 显示提醒头部
        showAlertHeader();
    }

    /**
     * 显示提醒头部信息
     */
    private void showAlertHeader() {
        if (mWorkingNote.hasClockAlert()) {
            long time = System.currentTimeMillis();
            if (time > mWorkingNote.getAlertDate()) {
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS));
            }
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActivityState(intent);  // 初始化新的Intent状态
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /**
         * 对于没有ID的新笔记，我们应该先保存它以生成ID。
         * 如果编辑的笔记不值得保存，则没有ID，相当于创建新笔记
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        Log.d(TAG, "保存工作笔记ID: " + mWorkingNote.getNoteId() + " 在onSaveInstanceState中");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 处理背景颜色选择器的触摸事件
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        // 处理字体大小选择器的触摸事件
        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 检查触摸事件是否在视图范围内
     */
    private boolean inRangeOfView(View view, MotionEvent ev) {
        int []location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
                    return false;
                }
        return true;
    }

    /**
     * 初始化资源
     */
    private void initResources() {
        // 初始化头部视图
        mHeadViewPanel = findViewById(R.id.note_title);
        mNoteHeaderHolder = new HeadViewHolder();
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);
        
        // 初始化编辑器
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        
        // 初始化背景颜色选择器
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);
        for (int id : sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(this);
        }

        // 初始化字体大小选择器
        mFontSizeSelector = findViewById(R.id.font_size_selector);
        for (int id : sFontSizeBtnsMap.keySet()) {
            View view = findViewById(id);
            view.setOnClickListener(this);
        };
        
        // 初始化共享首选项和字体大小
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);
        
        /**
         * HACKME: 修复在共享首选项中存储资源ID的错误。
         * ID可能大于资源长度，在这种情况下，返回默认字体大小
         */
        if(mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }
        
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存笔记
        if(saveNote()) {
            Log.d(TAG, "笔记数据已保存，长度:" + mWorkingNote.getContent().length());
        }
        clearSettingState();  // 清除设置状态
    }

    /**
     * 更新小部件
     */
    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // 根据小部件类型设置不同的广播接收器
        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "不支持的小部件类型");
            return;
        }

        // 发送广播更新小部件
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {
            mWorkingNote.getWidgetId()
        });

        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }
}
// 点击事件处理
public void onClick(View v) {
    int id = v.getId();
    // 背景颜色设置按钮点击
    if (id == R.id.btn_set_bg_color) {
        mNoteBgColorSelector.setVisibility(View.VISIBLE);
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.VISIBLE);
    } 
    // 背景颜色选择
    else if (sBgSelectorBtnsMap.containsKey(id)) {
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.GONE);
        mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(id));
        mNoteBgColorSelector.setVisibility(View.GONE);
    } 
    // 字体大小选择
    else if (sFontSizeBtnsMap.containsKey(id)) {
        findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.GONE);
        mFontSizeId = sFontSizeBtnsMap.get(id);
        mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit();
        findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
        // 根据当前模式更新文本显示
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            getWorkingText();
            switchToListMode(mWorkingNote.getContent());
        } else {
            mNoteEditor.setTextAppearance(this,
                    TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        }
        mFontSizeSelector.setVisibility(View.GONE);
    }
}

// 返回键处理
@Override
public void onBackPressed() {
    if(clearSettingState()) {  // 如果清除了设置状态，则不执行后续操作
        return;
    }

    saveNote();  // 保存笔记
    super.onBackPressed();
}

// 清除设置状态（背景选择器或字体大小选择器）
private boolean clearSettingState() {
    if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
        mNoteBgColorSelector.setVisibility(View.GONE);
        return true;
    } else if (mFontSizeSelector.getVisibility() == View.VISIBLE) {
        mFontSizeSelector.setVisibility(View.GONE);
        return true;
    }
    return false;
}

// 背景颜色改变回调
public void onBackgroundColorChanged() {
    findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
            View.VISIBLE);
    mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());
    mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
}

// 准备选项菜单
@Override
public boolean onPrepareOptionsMenu(Menu menu) {
    if (isFinishing()) {
        return true;
    }
    clearSettingState();
    menu.clear();
    // 根据笔记类型加载不同菜单
    if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
        getMenuInflater().inflate(R.menu.call_note_edit, menu);
    } else {
        getMenuInflater().inflate(R.menu.note_edit, menu);
    }
    // 设置清单模式/普通模式菜单项标题
    if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
        menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
    } else {
        menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
    }
    // 设置提醒相关菜单项可见性
    if (mWorkingNote.hasClockAlert()) {
        menu.findItem(R.id.menu_alert).setVisible(false);
    } else {
        menu.findItem(R.id.menu_delete_remind).setVisible(false);
    }
    return true;
}

// 菜单项选择处理
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case R.id.menu_new_note:  // 新建笔记
            createNewNote();
            break;
        case R.id.menu_delete:  // 删除笔记
            showDeleteNoteDialog();
            break;
        case R.id.menu_font_size:  // 字体大小
            mFontSizeSelector.setVisibility(View.VISIBLE);
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
            break;
        case R.id.menu_list_mode:  // 切换清单模式
            toggleCheckListMode();
            break;
        case R.id.menu_share:  // 分享笔记
            shareNote();
            break;
        case R.id.menu_send_to_desktop:  // 发送到桌面
            sendToDesktop();
            break;
        case R.id.menu_alert:  // 设置提醒
            setReminder();
            break;
        case R.id.menu_delete_remind:  // 删除提醒
            deleteReminder();
            break;
        default:
            break;
    }
    return true;
}

// 显示删除笔记对话框
private void showDeleteNoteDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.alert_title_delete));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setMessage(getString(R.string.alert_message_delete_note));
    builder.setPositiveButton(android.R.string.ok,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    deleteCurrentNote();
                    finish();
                }
            });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
}

// 设置提醒时间
private void setReminder() {
    DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
    d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
        public void OnDateTimeSet(AlertDialog dialog, long date) {
            mWorkingNote.setAlertDate(date, true);
        }
    });
    d.show();
}

// 分享笔记内容
private void sendTo(Context context, String info) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_TEXT, info);
    intent.setType("text/plain");
    context.startActivity(intent);
}

// 创建新笔记
private void createNewNote() {
    saveNote();  // 先保存当前笔记
    finish();  // 结束当前Activity
    // 启动新的NoteEditActivity
    Intent intent = new Intent(this, NoteEditActivity.class);
    intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
    intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId());
    startActivity(intent);
}

// 删除当前笔记
private void deleteCurrentNote() {
    if (mWorkingNote.existInDatabase()) {
        HashSet<Long> ids = new HashSet<Long>();
        long id = mWorkingNote.getNoteId();
        if (id != Notes.ID_ROOT_FOLDER) {
            ids.add(id);
        } else {
            Log.d(TAG, "错误的笔记ID，不应该发生");
        }
        // 根据同步模式选择删除方式
        if (!isSyncMode()) {
            if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) {
                Log.e(TAG, "删除笔记错误");
            }
        } else {
            if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                Log.e(TAG, "移动笔记到回收站错误，不应该发生");
            }
        }
    }
    mWorkingNote.markDeleted(true);
}

// 检查是否处于同步模式
private boolean isSyncMode() {
    return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
}

// 提醒时间改变回调
public void onClockAlertChanged(long date, boolean set) {
    // 对于未保存的笔记，设置提醒前先保存
    if (!mWorkingNote.existInDatabase()) {
        saveNote();
    }
    if (mWorkingNote.getNoteId() > 0) {
        // 设置或取消闹钟提醒
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));
        showAlertHeader();
        if(!set) {
            alarmManager.cancel(pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
        }
    } else {
        // 笔记为空，无法设置提醒
        Log.e(TAG, "提醒设置错误");
        showToast(R.string.error_note_empty_for_clock);
    }
}

// 小部件更新回调
public void onWidgetChanged() {
    updateWidget();
}

// 编辑文本删除回调
public void onEditTextDelete(int index, String text) {
    int childCount = mEditTextList.getChildCount();
    if (childCount == 1) {  // 至少保留一个条目
        return;
    }

    // 更新后续条目的索引
    for (int i = index + 1; i < childCount; i++) {
        ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                .setIndex(i - 1);
    }

    mEditTextList.removeViewAt(index);  // 移除指定条目
    NoteEditText edit = null;
    if(index == 0) {
        edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(
                R.id.et_edit_text);
    } else {
        edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(
                R.id.et_edit_text);
    }
    // 将删除的文本追加到前一个或后一个条目
    int length = edit.length();
    edit.append(text);
    edit.requestFocus();
    edit.setSelection(length);
}

// 编辑文本回车回调
public void onEditTextEnter(int index, String text) {
    if(index > mEditTextList.getChildCount()) {
        Log.e(TAG, "索引超出mEditTextList边界，不应该发生");
    }

    // 在指定位置插入新条目
    View view = getListItem(text, index);
    mEditTextList.addView(view, index);
    NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
    edit.requestFocus();
    edit.setSelection(0);
    // 更新后续条目的索引
    for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
        ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                .setIndex(i);
    }
}

// 切换到清单模式
private void switchToListMode(String text) {
    mEditTextList.removeAllViews();
    String[] items = text.split("\n");
    int index = 0;
    // 为每行文本创建一个清单条目
    for (String item : items) {
        if(!TextUtils.isEmpty(item)) {
            mEditTextList.addView(getListItem(item, index));
            index++;
        }
    }
    // 添加一个空条目用于输入
    mEditTextList.addView(getListItem("", index));
    mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();

    // 切换视图可见性
    mNoteEditor.setVisibility(View.GONE);
    mEditTextList.setVisibility(View.VISIBLE);
}

// 获取高亮查询结果的Spannable
private Spannable getHighlightQueryResult(String fullText, String userQuery) {
    SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);
    if (!TextUtils.isEmpty(userQuery)) {
        mPattern = Pattern.compile(userQuery);
        Matcher m = mPattern.matcher(fullText);
        int start = 0;
        // 高亮所有匹配的文本
        while (m.find(start)) {
            spannable.setSpan(
                    new BackgroundColorSpan(this.getResources().getColor(
                            R.color.user_query_highlight)), m.start(), m.end(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            start = m.end();
        }
    }
    return spannable;
}

// 获取清单条目视图
private View getListItem(String item, int index) {
    View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
    final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
    edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
    CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
    // 复选框状态改变监听
    cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            }
        }
    });

    // 处理已检查/未检查标记
    if (item.startsWith(TAG_CHECKED)) {
        cb.setChecked(true);
        edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        item = item.substring(TAG_CHECKED.length(), item.length()).trim();
    } else if (item.startsWith(TAG_UNCHECKED)) {
        cb.setChecked(false);
        edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
        item = item.substring(TAG_UNCHECKED.length(), item.length()).trim();
    }

    edit.setOnTextViewChangeListener(this);
    edit.setIndex(index);
    edit.setText(getHighlightQueryResult(item, mUserQuery));
    return view;
}

// 文本变化回调
public void onTextChange(int index, boolean hasText) {
    if (index >= mEditTextList.getChildCount()) {
        Log.e(TAG, "错误的索引，不应该发生");
        return;
    }
    // 根据是否有文本显示/隐藏复选框
    if(hasText) {
        mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE);
    } else {
        mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE);
    }
}

// 清单模式改变回调
public void onCheckListModeChanged(int oldMode, int newMode) {
    if (newMode == TextNote.MODE_CHECK_LIST) {
        switchToListMode(mNoteEditor.getText().toString());
    } else {
        if (!getWorkingText()) {
            mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ",
                    ""));
        }
        mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
        mEditTextList.setVisibility(View.GONE);
        mNoteEditor.setVisibility(View.VISIBLE);
    }
}

// 获取工作文本（清单模式或普通模式）
private boolean getWorkingText() {
    boolean hasChecked = false;
    if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
        StringBuilder sb = new StringBuilder();
        // 遍历所有清单条目，构建文本
        for (int i = 0; i < mEditTextList.getChildCount(); i++) {
            View view = mEditTextList.getChildAt(i);
            NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
            if (!TextUtils.isEmpty(edit.getText())) {
                if (((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked()) {
                    sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n");
                    hasChecked = true;
                } else {
                    sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n");
                }
            }
        }
        mWorkingNote.setWorkingText(sb.toString());
    } else {
        mWorkingNote.setWorkingText(mNoteEditor.getText().toString());
    }
    return hasChecked;
}

// 保存笔记
private boolean saveNote() {
    getWorkingText();
    boolean saved = mWorkingNote.saveNote();
    if (saved) {
        setResult(RESULT_OK);  // 设置结果代码
    }
    return saved;
}

// 发送笔记快捷方式到桌面
private void sendToDesktop() {
    // 对于新笔记，先保存
    if (!mWorkingNote.existInDatabase()) {
        saveNote();
    }

    if (mWorkingNote.getNoteId() > 0) {
        Intent sender = new Intent();
        Intent shortcutIntent = new Intent(this, NoteEditActivity.class);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        sender.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                makeShortcutIconTitle(mWorkingNote.getContent()));
        sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_app));
        sender.putExtra("duplicate", true);
        sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        showToast(R.string.info_note_enter_desktop);
        sendBroadcast(sender);
    } else {
        // 笔记为空，无法发送到桌面
        Log.e(TAG, "发送到桌面错误");
        showToast(R.string.error_note_empty_for_send_to_desktop);
    }
}

// 生成快捷方式标题
private String makeShortcutIconTitle(String content) {
    content = content.replace(TAG_CHECKED, "");
    content = content.replace(TAG_UNCHECKED, "");
    return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0,
            SHORTCUT_ICON_TITLE_MAX_LEN) : content;
}

// 显示Toast
private void showToast(int resId) {
    showToast(resId, Toast.LENGTH_SHORT);
}

private void showToast(int resId, int duration) {
    Toast.makeText(this, resId, duration).show();
}
