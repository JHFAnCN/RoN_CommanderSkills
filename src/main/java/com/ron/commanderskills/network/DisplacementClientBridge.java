package com.ron.commanderskills.network;

import java.util.function.Consumer;

/**
 * 桥接：网络包（主源码集）不直接引用客户端类，由客户端在加载时注册回调。
 * 错位传送“已选中第一个单位”时服务端发 S2C，此处回调在客户端执行，参数为第一个目标实体 ID。
 */
public class DisplacementClientBridge {

    /** 客户端收到 DisplacementFirstTargetS2CPacket 时调用，参数为第一个目标实体 ID，由客户端在初始化时赋值 */
    public static Consumer<Integer> onFirstTargetReceived = null;
}
