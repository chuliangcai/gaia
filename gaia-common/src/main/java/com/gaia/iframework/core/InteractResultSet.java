package com.gaia.iframework.core;

import java.io.Serializable;
import java.util.Map;

/**
 * RPC 响应：所有能力执行结果的聚合。
 *
 * <p>{@link #items} 的 key 为 {@code ModelRequest.assertType}（即能力 code），
 * value 为该能力的执行结果。{@link #success} 表示全部成功（任一失败即为 {@code false}）。</p>
 */
public class InteractResultSet implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String msgCode;
    private Map<String, IFResult<?>> items;

    public InteractResultSet() {
    }

    public InteractResultSet(boolean success, String msgCode, Map<String, IFResult<?>> items) {
        this.success = success;
        this.msgCode = msgCode;
        this.items = items;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public Map<String, IFResult<?>> getItems() {
        return items;
    }

    public void setItems(Map<String, IFResult<?>> items) {
        this.items = items;
    }
}
