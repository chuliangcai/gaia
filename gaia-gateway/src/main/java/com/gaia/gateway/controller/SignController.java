package com.gaia.gateway.controller;

import com.gaia.api.sign.SignService;
import com.gaia.common.constants.DubboConstants;
import com.gaia.common.dto.Result;
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
    public Result<SignService.SignResultDTO> sign(@RequestParam("userId") Long userId) {
        log.info("签到请求 userId={}", userId);
        SignService.SignResultDTO dto = signService.sign(userId);
        return Result.success(dto);
    }

    /**
     * 当月签到日历。
     */
    @GetMapping("/calendar")
    public Result<SignService.SignCalendarDTO> calendar(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "yearMonth", required = false) String yearMonth) {
        SignService.SignCalendarDTO dto = signService.calendar(userId, yearMonth);
        return Result.success(dto);
    }

    /**
     * 连续签到天数。
     */
    @GetMapping("/streak")
    public Result<Integer> streak(@RequestParam("userId") Long userId) {
        return Result.success(signService.streak(userId));
    }
}