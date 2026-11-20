package com.beautysalon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beautysalon.common.BusinessException;
import com.beautysalon.common.ErrorCode;
import com.beautysalon.common.LoginContext;
import com.beautysalon.common.PageResult;
import com.beautysalon.mapper.ResourceMapper;
import com.beautysalon.mapper.UserMapper;
import com.beautysalon.model.dto.ResourceCreateRequest;
import com.beautysalon.model.dto.ResourceResponse;
import com.beautysalon.model.dto.ResourceUpdateRequest;
import com.beautysalon.model.dto.StaffCandidateResponse;
import com.beautysalon.model.entity.Resource;
import com.beautysalon.model.entity.User;
import com.beautysalon.service.ResourceService;
import com.beautysalon.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资源管理实现
 *
 * <p>核心规则：</p>
 * <ol>
 *   <li>数据隔离：所有操作只针对当前登录用户所属商户。</li>
 *   <li>STAFF 技师必须绑定一个「非管理员」用户，名称未传时默认取用户昵称。</li>
 *   <li>ROOM 床位不绑定用户，名称必填。</li>
 *   <li>一个用户最多绑定一个技师资源。</li>
 *   <li>资源被排班/库存/预约引用后不允许删除。</li>
 * </ol>
 */
@Service
public class ResourceServiceImpl implements ResourceService {

    private static final String TYPE_STAFF = "STAFF";
    private static final String TYPE_ROOM = "ROOM";
    private static final String ROLE_ADMIN = "ADMIN";

    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final LoginContext loginContext;

    public ResourceServiceImpl(ResourceMapper resourceMapper,
                               UserMapper userMapper,
                               UserRoleService userRoleService,
                               LoginContext loginContext) {
        this.resourceMapper = resourceMapper;
        this.userMapper = userMapper;
        this.userRoleService = userRoleService;
        this.loginContext = loginContext;
    }

    @Override
    public PageResult<ResourceResponse> page(int page, int size, String keyword, String type, Integer status) {
        long merchantId = loginContext.merchantId();

        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getMerchantId, merchantId);

        if (StringUtils.hasText(type)) {
            wrapper.eq(Resource::getType, type.trim().toUpperCase(Locale.ROOT));
        }
        if (status != null) {
            wrapper.eq(Resource::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Resource::getName, keyword.trim());
        }

        wrapper.orderByDesc(Resource::getId);

        Page<Resource> result = resourceMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, User> userMap = loadUsers(result.getRecords());

        List<ResourceResponse> records = result.getRecords().stream()
                .map(resource -> toResponse(resource, userMap.get(resource.getUserId())))
                .toList();

        return PageResult.of(result, records);
    }

    @Override
    public ResourceResponse detail(Long id) {
        Resource resource = findResource(id);
        return toResponse(resource, loadUser(resource.getUserId()));
    }

    @Override
    public ResourceResponse create(ResourceCreateRequest request) {
        long merchantId = loginContext.merchantId();
        String type = normalizeAndValidateType(request.getType());

        Resource resource = new Resource();
        resource.setMerchantId(merchantId);
        resource.setType(type);
        resource.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        User boundUser = null;
        if (TYPE_STAFF.equals(type)) {
            if (request.getUserId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "技师资源必须选择绑定用户");
            }
            boundUser = validateStaffUser(request.getUserId(), merchantId, null);
            resource.setUserId(boundUser.getId());
            resource.setName(StringUtils.hasText(request.getName())
                    ? request.getName().trim()
                    : defaultName(boundUser));
        } else {
            if (request.getUserId() != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "床位资源不能绑定用户");
            }
            if (!StringUtils.hasText(request.getName())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "床位名称不能为空");
            }
            resource.setName(request.getName().trim());
        }

        resourceMapper.insert(resource);
        // 重新查一次，把数据库默认值（created_at / updated_at）带给前端
        return toResponse(resourceMapper.selectById(resource.getId()), boundUser);
    }

    @Override
    public ResourceResponse update(Long id, ResourceUpdateRequest request) {
        Resource resource = findResource(id);

        if (StringUtils.hasText(request.getType())) {
            String requestedType = normalizeAndValidateType(request.getType());
            if (!requestedType.equals(resource.getType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型创建后不允许修改");
            }
        }

        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
        }

        User boundUser = null;
        if (TYPE_STAFF.equals(resource.getType())) {
            if (request.getUserId() != null) {
                boundUser = validateStaffUser(request.getUserId(), resource.getMerchantId(), resource.getId());
                boolean userChanged = !Objects.equals(boundUser.getId(), resource.getUserId());
                resource.setUserId(boundUser.getId());
                // 换绑用户且没有单独传名称时，名称跟随新用户昵称
                if (userChanged && !StringUtils.hasText(request.getName())) {
                    resource.setName(defaultName(boundUser));
                }
            } else {
                if (resource.getUserId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "技师资源必须绑定用户");
                }
                boundUser = loadUser(resource.getUserId());
            }

            if (StringUtils.hasText(request.getName())) {
                resource.setName(request.getName().trim());
            }
        } else {
            if (request.getUserId() != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "床位资源不能绑定用户");
            }
            if (StringUtils.hasText(request.getName())) {
                resource.setName(request.getName().trim());
            }
        }

        resourceMapper.updateById(resource);
        // 重新查一次，拿到数据库 ON UPDATE 刷新后的 updated_at
        return toResponse(resourceMapper.selectById(resource.getId()), boundUser);
    }

    @Override
    public void delete(Long id) {
        findResource(id);

        if (resourceMapper.countReferences(id) > 0) {
            throw new BusinessException(ErrorCode.IN_USE, "该资源已被排班/库存/预约使用，无法删除");
        }

        resourceMapper.deleteById(id);
    }

    @Override
    public List<StaffCandidateResponse> staffCandidates(Long resourceId) {
        long merchantId = loginContext.merchantId();

        if (resourceId != null) {
            // 编辑场景：先确认资源属于当前商户，避免越权探测
            findResource(resourceId);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getMerchantId, merchantId)
                .eq(User::getStatus, 1)
                // 排除管理员：{0} 是预编译参数，不存在 SQL 注入风险
                .notExists("SELECT 1 FROM user_role ur JOIN role r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = user.id AND r.code = {0}", ROLE_ADMIN);

        // 排除已绑定其他技师资源的用户；编辑当前资源时保留其已绑定用户
        if (resourceId == null) {
            wrapper.notExists("SELECT 1 FROM resource res WHERE res.user_id = user.id");
        } else {
            wrapper.notExists("SELECT 1 FROM resource res WHERE res.user_id = user.id AND res.id <> {0}",
                    resourceId);
        }

        wrapper.orderByDesc(User::getId);

        return userMapper.selectList(wrapper).stream()
                .map(user -> StaffCandidateResponse.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .phone(user.getPhone())
                        .build())
                .toList();
    }

    /** 按 id 查资源，并校验属于当前商户 */
    private Resource findResource(Long id) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null
                || !Objects.equals(resource.getMerchantId(), loginContext.merchantId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    /**
     * 校验技师绑定用户：必须存在、同商户、已启用、非管理员、未绑定其他资源
     *
     * @param currentResourceId 编辑当前资源时传其 id，避免「自己绑定自己」被误判为重复
     */
    private User validateStaffUser(Long userId, long merchantId, Long currentResourceId) {
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getMerchantId(), merchantId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所选用户不存在或不属于当前商户");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所选用户已被禁用，不能作为技师");
        }
        if (userRoleService.hasRole(userId, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "管理员账号不能作为技师");
        }

        LambdaQueryWrapper<Resource> boundWrapper = new LambdaQueryWrapper<Resource>()
                .eq(Resource::getUserId, userId);
        if (currentResourceId != null) {
            boundWrapper.ne(Resource::getId, currentResourceId);
        }
        Long boundCount = resourceMapper.selectCount(boundWrapper);
        if (boundCount != null && boundCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_DATA, "该用户已绑定其他技师资源");
        }
        return user;
    }

    /** 名称默认值：优先昵称，昵称为空时用手机号 */
    private String defaultName(User user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getPhone();
    }

    /** 类型统一转大写并校验 */
    private String normalizeAndValidateType(String rawType) {
        String type = rawType.trim().toUpperCase(Locale.ROOT);
        if (!TYPE_STAFF.equals(type) && !TYPE_ROOM.equals(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "资源类型只能是 STAFF 或 ROOM");
        }
        return type;
    }

    private User loadUser(Long userId) {
        return userId == null ? null : userMapper.selectById(userId);
    }

    /** 批量查绑定用户，避免列表接口 N+1 查询 */
    private Map<Long, User> loadUsers(List<Resource> resources) {
        List<Long> userIds = resources.stream()
                .map(Resource::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private ResourceResponse toResponse(Resource resource, User user) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .merchantId(resource.getMerchantId())
                .type(resource.getType())
                .userId(resource.getUserId())
                .userName(user == null ? null : user.getNickname())
                .userPhone(user == null ? null : user.getPhone())
                .name(resource.getName())
                .status(resource.getStatus())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
