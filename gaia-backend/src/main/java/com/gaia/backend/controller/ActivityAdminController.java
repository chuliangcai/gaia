package com.gaia.backend.controller;

import com.gaia.backend.app.dto.ActivityCreateRequest;
import com.gaia.backend.app.dto.ActivityUpdateRequest;
import com.gaia.backend.app.dto.ActivityViewDTO;
import com.gaia.backend.app.dto.TaskConfigDTO;
import com.gaia.backend.app.service.ActivityAdminAppService;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
 * <p>全部走 {@link ActivityAdminAppService}（gaia-backend 自带应用层），不再走 Dubbo：
 * <ul>
 *   <li>list / delete 由 AppService.listActivities / deleteActivity 完成</li>
 *   <li>create / update / configureTasks / triggerDraw 由 AppService 编排 domain 完成</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Tag(name = "活动管理", description = "后台管理调用入口")
public class ActivityAdminController {

    private final ActivityAdminAppService activityAdminAppService;

    @PostMapping
    @Operation(summary = "创建活动")
    public Result<Long> create(@RequestBody ActivityCreateRequest req) {
        if (req == null) {
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        Long id = activityAdminAppService.createActivity(req);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新活动")
    public Result<Void> update(@PathVariable Long id, @RequestBody ActivityUpdateRequest req) {
        activityAdminAppService.updateActivity(id, req);
        return Result.success();
    }

    @GetMapping("/list")
    @Operation(summary = "活动列表（后台管理）")
    public Result<List<ActivityViewDTO>> list() {
        return Result.success(activityAdminAppService.listActivities());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除活动")
    public Result<Void> delete(@PathVariable Long id) {
        activityAdminAppService.deleteActivity(id);
        return Result.success();
    }

    @PutMapping("/{id}/tasks")
    @Operation(summary = "配置任务清单（覆盖式）")
    public Result<Void> configureTasks(@PathVariable Long id, @RequestBody List<TaskConfigDTO> tasks) {
        activityAdminAppService.configureTasks(id, tasks);
        return Result.success();
    }

    @PostMapping("/{id}/draw")
    @Operation(summary = "强制立即开奖")
    public Result<Void> triggerDraw(@PathVariable Long id) {
        activityAdminAppService.triggerDraw(id);
        return Result.success();
    }

    @GetMapping("/ping")
    @Operation(summary = "健康检查")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
