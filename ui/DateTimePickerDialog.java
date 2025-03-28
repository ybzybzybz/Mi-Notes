package net.micode.notes.ui;

import java.util.Calendar;

import net.micode.notes.R;
import net.micode.notes.ui.DateTimePicker;
import net.micode.notes.ui.DateTimePicker.OnDateTimeChangedListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

/**
 * 日期时间选择对话框
 * 封装了DateTimePicker控件，提供完整的日期时间选择功能
 */
public class DateTimePickerDialog extends AlertDialog implements OnClickListener {

    private Calendar mDate = Calendar.getInstance(); // 当前选择的日期时间
    private boolean mIs24HourView; // 是否24小时制
    private OnDateTimeSetListener mOnDateTimeSetListener; // 设置完成监听器
    private DateTimePicker mDateTimePicker; // 日期时间选择控件

    /**
     * 日期时间设置完成回调接口
     */
    public interface OnDateTimeSetListener {
        void OnDateTimeSet(AlertDialog dialog, long date);
    }

    /**
     * 构造函数
     * @param context 上下文
     * @param date 初始日期时间(毫秒)
     */
    public DateTimePickerDialog(Context context, long date) {
        super(context);
        
        // 初始化日期时间选择控件
        mDateTimePicker = new DateTimePicker(context);
        setView(mDateTimePicker); // 将选择器添加到对话框
        
        // 设置日期时间变化监听
        mDateTimePicker.setOnDateTimeChangedListener(new OnDateTimeChangedListener() {
            @Override
            public void onDateTimeChanged(DateTimePicker view, int year, int month,
                    int dayOfMonth, int hourOfDay, int minute) {
                // 更新内部Calendar对象
                mDate.set(Calendar.YEAR, year);
                mDate.set(Calendar.MONTH, month);
                mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                mDate.set(Calendar.MINUTE, minute);
                updateTitle(mDate.getTimeInMillis()); // 更新对话框标题
            }
        });
        
        // 设置初始日期时间
        mDate.setTimeInMillis(date);
        mDate.set(Calendar.SECOND, 0); // 秒数设为0
        mDateTimePicker.setCurrentDate(mDate.getTimeInMillis());
        
        // 设置对话框按钮
        setButton(context.getString(R.string.datetime_dialog_ok), this); // 确定按钮
        setButton2(context.getString(R.string.datetime_dialog_cancel), (OnClickListener)null); // 取消按钮
        
        // 设置时间显示格式
        set24HourView(DateFormat.is24HourFormat(this.getContext()));
        updateTitle(mDate.getTimeInMillis()); // 初始化标题
    }

    /**
     * 设置是否24小时制显示
     * @param is24HourView true为24小时制，false为12小时制
     */
    public void set24HourView(boolean is24HourView) {
        mIs24HourView = is24HourView;
    }

    /**
     * 设置日期时间设置完成监听器
     * @param callBack 监听器实现
     */
    public void setOnDateTimeSetListener(OnDateTimeSetListener callBack) {
        mOnDateTimeSetListener = callBack;
    }

    /**
     * 更新对话框标题显示
     * @param date 当前日期时间(毫秒)
     */
    private void updateTitle(long date) {
        int flag =
            DateUtils.FORMAT_SHOW_YEAR | // 显示年份
            DateUtils.FORMAT_SHOW_DATE | // 显示日期
            DateUtils.FORMAT_SHOW_TIME;  // 显示时间
        
        // 设置时间格式
        flag |= mIs24HourView ? DateUtils.FORMAT_24HOUR : DateUtils.FORMAT_12HOUR;
        
        // 格式化日期时间并设置为标题
        setTitle(DateUtils.formatDateTime(this.getContext(), date, flag));
    }

    /**
     * 对话框按钮点击事件处理
     */
    @Override
    public void onClick(DialogInterface arg0, int arg1) {
        // 触发设置完成回调
        if (mOnDateTimeSetListener != null) {
            mOnDateTimeSetListener.OnDateTimeSet(this, mDate.getTimeInMillis());
        }
    }
}
