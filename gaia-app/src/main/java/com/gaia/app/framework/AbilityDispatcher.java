package com.gaia.app.framework;

import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.GatewayMtopRequest;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.IAbility;
import com.gaia.iframework.core.ModelRequest;
import com.gaia.iframework.core.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 能力分派器。
 *
 * <p>启动时按 {@link Ability#code()} 索引所有 {@link Ability}-annotated bean，
 * 之后对一笔玩法请求内的 {@link ModelRequest} 列表逐个分派并聚集成结果。</p>
 *
 * <p>当前 v1 按模型请求顺序串行执行，{@code GatewayMtopRequest.parallel} 标志
 * 保留不读，留待 v2 切到 {@code CompletableFuture} 并发。</p>
 */
@Slf4j
@Component
public class AbilityDispatcher implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final ConcurrentMap<String, IAbility> abilities = new ConcurrentHashMap<>();

    public AbilityDispatcher() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Ability.class);
        for (Object bean : beans.values()) {
            // 关键：bean.getClass() 在 @Transactional / @Async 代理下返回的是 CGLIB 子类，
            // 直接 getAnnotation 会拿不到；用 AnnotationUtils.findAnnotation 会沿着代理/父类向上找。
            Ability ann = AnnotationUtils.findAnnotation(bean.getClass(), Ability.class);
            if (ann == null) {
                log.warn("skip bean without resolvable @Ability, class={}", bean.getClass().getName());
                continue;
            }
            String code = ann.code();
            if (abilities.putIfAbsent(code, (IAbility) bean) != null) {
                throw new IllegalStateException("Duplicate @Ability code=" + code
                        + " impl=" + bean.getClass().getName());
            }
            log.info("registered @Ability code={} bizNs={} name={} impl={}",
                    code, ann.bizNs(), ann.name(), bean.getClass().getSimpleName());
        }
        log.info("AbilityDispatcher initialized, abilityCount={}", abilities.size());
    }

    /**
     * 对一笔玩法请求内的每个 model 串行分派并聚合结果。
     *
     * @param req 网关请求体
     * @return key=能力 code (assertType), value=该能力的执行结果。
     *         空 {@code models} 时返回空 map（不报错）。
     */
    public Map<String, IFResult<?>> dispatch(GatewayMtopRequest req) {
        Map<String, IFResult<?>> out = new LinkedHashMap<>();
        List<ModelRequest> models = req == null ? null : req.getModels();
        if (models == null || models.isEmpty()) {
            return out;
        }
        for (ModelRequest m : models) {
            String key = m.getAssertType();
            IAbility ability = abilities.get(key);
            if (ability == null) {
                log.warn("ability not found, code={}", key);
                out.put(key, IFResult.fail("ABILITY_NOT_FOUND"));
                continue;
            }
            try {
                IFResult<?> r = ability.execute(toContext(req, m));
                out.put(key, r);
            } catch (Exception e) {
                log.error("ability execute failed code={}", key, e);
                out.put(key, IFResult.fail("ABILITY_EXECUTE_ERROR"));
            }
        }
        return out;
    }

    private RequestContext toContext(GatewayMtopRequest req, ModelRequest m) {
        RequestContext c = new RequestContext();
        c.setBizCode(req.getBizCode());
        c.setUserId(req.getUserId());
        c.setAssetType(m.getAssertType());
        Map<String, String> ext = new HashMap<>();
        ext.put("subType", m.getSubType() == null ? "" : m.getSubType());
        if (m.getExtParams() != null) {
            ext.putAll(m.getExtParams());
        }
        c.setExtParams(ext);
        return c;
    }
}
