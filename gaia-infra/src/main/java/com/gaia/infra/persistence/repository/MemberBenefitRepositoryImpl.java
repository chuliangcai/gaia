package com.gaia.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaia.domain.draw.DrawCandidate;
import com.gaia.domain.member.MemberBenefit;
import com.gaia.domain.repository.MemberBenefitRepository;
import com.gaia.infra.persistence.mapper.MemberBenefitMapper;
import com.gaia.infra.persistence.mapper.MemberBenefitMapper.WeightRow;
import com.gaia.infra.persistence.po.MemberBenefitPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员权益 Repository 实现（含夺宝码账本）。
 *
 * <p>夺宝码活动内唯一由 DB 生成列 {@code uk_lottery_code} 的 UK 保证；
 * 业务层负责捕获 {@link DuplicateKeyException} 并重试。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberBenefitRepositoryImpl implements MemberBenefitRepository {

    private final MemberBenefitMapper mapper;

    @Override
    public int insertBenefits(List<MemberBenefit> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        for (MemberBenefit b : benefits) {
            if (b.getGrantedAt() == null) {
                b.setGrantedAt(LocalDateTime.now());
            }
            if (b.getStatus() == null) {
                b.setStatus(1);
            }
            MemberBenefitPO po = toPO(b);
            try {
                mapper.insert(po);
                b.setId(po.getId());
                inserted++;
            } catch (DuplicateKeyException dup) {
                // DB 唯一约束冲突（如 LOTTERY_CODE 在同活动内重复），Repository 不吞
                log.info("member benefit duplicate type={} value={} activityId={}",
                        b.getBenefitType(), b.getBenefitValue(), b.getActivityId());
                throw dup;
            }
        }
        return inserted;
    }

    @Override
    public List<String> listLotteryCodesByUserAndActivity(Long userId, Long activityId) {
        QueryWrapper<MemberBenefitPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .eq("activity_id", activityId)
                .eq("benefit_type", "LOTTERY_CODE")
                .eq("status", 1)
                .orderByAsc("id");
        return mapper.selectList(qw).stream()
                .map(MemberBenefitPO::getBenefitValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<DrawCandidate> countLotteryCodesByActivity(Long activityId) {
        List<WeightRow> rows = mapper.countLotteryCodesByActivity(activityId);
        List<DrawCandidate> out = new ArrayList<>(rows.size());
        for (WeightRow row : rows) {
            DrawCandidate c = new DrawCandidate();
            c.setUserId(row.getUserId());
            c.setWeight(row.getWeight() == null ? 0 : row.getWeight().intValue());
            out.add(c);
        }
        return out;
    }

    @Override
    public List<MemberBenefit> listByUserId(Long userId) {
        QueryWrapper<MemberBenefitPO> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("status", 1).orderByDesc("id");
        return mapper.selectList(qw).stream()
                .map(MemberBenefitRepositoryImpl::toEntity)
                .collect(Collectors.toList());
    }

    // ----------------------- PO ↔ Domain -----------------------

    static MemberBenefit toEntity(MemberBenefitPO po) {
        if (po == null) {
            return null;
        }
        return MemberBenefit.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .benefitType(po.getBenefitType())
                .benefitValue(po.getBenefitValue())
                .activityId(po.getActivityId())
                .effectiveActivityId(po.getEffectiveActivityId())
                .sourceId(po.getSourceId())
                .status(po.getStatus())
                .grantedAt(po.getGrantedAt())
                .expireAt(po.getExpireAt())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    static MemberBenefitPO toPO(MemberBenefit e) {
        if (e == null) {
            return null;
        }
        MemberBenefitPO po = new MemberBenefitPO();
        po.setId(e.getId());
        po.setUserId(e.getUserId());
        po.setBenefitType(e.getBenefitType());
        po.setBenefitValue(e.getBenefitValue());
        po.setActivityId(e.getActivityId());
        po.setEffectiveActivityId(e.getEffectiveActivityId());
        po.setSourceId(e.getSourceId());
        po.setStatus(e.getStatus());
        po.setGrantedAt(e.getGrantedAt());
        po.setExpireAt(e.getExpireAt());
        po.setCreateTime(e.getCreateTime());
        po.setUpdateTime(e.getUpdateTime());
        return po;
    }
}