package com.ron.commanderskills.client;

import com.ron.commanderskills.skill.CommanderSkillState;

import java.util.HashMap;
import java.util.Map;

public class CommanderSkillsClientState {

    private static final Map<String, CommanderSkillState> STATES = new HashMap<>();

    /** 将服务端下发的技能状态合并到本地：只更新包内包含的技能，其它技能保持不变，使各技能独立互不干扰。 */
    public static void updateFromServer(Map<String, CommanderSkillState> updates) {
        if (updates != null) {
            STATES.putAll(updates);
        }
    }

    public static CommanderSkillState get(String id) {
        return STATES.get(id);
    }

    /** 断开连接/退出世界时清空，避免显示错误等级 */
    public static void clear() {
        STATES.clear();
    }
}

