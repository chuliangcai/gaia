package com.gaia.domain.task;

import com.gaia.iframework.annotation.DomainNs;
import org.springframework.stereotype.Component;

/**
 * 任务领域标记。
 *
 * <p>与 {@code @Ability(bizNs = "task")} 的能力（{@code task_list} /
 * {@code task_execute}）归属同一域。{@code @Component} 让 Spring 在启动期
 * 通过 {@code getBeansWithAnnotation(DomainNs.class)} 扫到它，
 * 进而写入 {@code DomainMetaRegistry}。</p>
 */
@Component
@DomainNs(code = "task", name = "任务域")
public class TaskDomain {
}
