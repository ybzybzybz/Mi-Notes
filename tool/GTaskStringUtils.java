/*
 * 该类定义了一组常量，用于与 Google Tasks（GTask）服务进行数据交换时所使用的 JSON 字段名。
 * 它还包含了一些用于标识笔记类型、文件夹分类、元数据等的标识符。
 * 主要用途是作为同步过程中构造/解析 JSON 的字段名引用，统一字段命名，减少硬编码。
 */
package net.micode.notes.tool;

public class GTaskStringUtils {

    // 操作相关字段
    public final static String GTASK_JSON_ACTION_ID = "action_id";               // 操作 ID
    public final static String GTASK_JSON_ACTION_LIST = "action_list";           // 操作列表
    public final static String GTASK_JSON_ACTION_TYPE = "action_type";           // 操作类型

    // 操作类型的具体值
    public final static String GTASK_JSON_ACTION_TYPE_CREATE = "create";         // 创建
    public final static String GTASK_JSON_ACTION_TYPE_GETALL = "get_all";        // 获取全部
    public final static String GTASK_JSON_ACTION_TYPE_MOVE = "move";             // 移动
    public final static String GTASK_JSON_ACTION_TYPE_UPDATE = "update";         // 更新

    // 实体属性字段
    public final static String GTASK_JSON_CREATOR_ID = "creator_id";             // 创建者 ID
    public final static String GTASK_JSON_CHILD_ENTITY = "child_entity";         // 子实体
    public final static String GTASK_JSON_CLIENT_VERSION = "client_version";     // 客户端版本
    public final static String GTASK_JSON_COMPLETED = "completed";               // 是否完成
    public final static String GTASK_JSON_CURRENT_LIST_ID = "current_list_id";   // 当前列表 ID
    public final static String GTASK_JSON_DEFAULT_LIST_ID = "default_list_id";   // 默认列表 ID
    public final static String GTASK_JSON_DELETED = "deleted";                   // 是否已删除
    public final static String GTASK_JSON_DEST_LIST = "dest_list";               // 目标列表 ID
    public final static String GTASK_JSON_DEST_PARENT = "dest_parent";           // 目标父项 ID
    public final static String GTASK_JSON_DEST_PARENT_TYPE = "dest_parent_type"; // 目标父项类型
    public final static String GTASK_JSON_ENTITY_DELTA = "entity_delta";         // 实体变化值
    public final static String GTASK_JSON_ENTITY_TYPE = "entity_type";           // 实体类型
    public final static String GTASK_JSON_GET_DELETED = "get_deleted";           // 是否获取已删除的项
    public final static String GTASK_JSON_ID = "id";                             // 实体 ID
    public final static String GTASK_JSON_INDEX = "index";                       // 排序索引
    public final static String GTASK_JSON_LAST_MODIFIED = "last_modified";       // 最后修改时间
    public final static String GTASK_JSON_LATEST_SYNC_POINT = "latest_sync_point"; // 最近同步点
    public final static String GTASK_JSON_LIST_ID = "list_id";                   // 所属列表 ID
    public final static String GTASK_JSON_LISTS = "lists";                       // 列表集合
    public final static String GTASK_JSON_NAME = "name";                         // 名称
    public final static String GTASK_JSON_NEW_ID = "new_id";                     // 新 ID（替换旧 ID）
    public final static String GTASK_JSON_NOTES = "notes";                       // 笔记内容
    public final static String GTASK_JSON_PARENT_ID = "parent_id";               // 父项 ID
    public final static String GTASK_JSON_PRIOR_SIBLING_ID = "prior_sibling_id"; // 前一个兄弟节点 ID
    public final static String GTASK_JSON_RESULTS = "results";                   // 返回结果
    public final static String GTASK_JSON_SOURCE_LIST = "source_list";           // 源列表 ID
    public final static String GTASK_JSON_TASKS = "tasks";                       // 任务集合
    public final static String GTASK_JSON_TYPE = "type";                         // 类型字段
    public final static String GTASK_JSON_TYPE_GROUP = "GROUP";                  // 类型：分组
    public final static String GTASK_JSON_TYPE_TASK = "TASK";                    // 类型：任务
    public final static String GTASK_JSON_USER = "user";                         // 用户字段

    // MIUI 文件夹前缀标识（用于区分系统文件夹）
    public final static String MIUI_FOLDER_PREFFIX = "[MIUI_Notes]";

    // 特殊文件夹名称
    public final static String FOLDER_DEFAULT = "Default";                       // 默认文件夹
    public final static String FOLDER_CALL_NOTE = "Call_Note";                   // 通话笔记文件夹
    public final static String FOLDER_META = "METADATA";                         // 元数据文件夹

    // 元数据头字段，用于同步时标记内容类型
    public final static String META_HEAD_GTASK_ID = "meta_gid";                  // GTask ID
    public final static String META_HEAD_NOTE = "meta_note";                     // 笔记元信息
    public final static String META_HEAD_DATA = "meta_data";                     // 数据元信息

    // 元信息的默认名称
    public final static String META_NOTE_NAME = "[META INFO] DON'T UPDATE AND DELETE"; // 不可修改和删除的标识名
}
