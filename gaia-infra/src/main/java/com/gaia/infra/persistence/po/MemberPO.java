package com.gaia.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员 PO。
 */
@Data
@TableName("gaia_member")
public class MemberPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String level;

    /** 0=失效,1=有效 */
    private Integer status;

    private LocalDateTime joinAt;
    private LocalDateTime expireAt;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}