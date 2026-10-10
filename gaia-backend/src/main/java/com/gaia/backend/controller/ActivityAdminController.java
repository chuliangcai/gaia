package com.gaia.backend.controller;

import com.gaia.api.activity.ActivityAdminService;
import com.gaia.api.activity.ActivityCreateRequest;
import com.gaia.api.activity.ActivityUpdateRequest;
import com.gaia.api.activity.ActivityViewDTO;
import com.gaia.api.activity.TaskConfigDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动管理后台接口。
 *
 * <p>list / delete 直接走 {@link ActivityRepository}（不绕 gaia-bootstrap），
 * 因为后台管理对实时性、事务边界、领域规则没有强诉求；
 * create / update / configureTasks / triggerDraw 仍走 gaia-app 的 Dubbo 服务。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Tag(name = "活动管理", description = "后台管理调用入口")
public class ActivityAdminController {

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private ActivityAdminService activityAdminService;

    private final ActivityRepository activityRepository;

    @PostMapping
    @Operation(summary = "创建活动")
    public Result<Long> create(@RequestBody ActivityCreateRequest req) {
        if (req == null) {
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        Long id = activityAdminService.createActivity(req);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新活动")
    public Result<Void> update(@PathVariable Long id, @RequestBody ActivityUpdateRequest req) {
        activityAdminService.updateActivity(id, req);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "活动列表（后台管理）")
    public Result<List<ActivityViewDTO>> list() {
        List<ActivityViewDTO> rows = activityRepository.findAll().stream()
                .map(ActivityAdminController::toView)
                .toList();
        return Result.success(rows);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动")
    public Result<Void> delete(@PathVariable Long id) {
        activityRepository.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/tasks")
    @Operation(summary = "配置任务清单（覆盖式）")
    public Result<Void> configureTasks(@PathVariable Long id, @RequestBody List<TaskConfigDTO> tasks) {
        activityAdminService.configureTasks(id, tasks);
        return Result.success();
    }

    @PostMapping("/{id}/draw")
    @Operation(summary = "强制立即开奖")
    public Result<Void> triggerDraw(@PathVariable Long id) {
        activityAdminService.triggerDraw(id);
        return Result.success();
    }

    @GetMapping("/ping")
    @Operation(summary = "健康检查")
    public Result<String> ping() {
        return Result.success("pong");
    }

    /**
     * 领域 Activity → 后台列表 DTO。
     * 状态机映射：0=草稿 1=上架 2=下架。
     */
    private static ActivityViewDTO toView(Activity a) {
        Integer viewStatus;
        if (a.getStatus() == null) {
            viewStatus = 0;
        } else {
            switch (a.getStatus()) {
                case 0:
                    viewStatus = 0; // 未开始 → 草稿
                    break;
                case 1:
                case 2:
                    viewStatus = 1; // 进行中 / 待开奖 → 上架
                    break;
                default:
                    viewStatus = 2; // 已开奖 → 下架
            }
        }
        return ActivityViewDTO.builder()
                .id(a.getId())
                .activityCode(a.getActivityCode())
                .name(a.getName())
                .coverUrl(a.getPrizeImgUrl() == null ? "" : a.getPrizeImgUrl())
                .link(a.getId() == null ? "" : ("#/treasure/" + a.getId()))
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(viewStatus)
                .build();
    }
}