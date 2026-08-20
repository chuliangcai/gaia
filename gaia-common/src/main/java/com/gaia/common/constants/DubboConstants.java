package com.gaia.common.constants;

/**
 * Dubbo RPC 公共常量。
 *
 * <p>所有 Provider / Consumer 共享同一 group 与默认 version，
 * 避免各业务接口在自身类中重复声明。</p>
 */
public final class DubboConstants {

    private DubboConstants() {
    }

    /** 统一服务组：所有 RPC 服务归属于同一 group，便于跨模块直连与注册中心路由 */
    public static final String GROUP = "gaia";

    /** 默认接口版本 */
    public static final String VERSION = "1.0.0";
}
