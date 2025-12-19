package com.zsh.controller;

import com.zsh.dao.ProductDao;
import com.zsh.dao.SeckillActivityDao;
import com.zsh.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {
    private final ProductDao productDao;
    private final SeckillActivityDao activityDao;

    @GetMapping("/data")
    public Result<?> testData() {
        Long productCount = productDao.selectCount(null);
        Long activityCount = activityDao.selectCount(null);

        return Result.success("数据初始化成功，商品数：" + productCount + "，活动数：" + activityCount);
    }
}
