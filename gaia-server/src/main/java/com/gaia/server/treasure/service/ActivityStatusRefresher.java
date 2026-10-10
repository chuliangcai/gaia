package com.gaia.server.treasure.service;

import com.gaia.domain.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动状态推进器。
 *
 * <p>被定时调度与手工触发共用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityStatusRefresher {

    private final ActivityRepository activityRepository;

    /**
     * 推进状态：0→1、1→2。
     *
     * @return 推进后的"待开奖"活动列表（需要立即开奖）
     */
    public List<Activity> refresh(LocalDateTime now) {
        // 0 → 1
        List<Activity> starting = activityRepository.findStartingAt(now);
        for (Activity a : starting) {
            try {
                a.markOngoing();
                activityRepository.save(a);
                log.info("活动推进 0→1 activityId={} code={}", a.getId(), a.getActivityCode());
            } catch (Exception e) {
                log.warn("活动状态推进 0→1 失败 id={}", a.getId(), e);
            }
        }
        // 1 → 2
        List<Activity> ending = activityRepository.findEndingAt(now);
        for (Activity a : ending) {
            try {
                a.markWaitingDraw();
                activityRepository.save(a);
                log.info("活动推进 1→2 activityId={} code={}", a.getId(), a.getActivityCode());
            } catch (Exception e) {
                log.warn("活动状态推进 1→2 失败 id={}", a.getId(), e);
            }
        }
        return ending;
    }
}