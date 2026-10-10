package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.activity.ActivityTask;
import com.gaia.domain.repository.ActivityTaskRepository;
import com.gaia.infra.persistence.mapper.ActivityTaskMapper;
import com.gaia.infra.persistence.po.ActivityTaskPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动任务 Repository 实现。
 */
@Repository
@RequiredArgsConstructor
public class ActivityTaskRepositoryImpl implements ActivityTaskRepository {

    private final ActivityTaskMapper mapper;

    @Override
    public Optional<ActivityTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(ActivityTaskRepositoryImpl::toEntity);
    }

    @Override
    public List<ActivityTask> findByActivityId(Long activityId) {
        QueryWrapper<ActivityTaskPO> qw = new QueryWrapper<>();
        qw.eq("activity_id", activityId).orderByAsc("sort_order");
        return mapper.selectList(qw).stream()
                .map(ActivityTaskRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int replaceByActivityId(Long activityId, List<ActivityTask> tasks) {
        // 删除旧配置
        QueryWrapper<ActivityTaskPO> del = new QueryWrapper<>();
        del.eq("activity_id", activityId);
        mapper.delete(del);
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        for (ActivityTask t : tasks) {
            t.setId(null);
            t.setActivityId(activityId);
            ActivityTaskPO po = toPO(t);
            mapper.insert(po);
            t.setId(po.getId());
        }
        return tasks.size();
    }

    // ----------------------- PO ↔ Domain -----------------------

    static ActivityTask toEntity(ActivityTaskPO po) {
        if (po == null) {
            return null;
        }
        return ActivityTask.builder()
                .id(po.getId())
                .activityId(po.getActivityId())
                .taskCode(po.getTaskCode())
                .taskName(po.getTaskName())
                .taskDesc(po.getTaskDesc())
                .rewardCodes(po.getRewardCodes())
                .maxExecutions(po.getMaxExecutions())
                .sortOrder(po.getSortOrder())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    static ActivityTaskPO toPO(ActivityTask e) {
        if (e == null) {
            return null;
        }
        ActivityTaskPO po = new ActivityTaskPO();
        po.setId(e.getId());
        po.setActivityId(e.getActivityId());
        po.setTaskCode(e.getTaskCode());
        po.setTaskName(e.getTaskName());
        po.setTaskDesc(e.getTaskDesc());
        po.setRewardCodes(e.getRewardCodes());
        po.setMaxExecutions(e.getMaxExecutions());
        po.setSortOrder(e.getSortOrder());
        po.setStatus(e.getStatus());
        po.setCreateTime(e.getCreateTime());
        po.setUpdateTime(e.getUpdateTime());
        return po;
    }
}