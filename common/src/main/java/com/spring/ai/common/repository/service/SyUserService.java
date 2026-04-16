package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyUser;
import java.util.List;

/**
 * 用户仓储服务接口
 */
public interface SyUserService extends IService<SyUser> {

    /**
     * 按用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     */
    SyUser getByUsername(String username);

    /**
     * 按手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体
     */
    SyUser getByPhone(String phone);

    /**
     * 按邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户实体
     */
    SyUser getByEmail(String email);

    /**
     * 查询用户详情
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    SyUser getDetailById(Long userId);

    /**
     * 查询全部用户
     *
     * @return 用户列表
     */
    List<SyUser> listAllUsers();

    /**
     * 按条件分页查询用户列表
     *
     * @param username 用户名
     * @param nickname 昵称
     * @param phone    手机号
     * @param email    邮箱
     * @param status   状态
     * @return 用户列表
     */
    List<SyUser> pageQueryUsers(String username, String nickname, String phone, String email, Integer status);

    /**
     * 统计指定租户下的用户数量。
     *
     * @param tenantId 租户 ID
     * @return 用户数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 统计系统用户总数
     *
     * @return 用户总数
     */
    long countAllUsers();

    /**
     * 按主键删除用户
     * <p>

 * 此方法用于根据用户ID删除对应的用户记录。
 * 这是一个接口方法，具体实现需要由子类或实现类完成。
 *
     * @param userId 用户 ID，用于唯一标识要删除的用户记录。不能为null。
     * @return 是否成功
     */
    boolean deleteByUserId(Long userId);
}
