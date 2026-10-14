package com.gaia.backend.app.service;

import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.IAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 后台手动触发能力应用服务（泛化版）。
 *
 * <p>承接后台"对指定活动人工触发某个能力"用例：加载活动、校验该能力
 * {@link Ability#backendTriggerable()} 为 {@code true}、构造
 * {@link RequestContext}、转发到对应 {@link IAbility#execute(RequestContext)}，
 * 把失败/成功结果落地为日志。</p>
 *
 * <p>与 {@code DrawAdminAppService} 的关系：本服务是其泛化版，按
 * {@code @Ability.code()} 路由到任意已注册的能力 bean；{@code DrawAdminAppService}
 * 作为历史端点（{@code POST /api/admin/activity/{id}/draw}）的承担者保留不变。</p>
 *
 * <p>启动期通过 Spring 注入所有 {@link IAbility} bean，按 {@code @Ability.code()}
 * 建立索引；后续不再扫描，结构在容器生命周期内不变。</p>
 *
 * <p>安全约束：即便前端绕过 UI 调用本服务，仍会在运行期校验
 * {@code backendTriggerable=true}，未声明可触发的能力会抛
 * {@link IllegalStateException} 拒绝触发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbilityTriggerAppService {

    /** Spring 自动注入所有实现 {@link IAbility} 的 bean。 */
    private final List<IAbility> abilities;
    private final ActivityRepository activityRepository;

    /** {@code @Ability.code()} → bean 实例。容器启动后结构不变。
     *  非 final 是为了绕开 JDT-LS 内嵌 Lombok 1.18.39 对 {@code @RequiredArgsConstructor} + 已初始化 final 字段的误判；
     *  实际引用不会被赋值（{@link #init()} 只调用 {@code putIfAbsent}），等同于 final。 */
    private ConcurrentMap<String, IAbility> byCode = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        for (IAbility ability : abilities) {
            // 与 AbilityMetaRegistry 同款：走 AnnotationUtils 避免 CGLIB 代理遮蔽注解
            Ability ann = AnnotationUtils.findAnnotation(ability.getClass(), Ability.class);
            if (ann == null) {
                log.warn("skip bean without resolvable @Ability, class={}", ability.getClass().getName());
                continue;
            }
            IAbility prev = byCode.putIfAbsent(ann.code(), ability);
            if (prev != null) {
                log.warn("duplicate ability code in trigger registry, code={} prev={} curr={}",
                        ann.code(), prev.getClass().getName(), ability.getClass().getName());
            }
        }
        log.info("AbilityTriggerAppService initialized, count={}", byCode.size());
    }

    /**
     * 对指定活动触发指定能力。
     *
     * <p>执行流程：code 索引查找 → 注解校验（{@code backendTriggerable=true}）→
     * 加载活动 → 构造 {@link RequestContext} → {@link IAbility#execute}。
     * 任一前置步骤失败（参数空 / code 未知 / 能力不允许触发 / 活动不存在）
     * 均记录日志后直接返回，不抛到 controller。</p>
     *
     * @param activityId 活动主键；为空时跳过
     * @param code       能力 code（{@code @Ability.code()}）；为空或找不到时跳过
     */
    public void trigger(Long activityId, String code) {
        if (activityId == null) {
            log.warn("trigger 跳过：activityId 为空");
            return;
        }
        if (code == null || code.isBlank()) {
            log.warn("trigger 跳过：ability code 为空 activityId={}", activityId);
            return;
        }

        IAbility ability = byCode.get(code);
        if (ability == null) {
            log.warn("trigger 跳过：未知能力 code={} activityId={}", code, activityId);
            return;
        }
        Ability ann = AnnotationUtils.findAnnotation(ability.getClass(), Ability.class);
        if (ann == null || !ann.backendTriggerable()) {
            log.warn("trigger 跳过：能力不允许后台触发 code={} activityId={} backendTriggerable={}",
                    code, activityId, ann == null ? "n/a" : ann.backendTriggerable());
            return;
        }

        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            log.warn("trigger 跳过：活动不存在 activityId={} code={}", activityId, code);
            return;
        }

        RequestContext ctx = new RequestContext();
        ctx.setUserId(0L);
        ctx.setBizCode(activity.getBizCode());
        ctx.setActivityId(String.valueOf(activityId));
        ctx.setExtParams(Map.of("activityId", String.valueOf(activityId)));

        IFResult<?> r = ability.execute(ctx);
        if (r == null) {
            log.warn("trigger 返回 null activityId={} code={}", activityId, code);
        } else if (!r.isSuccess()) {
            log.warn("trigger 未成功 activityId={} code={} msgCode={}",
                    activityId, code, r.getMsgCode());
        } else {
            log.info("trigger 成功 activityId={} code={}", activityId, code);
        }
    }
}
