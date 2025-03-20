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

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;
//导入所需的包
public class Contact {
    private static HashMap<String, String> sContactCache;//缓存用于存储联系人信息的 HashMap，键为联系人号码，值为联系人名称
    private static final String TAG = "Contact";//// TAG用于 Log 输出，便于调试

    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";// 查询联系人的条件：通过电话号码匹配来查询，其中包含查找电话类型的数据，获取与此电话号码相关联的 raw_contact_id，查询 phone_lookup 表

    public static String getContact(Context context, String phoneNumber) {
        if(sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }// 如果联系人缓存为空，则初始化一个新的 HashMap 来存储联系人信息

        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }//如果缓存中已经包含该电话号码的联系人信息，则直接返回缓存中的联系人名称

        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));//替换CALLER_ID_SELECTION中的"+"为标准化后的电话号码
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String [] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);//使用 content resolver 查询联系人数据表，匹配给定的电话号码

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String name = cursor.getString(0);//获取查询结果中的联系人名称（DISPLAY_NAME字段）
                sContactCache.put(phoneNumber, name);// 将联系人名称存入缓存
                return name;
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, " Cursor get string error " + e.toString());// 捕获查询结果索引超出范围异常，输出错误日志
                return null;
            } finally {
                cursor.close();// 不管是否发生异常，都关闭游标以释放资源
            }
        } else {
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }//如果查询没有结果，输出日志并返回 null
}
//本段代码在小米便签中，当用户查看与某个电话号码相关的记录时，使用此代码从联系人数据库中查找该电话号码对应的联系人名称，
//同时并将结果缓存以提升性能。如果没有找到匹配的联系人，返回 null，并在日志中记录未找到的电话号码。