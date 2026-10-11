package com.gaia.backend.controller;

import com.gaia.backend.app.dto.MemberSeedDTO;
import com.gaia.backend.app.service.ActivityAdminAppService;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员管理后台接口。
 *
 * <p>直接走 {@link ActivityAdminAppService#seedMembers(List)}，
 * 不再走 Dubbo。</p>
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@Tag(name = "会员管理", description = "后台管理调用入口")
public class MemberAdminController {

    private final ActivityAdminAppService activityAdminAppService;

    @PostMapping("/seed")
    @Operation(summary = "灌入会员测试数据")
    public Result<Void> seed(@RequestBody List<MemberSeedDTO> members) {
        activityAdminAppService.seedMembers(members);
        return Result.success();
    }
}
