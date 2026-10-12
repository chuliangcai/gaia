package com.gaia.domain.ability.draw;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia.domain.ability.draw.config.CollectDrawConfig;
import com.gaia.domain.ability.draw.dto.CollectDrawResultDTO;
import com.gaia.domain.ability.draw.ext.CalculateProbabilityExt;
import com.gaia.domain.ability.draw.ext.IssuePrizeExt;
import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.draw.DrawWinner;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.domain.service.treasure.WeightedDrawAlgorithm;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 集奖开奖 Ability（{@code collect_draw}，{@code bizNs="draw"}）。
 *
 * <p>该能力不覆写父类的 {@link #execute(RequestContext)}；执行骨架由
 * {@link AbstractBaseAbility} 默认实现按 {@link #getProcessor(String)} 返回的顺序
 * 反射调用同名 processor 方法：</p>
 * <ol>
 *     <li>{@link #preCheck(RequestContext) preCheck} —
 *     校验活动存在、状态必须为 {@code WAITING_DRAW}（2），从 {@link Activity#getConfigurations()}
 *     解析 {@link CollectDrawConfig} 并校验当前时间 ≥ {@code drawTime}；任一失败即短路。</li>
 *     <li>{@link #calculateProbabilities(RequestContext) calculateProbabilities} —
 *     调用扩展点 {@link CalculateProbabilityExt} 计算每个参与用户的中奖概率
 *     （{@code userId → probability}）。</li>
 *     <li>{@link #draw(RequestContext) draw} —
 *     将概率归一化为权重，调用 {@link WeightedDrawAlgorithm} 按权重不放回抽出
 *     {@link CollectDrawConfig#getTotalPrizeCount()} 名中奖者。</li>
 *     <li>{@link #issuePrize(RequestContext) issuePrize} —
 *     调用扩展点 {@link IssuePrizeExt} 得到待入库的 {@link MemberBenefit} 奖品条目，
 *     统一写入账本与活动明细（{@code DRAW_WON} + {@code DRAW_EXECUTED}），并将
 *     {@link Activity} 状态推进为 {@code DRAWN}（3）。</li>
 * </ol>
 *
 * <p>扩展点通过 {@link AbstractBaseAbility#reduceExecute(RequestContext, String,
 * AbstractBaseAbility.ExtensionCallback)} 调用，bizCode 取自运行时上下文，
 * 领域层不感知玩法归属。{@code issuePrize} 的事务子方法不调用扩展点 ——
 * {@link #reduceExecute} 要求非空 {@link RequestContext}，而事务方法由基类反射调用，
 * 无该入参；扩展点调用与事务边界分离。</p>
 */
@Slf4j
@Component
@Ability(code = "collect_draw", bizNs = "draw", name = "集奖开奖", configClass = CollectDrawConfig.class, backendTriggerable = true)
@RequiredArgsConstructor
public class CollectDrawAbility extends AbstractBaseAbility {

    /** 扩展点 code：计算参与用户概率 */
    private static final String EXT_CALC_PROBABILITY = "calc_probability";
    /** 扩展点 code：发放奖品 */
    private static final String EXT_ISSUE_PRIZE = "issue_prize";

    /** 处理器名：前置校验 */
    private static final String PROC_PRE_CHECK = "preCheck";
    /** 处理器名：计算概率 */
    private static final String PROC_CALC_PROB = "calculateProbabilities";
    /** 处理器名：按概率抽奖 */
    private static final String PROC_DRAW = "draw";
    /** 处理器名：发放奖品 */
    private static final String PROC_ISSUE_PRIZE = "issuePrize";

    /** 幂等键前缀。完整 key 形如 {@code collect_draw:{activityId}} */
    private static final String ACTION_CODE_PREFIX = "collect_draw";

    /** 概率 → 权重的归一化放大倍数；保证权重为正整数且相对比例稳定。 */
    private static final int PROBABILITY_WEIGHT_SCALE = 1_000_000;

    private static final String CONFIG_KEY = CollectDrawConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final WeightedDrawAlgorithm drawAlgorithm;
    private final ObjectMapper objectMapper;

    // ---------------------- processor 声明 ----------------------

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(PROC_PRE_CHECK, PROC_CALC_PROB, PROC_DRAW, PROC_ISSUE_PRIZE);
    }

    // ---------------------- processor 实现 ----------------------

    /**
     * 前置校验：解析入参 → 加载活动 → 校验状态 → 加载配置 → 校验开奖时间。
     * 校验失败时短路返回对应 msgCode；成功时把活动与配置塞入 {@link ExecutionContext}。
     */
    IFResult<CollectDrawResultDTO> preCheck(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        if (!ec.activity.isWaitingDraw()) {
            if (ec.activity.isDrawn()) {
                return IFResult.fail("DRAW_ALREADY_EXECUTED");
            }
            return IFResult.fail("ACTIVITY_NOT_DRAWABLE");
        }
        if (ec.config == null) {
            return IFResult.fail("DRAW_CONFIG_MISSING");
        }
        if (ec.config.getDrawTime() == null) {
            return IFResult.fail("DRAW_TIME_MISSING");
        }
        if (LocalDateTime.now().isBefore(ec.config.getDrawTime())) {
            return IFResult.fail("DRAW_NOT_DUE");
        }
        return IFResult.success(CollectDrawResultDTO.builder()
                .activityId(ec.activityId)
                .status(ec.activity.getStatus())
                .drawAt(LocalDateTime.now())
                .winners(Collections.emptyList())
                .prizes(Collections.emptyList())
                .build());
    }

    /**
     * 计算概率：调用 {@link CalculateProbabilityExt} 扩展点。
     * 概率为 {@code null} / 空 / 全为非正数时视为无人参与，本步直接返回空结果。
     */
    IFResult<CollectDrawResultDTO> calculateProbabilities(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }

        Map<Long, Double> rawProbs = reduceExecute(
                context, EXT_CALC_PROBABILITY,
                ext -> ((CalculateProbabilityExt) ext).calculate(ec.activityId));
        Map<Long, Double> cleaned = new HashMap<>();
        double sum = 0d;
        if (rawProbs != null) {
            for (Map.Entry<Long, Double> e : rawProbs.entrySet()) {
                Long uid = e.getKey();
                Double p = e.getValue();
                if (uid == null || p == null || !Double.isFinite(p) || p <= 0d) {
                    continue;
                }
                if (cleaned.containsKey(uid)) {
                    continue;
                }
                cleaned.put(uid, p);
                sum += p;
            }
        }
        ec.probabilities = cleaned;
        ec.probabilitySum = sum;
        return IFResult.success(CollectDrawResultDTO.builder()
                .activityId(ec.activityId)
                .build());
    }

    /**
     * 按概率抽奖：归一化为权重 → 调 {@link WeightedDrawAlgorithm}。
     * {@code totalPrizeCount <= 0} 或概率总和 ≤ 0 时视为"无人中奖"。
     */
    IFResult<CollectDrawResultDTO> draw(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }
        Integer totalPrizeCount = ec.config.getTotalPrizeCount();
        if (totalPrizeCount == null || totalPrizeCount <= 0) {
            log.info("totalPrizeCount 未配置或非正，跳过抽奖 activityId={}", ec.activityId);
            ec.winners = Collections.emptyList();
            return IFResult.success(CollectDrawResultDTO.builder()
                    .activityId(ec.activityId)
                    .build());
        }
        if (ec.probabilities == null || ec.probabilities.isEmpty() || ec.probabilitySum <= 0d) {
            log.info("无有效参与用户，跳过抽奖 activityId={}", ec.activityId);
            ec.winners = Collections.emptyList();
            return IFResult.success(CollectDrawResultDTO.builder()
                    .activityId(ec.activityId)
                    .build());
        }

        List<DrawCandidate> candidates = new ArrayList<>(ec.probabilities.size());
        for (Map.Entry<Long, Double> e : ec.probabilities.entrySet()) {
            // weight = max(1, round(p / sum * SCALE))；保证至少为 1 以进入候选池。
            long weight = Math.max(1L, Math.round(
                    (e.getValue() / ec.probabilitySum) * PROBABILITY_WEIGHT_SCALE));
            candidates.add(DrawCandidate.builder()
                    .userId(e.getKey())
                    .weight((int) Math.min(Integer.MAX_VALUE, weight))
                    .build());
        }

        List<DrawWinner> winners = drawAlgorithm.draw(candidates, totalPrizeCount);
        // 给每个中奖者从其夺宝码集合中取一条作为中奖码
        for (DrawWinner w : winners) {
            List<String> userCodes = memberBenefitRepository
                    .listLotteryCodesByUserAndActivity(w.getUserId(), ec.activityId);
            w.setCode(userCodes.isEmpty() ? null : userCodes.get(0));
        }
        ec.winners = winners;
        log.info("抽奖完成 activityId={} candidateSize={} totalPrizeCount={} winners={}",
                ec.activityId, candidates.size(), totalPrizeCount, winners.size());
        return IFResult.success(CollectDrawResultDTO.builder()
                .activityId(ec.activityId)
                .winners(winners)
                .build());
    }

    /**
     * 发放奖品：先调扩展点拿到奖品条目，再进入事务写奖品账本 + {@code DRAW_WON}
     * 与 {@code DRAW_EXECUTED} 明细 + 活动状态推进为 {@code DRAWN}；幂等命中时
     * 复用上次结果直接返回。
     */
    IFResult<CollectDrawResultDTO> issuePrize(RequestContext context) {
        ExecutionContext ec = resolveContext(context);
        if (ec.errorMsgCode != null) {
            return IFResult.fail(ec.errorMsgCode);
        }

        String actionCode = buildActionCode(ec.activityId);
        Optional<ActivityRecord> existing = activityRecordRepository.findByActionCode(actionCode);
        if (existing.isPresent()) {
            // 幂等命中：复用上次结果，不再重复写奖品 / 推进状态
            return buildIdempotentResult(ec, existing.get());
        }

        List<DrawWinner> winners = ec.winners == null ? Collections.emptyList() : ec.winners;
        List<MemberBenefit> prizes = reduceExecute(
                context, EXT_ISSUE_PRIZE,
                ext -> ((IssuePrizeExt) ext).issue(ec.activityId, winners));
        if (prizes == null) {
            prizes = Collections.emptyList();
        }

        return doIssuePrizeTx(ec, actionCode, winners, prizes);
    }

    // ---------------------- 事务 ----------------------

    /**
     * 事务子方法：仅负责 DB 写入（账本 + 明细 + 活动状态）。
     * 扩展点已在 {@link #issuePrize(RequestContext)} 中调用，避免事务子方法需要
     * {@link RequestContext} 才能调 {@link #reduceExecute} 的循环依赖。
     */
    @Transactional(rollbackFor = Exception.class)
    IFResult<CollectDrawResultDTO> doIssuePrizeTx(ExecutionContext ec,
                                                  String actionCode,
                                                  List<DrawWinner> winners,
                                                  List<MemberBenefit> prizes) {
        // 1. 写奖品账本
        int inserted = memberBenefitRepository.insertBenefits(prizes);

        // 2. 写 DRAW_WON 明细（按 user 幂等）
        for (DrawWinner w : winners) {
            ActivityRecord rec = ActivityRecord.builder()
                    .userId(w.getUserId())
                    .activityId(ec.activityId)
                    .actionType(ActionType.DRAW_WON)
                    .actionCode("draw_win:" + ec.activityId + ":" + w.getUserId())
                    .bizId(ec.activityId)
                    .payload(null)
                    .result("{\"rank\":" + w.getRank()
                            + ",\"code\":\"" + safeCode(w.getCode()) + "\"}")
                    .createdAt(LocalDateTime.now())
                    .build();
            activityRecordRepository.insertIfAbsent(rec);
        }

        // 3. 写 DRAW_EXECUTED 总明细（幂等键 = actionCode）
        String winnersJson = buildWinnersJson(winners);
        ActivityRecord exec = ActivityRecord.builder()
                .userId(0L)
                .activityId(ec.activityId)
                .actionType(ActionType.DRAW_EXECUTED)
                .actionCode(actionCode)
                .bizId(ec.activityId)
                .payload(null)
                .result("{\"winners\":" + winnersJson + "}")
                .createdAt(LocalDateTime.now())
                .build();
        boolean execInserted = activityRecordRepository.insertIfAbsent(exec);
        if (!execInserted) {
            // 并发场景：另一线程先写入，回退为幂等结果
            Optional<ActivityRecord> prev = activityRecordRepository.findByActionCode(actionCode);
            if (prev.isPresent()) {
                return buildIdempotentResult(ec, prev.get());
            }
            return IFResult.fail("DUPLICATE_REQUEST");
        }

        // 4. 活动状态推进 2 → 3
        Activity activity = ec.activity;
        activity.markDrawn();
        try {
            activityRepository.save(activity);
        } catch (Exception e) {
            log.error("活动状态推进 2→3 失败 activityId={}", ec.activityId, e);
            throw e;
        }

        log.info("集奖开奖完成 activityId={} winners={} prizesInserted={}",
                ec.activityId, winners.size(), inserted);

        return IFResult.success(CollectDrawResultDTO.builder()
                .activityId(ec.activityId)
                .status(activity.getStatus())
                .drawAt(LocalDateTime.now())
                .winners(winners)
                .prizes(prizes)
                .build());
    }

    // ---------------------- 入参解析 ----------------------

    /**
     * 从 {@link RequestContext} 解析入参，加载活动与配置。校验失败时
     * {@link ExecutionContext#errorMsgCode} 非空。
     */
    ExecutionContext resolveContext(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        String rawId = ext == null ? null : ext.get("activityId");
        if (rawId == null || rawId.isBlank()) {
            return ExecutionContext.fail("ACTIVITY_ID_REQUIRED");
        }
        Long activityId;
        try {
            activityId = Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            return ExecutionContext.fail("ACTIVITY_ID_INVALID");
        }
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return ExecutionContext.fail("ACTIVITY_NOT_FOUND");
        }
        CollectDrawConfig cfg = loadConfig(activity);
        return ExecutionContext.ok(activityId, activity, cfg);
    }

    String buildActionCode(Long activityId) {
        return ACTION_CODE_PREFIX + ":" + activityId;
    }

    IFResult<CollectDrawResultDTO> buildIdempotentResult(ExecutionContext ec, ActivityRecord rec) {
        Map<String, Object> result = parseJson(rec.getResult());
        List<DrawWinner> winners = parseWinners(result.get("winners"));
        log.info("集奖开奖幂等命中 activityId={} recordId={} winners={}",
                ec.activityId, rec.getId(), winners.size());
        return IFResult.success(CollectDrawResultDTO.builder()
                .activityId(ec.activityId)
                .status(ec.activity.getStatus())
                .drawAt(rec.getCreatedAt())
                .winners(winners)
                .prizes(Collections.emptyList())
                .build());
    }

    // ---------------------- helpers ----------------------

    /**
     * 从 {@link Activity#getConfigurations()} 反序列化 {@link CollectDrawConfig}。
     * 配置缺失或解析失败时返回 {@code null}（{@link #preCheck} 据此短路）。
     */
    CollectDrawConfig loadConfig(Activity activity) {
        String raw = activity.getConfigurations();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    raw, new TypeReference<Map<String, Object>>() {});
            Object payload = root.get(CONFIG_KEY);
            if (payload == null) {
                return null;
            }
            return objectMapper.convertValue(payload, CollectDrawConfig.class);
        } catch (Exception e) {
            log.warn("反序列化 CollectDrawConfig 失败 activityId={}", activity.getId(), e);
            return null;
        }
    }

    private static String safeCode(String code) {
        return code == null ? "" : code.replace("\"", "\\\"");
    }

    private static String buildWinnersJson(List<DrawWinner> winners) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < winners.size(); i++) {
            DrawWinner w = winners.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"userId\":").append(w.getUserId())
                    .append(",\"code\":\"").append(safeCode(w.getCode())).append("\"")
                    .append(",\"rank\":").append(w.getRank())
                    .append(",\"weight\":").append(w.getWeight() == null ? 0 : w.getWeight())
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private Map<String, Object> parseJson(String s) {
        if (s == null || s.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 反序列化失败: {}", s, e);
            return Collections.emptyMap();
        }
    }

    private List<DrawWinner> parseWinners(Object arrObj) {
        if (!(arrObj instanceof List<?> arr)) {
            return Collections.emptyList();
        }
        List<DrawWinner> out = new ArrayList<>(arr.size());
        for (Object o : arr) {
            if (!(o instanceof Map<?, ?> m)) {
                continue;
            }
            Long userId = toLong(m.get("userId"));
            if (userId == null) {
                continue;
            }
            out.add(DrawWinner.builder()
                    .userId(userId)
                    .code(String.valueOf(m.get("code")))
                    .rank(toInt(m.get("rank")))
                    .weight(toInt(m.get("weight")))
                    .build());
        }
        return out;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------- 内部数据结构 ----------------------

    /**
     * processor 间共享的入参上下文。{@link #errorMsgCode} 为 {@code null} 表示成功。
     * 成功时依次携带 {@link #activityId} / {@link #activity} / {@link #config}；
     * {@link #probabilities} / {@link #probabilitySum} 在 {@code calculateProbabilities} 后填充，
     * {@link #winners} 在 {@code draw} 后填充。
     */
    static final class ExecutionContext {
        String errorMsgCode;
        Long activityId;
        Activity activity;
        CollectDrawConfig config;
        Map<Long, Double> probabilities;
        double probabilitySum;
        List<DrawWinner> winners;

        static ExecutionContext ok(Long activityId, Activity activity, CollectDrawConfig config) {
            ExecutionContext ec = new ExecutionContext();
            ec.activityId = activityId;
            ec.activity = activity;
            ec.config = config;
            return ec;
        }

        static ExecutionContext fail(String msgCode) {
            ExecutionContext ec = new ExecutionContext();
            ec.errorMsgCode = msgCode;
            return ec;
        }
    }
}
