package com.gaia.backend.controller;

import com.gaia.backend.app.dto.MemberCreateRequest;
import com.gaia.backend.app.dto.MemberUpdateRequest;
import com.gaia.backend.app.dto.MemberViewDTO;
import com.gaia.backend.app.service.MemberAdminAppService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员管理后台接口。
 *
 * <p>全部走 {@link MemberAdminAppService}（gaia-backend 自带应用层），不再走 Dubbo。
 * 路径变量使用 {@code userId}（与领域自然键一致），按 id 倒序列表，
 * 可选按 status / level 过滤。</p>
 */
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@Tag(name = "会员管理", description = "后台管理调用入口")
public class MemberAdminController {

    private final MemberAdminAppService memberAdminAppService;

    @PostMapping
    @Operation(summary = "新增会员")
    public Result<Long> create(@RequestBody MemberCreateRequest req) {
        if (req == null) {
            return Result.fail(Result.CodeEnum.BAD_REQUEST);
        }
        Long userId = memberAdminAppService.createMember(req);
        return Result.success(userId);
    }

    @GetMapping("/list")
    @Operation(summary = "会员列表（后台管理，按 id 倒序，可按 status / level 过滤）")
    public Result<List<MemberViewDTO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String level) {
        return Result.success(memberAdminAppService.listMembers(status, level));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "会员详情")
    public Result<MemberViewDTO> detail(@PathVariable Long userId) {
        return Result.success(memberAdminAppService.getMember(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "更新会员（仅修改非空字段：level / status / expireAt）")
    public Result<Void> update(@PathVariable Long userId, @RequestBody MemberUpdateRequest req) {
        memberAdminAppService.updateMember(userId, req);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除会员（物理删除）")
    public Result<Void> delete(@PathVariable Long userId) {
        memberAdminAppService.deleteMember(userId);
        return Result.success();
    }
}
