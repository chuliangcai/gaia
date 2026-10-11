package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.base.activity.ActivityRecord;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.infra.persistence.mapper.ActivityRecordMapper;
import com.gaia.infra.persistence.po.ActivityRecordPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动明细 Repository 实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepository {

    private final ActivityRecordMapper mapper;

    @Override
    public boolean insertIfAbsent(ActivityRecord record) {
        if (record.getCreatedAt() == null) {
            record.setCreatedAt(LocalDateTime.now());
        }
        ActivityRecordPO po = toPO(record);
        try {
            int rows = mapper.insert(po);
            record.setId(po.getId());
            return rows > 0;
        } catch (DuplicateKeyException dup) {
            log.info("activity record duplicate actionCode={}", record.getActionCode());
            return false;
        }
    }

    @Override
    public Optional<ActivityRecord> findByActionCode(String actionCode) {
        QueryWrapper<ActivityRecordPO> qw = new QueryWrapper<>();
        qw.eq("action_code", actionCode);
        return Optional.ofNullable(mapper.selectOne(qw)).map(ActivityRecordRepositoryImpl::toEntity);
    }

    @Override
    public List<ActivityRecord> findByUserAndActivity(Long userId, Long activityId) {
        QueryWrapper<ActivityRecordPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("activity_id", activityId)
                .orderByDesc("created_at");
        return mapper.selectList(qw).stream()
                .map(ActivityRecordRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActivityRecord> findByActivityAndType(Long activityId, String actionType) {
        QueryWrapper<ActivityRecordPO> qw = new QueryWrapper<>();
        qw.eq("activity_id", activityId).eq("action_type", actionType)
                .orderByAsc("id");
        return mapper.selectList(qw).stream()
                .map(ActivityRecordRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserTask(Long userId, Long activityId, Long taskId) {
        QueryWrapper<ActivityRecordPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .eq("activity_id", activityId)
                .eq("biz_id", taskId)
                .eq("action_type", "TASK_EXECUTED");
        return mapper.selectCount(qw);
    }

    @Override
    public long countByUserTaskOnDay(Long userId, Long activityId, Long taskId,
                                     LocalDateTime dayStart, LocalDateTime dayEnd) {
        QueryWrapper<ActivityRecordPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .eq("activity_id", activityId)
                .eq("biz_id", taskId)
                .eq("action_type", "TASK_EXECUTED")
                .ge("created_at", dayStart)
                .lt("created_at", dayEnd);
        return mapper.selectCount(qw);
    }

    // ----------------------- PO ↔ Domain -----------------------

    static ActivityRecord toEntity(ActivityRecordPO po) {
        if (po == null) {
            return null;
        }
        return ActivityRecord.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .activityId(po.getActivityId())
                .actionType(po.getActionType())
                .actionCode(po.getActionCode())
                .bizId(po.getBizId())
                .payload(po.getPayload())
                .result(po.getResult())
                .createdAt(po.getCreatedAt())
                .build();
    }

    static ActivityRecordPO toPO(ActivityRecord e) {
        if (e == null) {
            return null;
        }
        ActivityRecordPO po = new ActivityRecordPO();
        po.setId(e.getId());
        po.setUserId(e.getUserId());
        po.setActivityId(e.getActivityId());
        po.setActionType(e.getActionType());
        po.setActionCode(e.getActionCode());
        po.setBizId(e.getBizId());
        po.setPayload(e.getPayload());
        po.setResult(e.getResult());
        po.setCreatedAt(e.getCreatedAt());
        return po;
    }
}