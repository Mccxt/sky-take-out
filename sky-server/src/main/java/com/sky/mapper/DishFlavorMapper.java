package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 批量删除菜品关联的口味
     * @param DishIds
     */
    void deleteFlavorByIds(List<Long> DishIds);

    /**
     * 根据菜品id查询对应的口味
     * @param id
     */
    @Select("select * from dish_flavor where dish_id=#{id}")
    List<DishFlavor> getById(Long id);
}
