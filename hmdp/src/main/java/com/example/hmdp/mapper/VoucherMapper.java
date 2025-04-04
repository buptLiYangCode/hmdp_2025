package com.example.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.hmdp.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    /**
     * 使用乐观锁方式减库存
     * @param voucherId 商品ID
     * @return 影响的行数
     */
    @Update("UPDATE tb_voucher SET stock = stock - 1 WHERE id = #{voucherId} AND stock > 0")
    int decreaseStock(Long voucherId);
} 