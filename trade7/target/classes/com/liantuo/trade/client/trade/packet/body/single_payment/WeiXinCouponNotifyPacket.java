package com.liantuo.trade.client.trade.packet.body.single_payment;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;

import org.dozer.Mapper;
import org.dozer.Mapping;

import com.liantuo.trade.common.validate.Format;

public class WeiXinCouponNotifyPacket {
	// 代金券或立减优惠ID
	@Pattern(message = "coupon_id content not match", regexp = ".{0,20}", groups = Format.class)
	private String coupon_id;
	// 单个代金券或立减优惠支付金额
	@DecimalMin(message = "coupon_fee format error", groups = { Format.class }, value = "0")
	@Digits(message = "coupon_fee format error", groups = { Format.class }, integer = 13, fraction = 0)
	private String coupon_fee;
	// 代金券或立减优惠批次ID
	private String coupon_batch_id;

	public String getCoupon_id() {
		return coupon_id;
	}

	public void setCoupon_id(String coupon_id) {
		this.coupon_id = coupon_id;
	}

	public String getCoupon_fee() {
		return coupon_fee;
	}

	public void setCoupon_fee(String coupon_fee) {
		this.coupon_fee = coupon_fee;
	}

	public String getCoupon_batch_id() {
		return coupon_batch_id;
	}

	public void setCoupon_batch_id(String coupon_batch_id) {
		this.coupon_batch_id = coupon_batch_id;
	}

}
