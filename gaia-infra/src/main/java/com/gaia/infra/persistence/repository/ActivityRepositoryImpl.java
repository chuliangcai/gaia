package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.infra.persistence.mapper.ActivityMapper;
import com.gaia.infra.persistence.po.ActivityPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动 Repository 实现。
 */
@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepository {

    private final ActivityMapper mapper;

    @Override
    public Optional<Activity> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(ActivityRepositoryImpl::toEntity);
    }

    @Override
    public Optional<Activity> findByCode(String activityCode) {
        QueryWrapper<ActivityPO> qw = new QueryWrapper<>();
        qw.eq("activity_code", activityCode);
        return Optional.ofNullable(mapper.selectOne(qw)).map(ActivityRepositoryImpl::toEntity);
    }

    @Override
    public List<Activity> findStartingAt(LocalDateTime now) {
        QueryWrapper<ActivityPO> qw = new QueryWrapper<>();
        qw.eq("status", 0).le("start_time", now);
        return mapper.selectList(qw).stream()
                .map(ActivityRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Activity> findEndingAt(LocalDateTime now) {
        QueryWrapper<ActivityPO> qw = new QueryWrapper<>();
        qw.eq("status", 1).le("end_time", now);
        return mapper.selectList(qw).stream()
                .map(ActivityRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Activity> findDrawn() {
        QueryWrapper<ActivityPO> qw = new QueryWrapper<>();
        qw.eq("status", 3);
        return mapper.selectList(qw).stream()
                .map(ActivityRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Activity save(Activity activity) {
        ActivityPO po = toPO(activity);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            // 乐观锁：依赖 MyBatis-Plus @Version 自动 +1
            mapper.updateById(po);
        }
        activity.setId(po.getId());
        activity.setVersion(po.getVersion());
        return activity;
    }

    @Override
    public List<Activity> findAll() {
        QueryWrapper<ActivityPO> qw = new QueryWrapper<>();
        qw.orderByDesc("id");
        return mapper.selectList(qw).stream()
                .map(ActivityRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        mapper.deleteById(id);
    }

    // ----------------------- PO ↔ Domain -----------------------

    static Activity toEntity(ActivityPO po) {
        if (po == null) {
            return null;
        }
        return Activity.builder()
                .id(po.getId())
                .activityCode(po.getActivityCode())
                .bizCode(po.getBizCode())
                .name(po.getName())
                .prizeDesc(po.getPrizeDesc())
                .prizeImgUrl(po.getPrizeImgUrl())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .status(po.getStatus())
                .winnerCount(po.getWinnerCount())
                .drawAt(po.getDrawAt())
                .version(po.getVersion())
                .configurations(po.getConfigurations())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    static ActivityPO toPO(Activity e) {
        if (e == null) {
            return null;
        }
        ActivityPO po = new ActivityPO();
        po.setId(e.getId());
        po.setActivityCode(e.getActivityCode());
        po.setBizCode(e.getBizCode());
        po.setName(e.getName());
        po.setPrizeDesc(e.getPrizeDesc());
        po.setPrizeImgUrl(e.getPrizeImgUrl());
        po.setStartTime(e.getStartTime());
        po.setEndTime(e.getEndTime());
        po.setStatus(e.getStatus());
        po.setWinnerCount(e.getWinnerCount());
        po.setDrawAt(e.getDrawAt());
        po.setVersion(e.getVersion());
        po.setConfigurations(e.getConfigurations());
        po.setCreateTime(e.getCreateTime());
        po.setUpdateTime(e.getUpdateTime());
        return po;
    }
}