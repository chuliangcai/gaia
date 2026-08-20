package com.gaia.gateway.controller;

import com.gaia.api.sign.SignService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 签到 HTTP 入口。
 *
 * <p>只做参数透传与结果包装，不做任何业务计算。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/sign")
@Tag(name = "签到服务", description = "用户每日签到、日历查询、连续天数")
public class SignController {

    @DubboReference(
            group = DubboConstants.GROUP,
            version = DubboConstants.VERSION,
            url = "${gaia.dubbo.sign.url}")
    private SignService signService;

    /**
     * 用户签到。
     */
    @PostMapping("/do")
    @Operation(summary = "用户签到", description = "为指定用户执行签到，幂等（当日重复签到返回失败）")
    public Result<SignService.SignResultDTO> sign(
            @Parameter(description = "用户 ID", required = true, example = "1001")
            @RequestParam("userId") Long userId) {
        log.info("签到请求 userId={}", userId);
        SignService.SignResultDTO dto = signService.sign(userId);
        return Result.success(dto);
    }

    /**
     * 当月签到日历。
     */
    @GetMapping("/calendar")
    @Operation(summary = "当月签到日历", description = "返回指定月份已签到的日期集合")
    public Result<SignService.SignCalendarDTO> calendar(
            @Parameter(description = "用户 ID", required = true, example = "1001")
            @RequestParam("userId") Long userId,
            @Parameter(description = "年月 yyyyMM，缺省取当月", example = "202608")
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {
        SignService.SignCalendarDTO dto = signService.calendar(userId, yearMonth);
        return Result.success(dto);
    }

    /**
     * 连续签到天数。
     */
    @GetMapping("/streak")
    @Operation(summary = "连续签到天数", description = "查询用户当前连续签到天数")
    public Result<Integer> streak(
            @Parameter(description = "用户 ID", required = true, example = "1001")
            @RequestParam("userId") Long userId) {
        return Result.success(signService.streak(userId));
    }
}
