package com.gaia.api.member;

import java.util.List;

/**
 * 会员查询 Dubbo 服务（gateway 侧消费）。
 *
 * <p>用于支撑"切换当前用户"面板——gateway 不直连 DB，
 * 经由此 RPC 拿当前活跃用户列表。</p>
 */
public interface MemberQueryService {

    /**
     * 列出当前可切换的会员用户（status=1 的 DISTINCT user_id）。
     *
     * <p>若表中无任何有效会员，本方法会自动 seed 三个 dev 演示用户
     * （10001 / 10002 / 10003），便于本地首次启动即可使用切换面板。</p>
     */
    List<MemberSummaryDTO> listActiveMembers();
}