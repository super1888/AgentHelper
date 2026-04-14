package com.spring.ai.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.common.repository.enitiy.SyUser;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户仓储 Mapper。
 */
@Mapper
public interface SyUserMapper extends BaseMapper<SyUser> {

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    @Select("""
            select id, ext, remark, create_id, create_name, create_time, update_id, update_name, update_time, version,
                   username, nickname, phone, email, password_hash, status, tenant_id
              from sy_user
             where username = #{username}
             limit 1
            """)
    SyUser selectByUsername(@Param("username") String username);

    /**
     * 按手机号查询用户。
     *
     * @param phone 手机号
     * @return 用户实体
     */
    @Select("""
            select id, ext, remark, create_id, create_name, create_time, update_id, update_name, update_time, version,
                   username, nickname, phone, email, password_hash, status, tenant_id
              from sy_user
             where phone = #{phone}
             limit 1
            """)
    SyUser selectByPhone(@Param("phone") String phone);

    /**
     * 按邮箱查询用户。
     *
     * @param email 邮箱
     * @return 用户实体
     */
    @Select("""
            select id, ext, remark, create_id, create_name, create_time, update_id, update_name, update_time, version,
                   username, nickname, phone, email, password_hash, status, tenant_id
              from sy_user
             where email = #{email}
             limit 1
            """)
    SyUser selectByEmail(@Param("email") String email);

    /**
     * 按主键查询用户明细。
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    @Select("""
            select id, ext, remark, create_id, create_name, create_time, update_id, update_name, update_time, version,
                   username, nickname, phone, email, password_hash, status, tenant_id
              from sy_user
             where id = #{userId}
             limit 1
            """)
    SyUser selectDetailById(@Param("userId") Long userId);

    /**
     * 查询全部用户。
     *
     * @return 用户列表
     */
    @Select("""
            select id, ext, remark, create_id, create_name, create_time, update_id, update_name, update_time, version,
                   username, nickname, phone, email, password_hash, status, tenant_id
              from sy_user
             order by create_time desc, id desc
            """)
    List<SyUser> selectAllUsers();

    /**
     * 按主键删除用户。
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    @Delete("""
            delete
              from sy_user
             where id = #{userId}
            """)
    int deleteByUserId(@Param("userId") Long userId);
}
