package com.gaia.backend.controller;

import com.gaia.api.ability.AbilityDescriptor;
import com.gaia.api.ability.AbilityMetaService;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 能力元数据后台接口。
 *
 * <p>由后台 admin 拉取"有哪些玩法、玩法下有哪些能力、能力的配置 schema 长啥样"，
 * 用于活动创建/编辑页面的动态表单渲染。底层调用 gaia-api 的
 * {@link AbilityMetaService}（由 gaia-domain 的 {@code AbilityMetaServiceImpl} 实现）。</p>
 */
@RestController
@RequestMapping("/api/admin/meta/abilities")
@RequiredArgsConstructor
@Tag(name = "能力元数据", description = "玩法 → 能力 → 配置 schema 动态查询")
public class AbilityMetaController {

    private final AbilityMetaService abilityMetaService;

    @GetMapping
    @Operation(summary = "按玩法过滤拉取能力元数据；不传 bizNs = 全量")
    public Result<List<AbilityDescriptor>> list(@RequestParam(required = false) String bizNs) {
        return Result.success(abilityMetaService.listAbilities(bizNs));
    }

    @GetMapping("/triggerable")
    @Operation(summary = "列出所有可由后台触发的管理端调用的能力（@Ability.backendTriggerable=true）")
    public Result<List<AbilityDescriptor>> listTriggerable() {
        List<AbilityDescriptor> all = abilityMetaService.listAbilities();
        List<AbilityDescriptor> triggerable = all.stream()
                .filter(d -> d != null && d.isBackendTriggerable())
                .toList();
        return Result.success(triggerable);
    }

    @GetMapping("/{code}")
    @Operation(summary = "按 code 取单条能力元数据")
    public Result<AbilityDescriptor> get(@PathVariable String code) {
        return abilityMetaService.findAbility(code)
                .map(Result::success)
                .orElse(Result.fail(Result.CodeEnum.NOT_FOUND));
    }
}
