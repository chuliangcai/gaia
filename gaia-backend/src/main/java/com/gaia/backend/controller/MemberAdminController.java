package com.gaia.backend.controller;

import com.gaia.api.activity.ActivityAdminService;
import com.gaia.api.activity.MemberSeedDTO;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员管理后台接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@Tag(name = "会员管理", description = "后台管理调用入口")
public class MemberAdminController {

    @DubboReference(group = DubboConstants.GROUP, version = DubboConstants.VERSION)
    private ActivityAdminService activityAdminService;

    @PostMapping("/seed")
    @Operation(summary = "灌入会员测试数据")
    public Result<Void> seed(@RequestBody List<MemberSeedDTO> members) {
        activityAdminService.seedMembers(members);
        return Result.success();
    }
}