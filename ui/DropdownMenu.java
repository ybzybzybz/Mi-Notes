package net.micode.notes.ui;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

/**
 * 下拉菜单控件封装类
 * 将Button与PopupMenu组合实现下拉菜单功能
 */
public class DropdownMenu {
    private Button mButton;       // 作为下拉菜单触发器的按钮
    private PopupMenu mPopupMenu; // 弹出式菜单对象
    private Menu mMenu;           // 菜单对象

    /**
     * 构造函数
     * @param context 上下文环境
     * @param button 用于触发下拉菜单的按钮
     * @param menuId 菜单资源ID
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button;
        // 设置按钮背景为下拉图标
        mButton.setBackgroundResource(R.drawable.dropdown_icon);
        
        // 初始化PopupMenu，绑定到按钮
        mPopupMenu = new PopupMenu(context, mButton);
        // 获取菜单对象
        mMenu = mPopupMenu.getMenu();
        // 填充菜单布局
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
        
        // 设置按钮点击事件 - 点击时显示下拉菜单
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupMenu.show();
            }
        });
    }

    /**
     * 设置菜单项点击监听器
     * @param listener 菜单项点击监听器实现
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }

    /**
     * 查找菜单项
     * @param id 菜单项ID
     * @return 找到的菜单项，未找到返回null
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    /**
     * 设置按钮文本
     * @param title 要显示的文本
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}
