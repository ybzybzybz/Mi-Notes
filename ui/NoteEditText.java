package net.micode.notes.ui;

import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义笔记编辑文本框，扩展了EditText功能
 * 支持清单模式下的特殊按键处理和链接识别
 */
public class NoteEditText extends EditText {
    private static final String TAG = "NoteEditText";
    private int mIndex;                      // 当前文本框在列表中的索引
    private int mSelectionStartBeforeDelete; // 删除操作前的选择起始位置

    // 链接协议常量
    private static final String SCHEME_TEL = "tel:";
    private static final String SCHEME_HTTP = "http:";
    private static final String SCHEME_EMAIL = "mailto:";

    // 链接协议与对应菜单资源ID的映射
    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);    // 电话链接
        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);   // 网页链接
        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);// 邮件链接
    }

    /**
     * 文本框变化监听接口
     * 由NoteEditActivity实现，用于处理文本框的增删改查
     */
    public interface OnTextViewChangeListener {
        /**
         * 当发生删除操作且文本为空时删除当前文本框
         * @param index 当前文本框索引
         * @param text  文本框内容
         */
        void onEditTextDelete(int index, String text);

        /**
         * 当回车键按下时在当前文本框后添加新文本框
         * @param index 新文本框索引
         * @param text  新文本框初始内容
         */
        void onEditTextEnter(int index, String text);

        /**
         * 当文本变化时显示或隐藏选项
         * @param index   文本框索引
         * @param hasText 是否有文本
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener;

    // 构造方法
    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0;
    }

    // 设置当前文本框索引
    public void setIndex(int index) {
        mIndex = index;
    }

    // 设置文本框变化监听器
    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 触摸事件处理
     * 主要用于精确定位触摸位置的选择点
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 计算触摸点在文本中的准确位置
                int x = (int) event.getX();
                int y = (int) event.getY();
                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();
                x += getScrollX();
                y += getScrollY();

                Layout layout = getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                Selection.setSelection(getText(), off); // 设置选择位置
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 按键按下事件处理
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:  // 回车键
                if (mOnTextViewChangeListener != null) {
                    return false; // 交给onKeyUp处理
                }
                break;
            case KeyEvent.KEYCODE_DEL:    // 删除键
                mSelectionStartBeforeDelete = getSelectionStart(); // 记录删除前位置
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 按键释放事件处理
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DEL: // 删除键释放
                if (mOnTextViewChangeListener != null) {
                    // 如果是在开头删除且不是第一个文本框，则通知删除
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            case KeyEvent.KEYCODE_ENTER: // 回车键释放
                if (mOnTextViewChangeListener != null) {
                    // 分割文本，后半部分放入新文本框
                    int selectionStart = getSelectionStart();
                    String text = getText().subSequence(selectionStart, length()).toString();
                    setText(getText().subSequence(0, selectionStart));
                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not seted");
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 焦点变化回调
     */
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            // 根据是否有文本通知状态变化
            if (!focused && TextUtils.isEmpty(getText())) {
                mOnTextViewChangeListener.onTextChange(mIndex, false);
            } else {
                mOnTextViewChangeListener.onTextChange(mIndex, true);
            }
        }
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    /**
     * 创建上下文菜单（长按菜单）
     */
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            // 获取选择范围的起始和结束
            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            // 检查选择范围内是否有URL链接
            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                int defaultResId = 0;
                // 根据URL协议类型获取对应的菜单资源ID
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                // 如果没有匹配的协议类型，使用默认"其他链接"
                if (defaultResId == 0) {
                    defaultResId = R.string.note_link_other;
                }

                // 添加菜单项并设置点击监听
                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                // 触发链接点击事件
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
