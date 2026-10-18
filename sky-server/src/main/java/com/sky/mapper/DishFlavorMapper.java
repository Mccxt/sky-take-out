package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertFlavor(List<DishFlavor> flavors);

    /**
     * 根据菜品id删除对应的口味
     * @param DishId
     */
    @Delete("delete from dish_flavor where dish_flavor.dish_id=#{DishId}")
    void deleteFlavorById(Long DishId);
}
