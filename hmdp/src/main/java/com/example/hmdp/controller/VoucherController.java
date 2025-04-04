package com.example.hmdp.controller;

import com.example.hmdp.entity.Voucher;
import com.example.hmdp.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Autowired
    private IVoucherService voucherService;

    /**
     * 添加普通券
     * @param voucher 券信息
     * @return 券ID
     */
    @PostMapping
    public Long addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return voucher.getId();
    }

    /**
     * 添加秒杀券
     * @param voucher 券信息
     * @return 券ID
     */
    @PostMapping("/seckill")
    public Long addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return voucher.getId();
    }

    /**
     * 查询所有券
     * @return 券列表
     */
    @GetMapping("/list")
    public List<Voucher> listVouchers() {
        return voucherService.list();
    }
}