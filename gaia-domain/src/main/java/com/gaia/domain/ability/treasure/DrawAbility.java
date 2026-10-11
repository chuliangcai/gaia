package com.gaia.domain.ability.treasure;

import com.gaia.domain.base.activity.ActionType;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.domain.ability.treasure.dto.DrawResultDTO;
import com.gaia.domain.ability.treasure.dto.DrawWinnerDTO;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 开奖查询 Ability（C 端只读）。
 *
 * <p>subType：</p>
 * <ul>
 *   <li>{@code my_result} — 单用户中奖情况</li>
 *   <li>{@code activity_result} — 活动中奖名单</li>
 *   <li>{@code code_lookup} — 按码反查</li>
 * </ul>
 */
@Slf4j
@Component
@Ability(code = "treasure_draw", bizNs = "treasure", name = "开奖查询")
@RequiredArgsConstructor
public class DrawAbility extends AbstractBaseAbility {

    private static final String SUB_MY = "my_result";
    private static final String SUB_ACTIVITY = "activity_result";
    private static final String SUB_CODE_LOOKUP = "code_lookup";

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final MemberBenefitRepository memberBenefitRepository;
    private final ObjectMapper objectMapper;

    @Override
    public IFResult<?> execute(RequestContext context) {
        String sub = resolveSubType(context);
        return switch (sub) {
            case SUB_MY -> doMyResult(context);
            case SUB_ACTIVITY -> doActivityResult(context);
            case SUB_CODE_LOOKUP -> doCodeLookup(context);
            default -> IFResult.fail("UNKNOWN_SUBTYPE");
        };
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of(SUB_MY, SUB_ACTIVITY, SUB_CODE_LOOKUP);
    }

    private String resolveSubType(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        String sub = (ext == null) ? null : ext.get("subType");
        return (sub == null || sub.isBlank()) ? SUB_ACTIVITY : sub;
    }

    private IFResult<DrawResultDTO> doMyResult(RequestContext context) {
        Long userId = context.getUserId();
        if (userId == null || userId <= 0) {
            return IFResult.fail("USER_ID_REQUIRED");
        }
        Map<String, String> ext = context.getExtParams();
        if (ext == null) {
            return IFResult.fail("ACTIVITY_ID_REQUIRED");
        }
        Long activityId = parseLong(ext.get("activityId"));
        if (activityId == null) {
            return IFResult.fail("ACTIVITY_ID_REQUIRED");
        }
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        List<ActivityRecord> wins = activityRecordRepository.findByActivityAndType(activityId, ActionType.DRAW_WON);
        DrawWinnerDTO myWinner = null;
        for (ActivityRecord r : wins) {
            if (userId.equals(r.getUserId())) {
                Map<String, Object> result = parseJson(r.getResult());
                myWinner = DrawWinnerDTO.builder()
                        .userId(userId)
                        .code(String.valueOf(result.get("code") == null ? "" : result.get("code")))
                        .rank(toInt(result.get("rank")))
                        .weight(null)
                        .build();
                break;
            }
        }
        List<DrawWinnerDTO> winners = new ArrayList<>();
        if (myWinner != null) {
            winners.add(myWinner);
        }
        return IFResult.success(DrawResultDTO.builder()
                .activityId(activityId)
                .status(activity.getStatus())
                .drawAt(activity.getDrawAt())
                .winners(winners)
                .build());
    }

    private IFResult<DrawResultDTO> doActivityResult(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        if (ext == null) {
            return IFResult.fail("ACTIVITY_ID_REQUIRED");
        }
        Long activityId = parseLong(ext.get("activityId"));
        if (activityId == null) {
            return IFResult.fail("ACTIVITY_ID_REQUIRED");
        }
        Activity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        List<ActivityRecord> execRec = activityRecordRepository.findByActivityAndType(
                activityId, ActionType.DRAW_EXECUTED);
        List<DrawWinnerDTO> winners = new ArrayList<>();
        if (!execRec.isEmpty()) {
            // 取最新一条 DRAW_EXECUTED
            ActivityRecord latest = execRec.get(execRec.size() - 1);
            Map<String, Object> result = parseJson(latest.getResult());
            Object arrObj = result.get("winners");
            if (arrObj instanceof List<?> arr) {
                for (Object o : arr) {
                    if (o instanceof Map<?, ?> m) {
                        winners.add(DrawWinnerDTO.builder()
                                .userId(toLong(m.get("userId")))
                                .code(String.valueOf(m.get("code") == null ? "" : m.get("code")))
                                .rank(toInt(m.get("rank")))
                                .weight(toInt(m.get("weight")))
                                .build());
                    }
                }
            }
        }
        return IFResult.success(DrawResultDTO.builder()
                .activityId(activityId)
                .status(activity.getStatus())
                .drawAt(activity.getDrawAt())
                .winners(winners)
                .build());
    }

    private IFResult<DrawWinnerDTO> doCodeLookup(RequestContext context) {
        Map<String, String> ext = context.getExtParams();
        if (ext == null) {
            return IFResult.fail("CODE_REQUIRED");
        }
        String code = ext.get("code");
        Long activityId = parseLong(ext.get("activityId"));
        if (code == null || code.isBlank() || activityId == null) {
            return IFResult.fail("CODE_AND_ACTIVITY_REQUIRED");
        }
        // 反查：通过该活动的夺宝码找出该码所属用户
        List<String> myCodes = memberBenefitRepository
                .listLotteryCodesByUserAndActivity(context.getUserId() == null ? 0L : context.getUserId(),
                        activityId);
        // 简化：code_lookup 用 List<MemberBenefit> 的 repo.listByUserId 反查，
        // 这里退化为扫一次活动所有用户的码（由调用方传入 userId 限定）。
        // 实际生产可在 Mapper 加按码查询的 SQL。
        if (!myCodes.contains(code)) {
            return IFResult.fail("CODE_NOT_FOUND");
        }
        return IFResult.success(DrawWinnerDTO.builder()
                .userId(context.getUserId())
                .code(code)
                .rank(null)
                .weight(null)
                .build());
    }

    // --------------- helpers ---------------

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseJson(String s) {
        if (s == null || s.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JSON 反序列化失败: {}", s, e);
            return Map.of();
        }
    }
}