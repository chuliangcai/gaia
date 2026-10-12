package com.gaia.backend.app.service;

import com.gaia.domain.ability.draw.CollectDrawAbility;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 开奖后台应用服务。
 *
 * <p>承接后台"强制立即开奖"用例：加载活动、构造 {@link RequestContext}、
 * 转发到 {@link CollectDrawAbility#execute(RequestContext)}，把失败/成功结果
 * 落地为日志。该能力原先挂在 ActivityAdminAppService 下（经端口 + Adapter 解耦），
 * 为了让活动只感知"活动"，已把它独立为单独的应用服务；目前只有一个实现，无需 Port 抽象。</p>
 *
 * <p>后续若新增定时开奖 / 自动重试等同类用例，直接在本服务上追加方法即可；
 * 活动侧不需要改动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrawAdminAppService {

    private final CollectDrawAbility collectDrawAbility;
    private final ActivityRepository activityRepository;

    public void triggerDraw(Long activityId) {
        if (activityId == null) {
            log.warn("triggerDraw 跳过：activityId 为空");
            return;
        }
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            log.warn("triggerDraw 跳过：活动不存在 activityId={}", activityId);
            return;
        }

        RequestContext ctx = new RequestContext();
        ctx.setUserId(0L);
        ctx.setBizCode(activity.getBizCode());
        ctx.setActivityId(String.valueOf(activityId));
        ctx.setExtParams(Map.of("activityId", String.valueOf(activityId)));

        IFResult<?> r = collectDrawAbility.execute(ctx);
        if (r == null) {
            log.warn("triggerDraw 返回 null activityId={}", activityId);
        } else if (!r.isSuccess()) {
            log.warn("triggerDraw 未成功 activityId={} msgCode={}", activityId, r.getMsgCode());
        } else {
            log.info("triggerDraw 成功 activityId={}", activityId);
        }
    }
}