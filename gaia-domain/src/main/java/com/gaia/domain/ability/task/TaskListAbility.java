package com.gaia.domain.ability.task;

import com.gaia.domain.ability.task.config.Task;
import com.gaia.domain.ability.task.config.TaskConfig;
import com.gaia.domain.ability.task.dto.TaskDTO;
import com.gaia.domain.base.activity.Activity;
import com.gaia.domain.repository.ActivityRecordRepository;
import com.gaia.domain.repository.ActivityRepository;
import com.gaia.iframework.annotation.Ability;
import com.gaia.iframework.core.AbstractBaseAbility;
import com.gaia.iframework.core.IFResult;
import com.gaia.iframework.core.RequestContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务清单 Ability。
 *
 * <p>从活动的 {@link Activity#getConfigurations()} 反序列化出
 * {@link TaskConfig}，输出任务清单及其当前用户执行进度。该能力与
 * {@link TaskExecuteAbility} 平级、互不重叠，二者共享同一份 {@code TaskConfig}。</p>
 */
@Slf4j
@Component
@Ability(code = "task_list", bizNs = "task", name = "任务清单", configClass = TaskConfig.class)
@RequiredArgsConstructor
public class TaskListAbility extends AbstractBaseAbility {

    private static final String CONFIG_KEY = TaskConfig.class.getName();

    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    public IFResult<List<TaskDTO>> execute(RequestContext context) {
        Long userId = context.getUserId();
        var ext = context.getExtParams();
        String activityCode = ext == null ? null : ext.get("activityCode");
        if (activityCode == null || activityCode.isBlank()) {
            return IFResult.fail("ACTIVITY_CODE_REQUIRED");
        }
        Optional<Activity> opt = activityRepository.findByCode(activityCode);
        if (opt.isEmpty()) {
            return IFResult.fail("ACTIVITY_NOT_FOUND");
        }
        Activity activity = opt.get();
        List<Task> tasks = loadConfigTasks(activity);
        if (tasks == null || tasks.isEmpty()) {
            return IFResult.success(Collections.emptyList());
        }

        List<TaskDTO> out = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            long executed = 0;
            if (userId != null && userId > 0) {
                executed = activityRecordRepository.countByUserTask(
                        userId, activity.getId(), t.getId());
            }
            out.add(TaskDTO.builder()
                    .id(t.getId())
                    .taskCode(t.getTaskCode())
                    .taskName(t.getTaskName())
                    .taskDesc(t.getTaskDesc())
                    .rewardCodes(t.getRewardCodes())
                    .maxExecutions(t.getMaxExecutions())
                    .executedCount((int) executed)
                    .available(t.isAvailableFor((int) executed))
                    .sortOrder(t.getSortOrder())
                    .build());
        }
        return IFResult.success(out);
    }

    @Override
    public List<String> getProcessor(String bizCode) {
        return List.of();
    }

    /**
     * 从 {@link Activity#getConfigurations()} 反序列化 {@link TaskConfig}。
     * 配置缺失或解析失败时返回空列表（视为"该活动未启用任何任务"，不报错）。
     */
    List<Task> loadConfigTasks(Activity activity) {
        String raw = activity.getConfigurations();
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> root = objectMapper.readValue(
                    raw, new TypeReference<Map<String, Object>>() {});
            Object payload = root.get(CONFIG_KEY);
            if (payload == null) {
                return Collections.emptyList();
            }
            TaskConfig cfg = objectMapper.convertValue(payload, TaskConfig.class);
            return cfg.getTasks() == null ? Collections.emptyList() : cfg.getTasks();
        } catch (Exception e) {
            log.warn("反序列化 TaskConfig 失败 activityId={}", activity.getId(), e);
            return Collections.emptyList();
        }
    }
}
