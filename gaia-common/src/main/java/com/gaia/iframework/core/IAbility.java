package com.gaia.iframework.core;

import java.util.List;

/**
 * 能力接口。
 *
 * <p>实现类通过 {@code @Ability}（位于 {@code com.gaia.iframework.annotation}）注册，
 * 由框架按 {@code ModelRequest.assertType} 分派执行。</p>
 */
public interface IAbility {

    /**
     * 在给定上下文下执行该能力。
     *
     * @param context 由 dispatcher 构建的运行时上下文
     * @return 业务结果
     */
    IFResult<?> execute(RequestContext context);

    /**
     * 列出该能力在指定玩法下的处理器名清单（仅用于读取/调试，不参与调度）。
     *
     * @param bizCode 玩法 code（可为 {@code null}）
     */
    List<String> getProcessor(String bizCode);
}
