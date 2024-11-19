package com.wky.feishuservice.model.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author wky
 * @date 2024/10/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "location")
public class LocationDO implements Serializable {

    @TableId
    private String id;

    /**
     * 地区名，细化到区县
     */
    private String locationName;

    /**
     * 地区所属省或直辖市
     */
    private String province;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 未读
     */
    private BigDecimal latitude;

    /**
     * 中国行政区域编码
     */
    private String adCode;

}