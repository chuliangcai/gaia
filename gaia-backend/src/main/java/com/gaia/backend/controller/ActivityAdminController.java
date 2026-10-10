package com.gaia.backend.controller;

import com.gaia.api.activity.ActivityAdminService;
import com.gaia.api.activity.ActivityCreateRequest;
import com.gaia.api.activity.ActivityUpdateRequest;
import com.gaia.api.activity.TaskConfigDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Tag(name = "活动管理", description = "后台管理调用入口")
public class ActivityAdminController {

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private ActivityAdminService activityAdminService;

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
}