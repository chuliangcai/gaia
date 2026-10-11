package com.gaia.domain.ability.task.config;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务能力配置（task_list / task_execute 共用）。
 *
 * <p>取代原先 {@code gaia_activity_task} 表中的每行配置，序列化为活动记录的
 * {@code configurations} JSON 字段，运行时由 {@link TaskListAbility} /
 * {@link TaskExecuteAbility} 反序列化读取。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("任务配置")
public class TaskConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务列表配置 */
    @JsonPropertyDescription("任务列表")
    private List<Task> tasks;
}
