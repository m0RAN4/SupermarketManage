package com.supermarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.supermarket.entity.ShelfCapacityTracking;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ShelfCapacityTrackingMapper extends BaseMapper<ShelfCapacityTracking> {

    /** 查询某货架某层的追踪记录 */
    @Select("SELECT * FROM shelf_capacity_tracking WHERE shelf_id = #{shelfId} AND shelf_layer = #{shelfLayer}")
    ShelfCapacityTracking selectByShelfAndLayer(@Param("shelfId") Long shelfId, @Param("shelfLayer") Integer shelfLayer);

    /** 查询某货架所有层的追踪记录 */
    @Select("SELECT * FROM shelf_capacity_tracking WHERE shelf_id = #{shelfId} ORDER BY shelf_layer")
    java.util.List<ShelfCapacityTracking> selectByShelfId(@Param("shelfId") Long shelfId);

    /** 增加某层已使用数量 */
    @Update("UPDATE shelf_capacity_tracking SET used_count = used_count + #{delta}, update_time = NOW() " +
            "WHERE shelf_id = #{shelfId} AND shelf_layer = #{shelfLayer}")
    int incrementUsed(@Param("shelfId") Long shelfId, @Param("shelfLayer") Integer shelfLayer, @Param("delta") Integer delta);

    /** 减少某层已使用数量 */
    @Update("UPDATE shelf_capacity_tracking SET used_count = IF(used_count >= #{delta}, used_count - #{delta}, 0), update_time = NOW() " +
            "WHERE shelf_id = #{shelfId} AND shelf_layer = #{shelfLayer}")
    int decrementUsed(@Param("shelfId") Long shelfId, @Param("shelfLayer") Integer shelfLayer, @Param("delta") Integer delta);

    /** 更新某层最大容量（货架信息变更时同步） */
    @Update("UPDATE shelf_capacity_tracking SET max_capacity = #{maxCapacity}, update_time = NOW() " +
            "WHERE shelf_id = #{shelfId} AND shelf_layer = #{shelfLayer}")
    int updateMaxCapacity(@Param("shelfId") Long shelfId, @Param("shelfLayer") Integer shelfLayer, @Param("maxCapacity") Integer maxCapacity);

    /** 查询所有货架总剩余容量 */
    @Select("SELECT COALESCE(SUM(max_capacity - used_count), 0) FROM shelf_capacity_tracking")
    Integer selectTotalRemaining();

    /** 查询某货架总剩余容量 */
    @Select("SELECT COALESCE(SUM(max_capacity - used_count), 0) FROM shelf_capacity_tracking WHERE shelf_id = #{shelfId}")
    Integer selectShelfRemaining(@Param("shelfId") Long shelfId);
}
