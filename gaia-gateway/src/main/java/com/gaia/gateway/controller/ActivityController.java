package com.gaia.gateway.controller;

import com.gaia.api.activity.ActivityQueryService;
import com.gaia.api.activity.ActivitySummaryDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 移动端活动列表 HTTP 入口。
 *
 * <p>对应 mobile {@code frontend-mobile/src/api/activities.js#getActivities()}
 * 调用的 {@code GET /api/activities}：返回当前"在线"的活动列表（领域 status ∈ {ONGOING, WAITING_DRAW}），
 * 用于会员中心"热门活动"卡片渲染。</p>
 *
 * <p>本 controller 只做 HTTP 透转，业务不过 controller：
 * 数据由 gaia-app 的 {@code ActivityQueryService} Dubbo provider 提供。
 * 详见 [[gaia-app-scope]]。</p>
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "活动列表", description = "移动端会员中心\"热门活动\"HTTP 入口")
public class ActivityController {

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private ActivityQueryService activityQueryService;

    @GetMapping
    @Operation(summary = "列出移动端可见的在线活动",
            description = "返回领域 status ∈ {ONGOING, WAITING_DRAW} 的活动；"
                    + "DTO.status 为 UI 状态（0=草稿/1=已上架/2=已下架）")
    public Result<List<ActivitySummaryDTO>> list() {
        return Result.success(activityQueryService.listLiveActivities());
    }
}