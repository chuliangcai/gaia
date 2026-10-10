package com.gaia.server.treasure.scheduler;

import com.gaia.domain.activity.Activity;
import com.gaia.server.treasure.service.ActivityStatusRefresher;
import com.gaia.server.treasure.service.DrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 夺宝开奖定时调度器。
 *
 * <p>每分钟执行：</p>
 * <ol>
 *   <li>推进活动状态：0→1、1→2</li>
 *   <li>对状态=2 的活动调用 {@link DrawService#draw(Long)}</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TreasureDrawScheduler {

    private final ActivityStatusRefresher refresher;
    private final DrawService drawService;

    @Scheduled(fixedDelay = 60_000L)
    public void tick() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        try {
            List<Activity> awaitingDraw = refresher.refresh(now);
            if (awaitingDraw.isEmpty()) {
                return;
            }
            log.info("夺宝定时调度，待开奖活动数={}", awaitingDraw.size());
            for (Activity a : awaitingDraw) {
                try {
                    drawService.draw(a.getId());
                } catch (Exception e) {
                    log.error("夺宝定时开奖失败 activityId={}", a.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("夺宝定时调度异常", e);
        }
    }
}