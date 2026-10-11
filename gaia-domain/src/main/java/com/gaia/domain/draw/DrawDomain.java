package com.gaia.domain.draw;

import com.gaia.iframework.annotation.DomainNs;
import org.springframework.stereotype.Component;

/**
 * 开奖领域标记。
 *
 * <p>与 {@code @Ability(bizNs = "draw")} 的能力（{@code collect_draw}）归属
 * 同一域。{@code @Component} 让 Spring 在启动期通过
 * {@code getBeansWithAnnotation(DomainNs.class)} 扫到它，进而写入
 * {@code DomainMetaRegistry}。</p>
 */
@Component
@DomainNs(code = "draw", name = "开奖域")
public class DrawDomain {
}
