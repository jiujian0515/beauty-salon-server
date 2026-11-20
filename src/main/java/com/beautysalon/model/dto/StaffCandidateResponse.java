package com.beautysalon.model.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 技师候选用户返回体
 *
 * <p>只包含当前商户下「非管理员、已启用、未被其他技师资源绑定」的用户，
 * 供资源管理页面的技师下拉框使用。</p>
 */
@Data
@Builder
public class StaffCandidateResponse {

    /** 用户 id */
    private Long id;

    /** 姓名/昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;
}
