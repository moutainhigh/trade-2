package com.liantuo.trade.client.trade.packet.body.zf;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import com.liantuo.trade.common.validate.Format;

public class AliPaymentAsynAttachment extends Attachment  {
	//Token	字符串40	否	
    @Pattern(message = "app_auth_token format error", regexp = ".{0,40}", groups = Format.class)
	private String app_auth_token;
    
	//卖方ID	字符串28	否	
    @Pattern(message = "seller_id format error", regexp = ".{0,28}", groups = Format.class)
	private String seller_id;
    
	//买家支付宝账号	字符串100	否	
    @Pattern(message = "buyer_logon_id format error", regexp = ".{0,100}", groups = Format.class)
	private String buyer_logon_id;
    
	//订单包含的商品列表信息goods_detail[]	否 json格式
    //TODO 需要确定是否需要长度
    @Valid
	private List<Goods_Detail> goods_detail;
    
	//商户操作员编号	字符串28	否	
    @Pattern(message = "operator_id format error", regexp = ".{0,28}", groups = Format.class)
	private String operator_id;
    
	//商户门店编号	字符串32	否	
    @Pattern(message = "store_id format error", regexp = ".{0,32}", groups = Format.class)
	private String store_id;
    
	//商户机具终端编号	字符串32	否
    @Pattern(message = "terminal_id format error", regexp = ".{0,32}", groups = Format.class)
	private String terminal_id;
    
	//订单失效时间	字符串6	否 逾期将关闭交易。取值范围：1m～15d。m-分钟，h-小时，d-天，1c-当天（1c-当天的情况下，无论交易何时创建，都在0点关闭）。 该参数数值不接受小数点， 如 1.5h，可转换为 90m
    @Pattern(message = "timeout_express format error", regexp = ".{0,6}", groups = Format.class)
    private String timeout_express;
    
	//业务扩展参数	extend_params	否 json格式
    @Valid
    private Extend_params extend_params	;
	public String getApp_auth_token() {
		return app_auth_token;
	}
	public void setApp_auth_token(String app_auth_token) {
		this.app_auth_token = app_auth_token;
	}
	public String getSeller_id() {
		return seller_id;
	}
	public void setSeller_id(String seller_id) {
		this.seller_id = seller_id;
	}
	public String getBuyer_logon_id() {
		return buyer_logon_id;
	}
	public void setBuyer_logon_id(String buyer_logon_id) {
		this.buyer_logon_id = buyer_logon_id;
	}
	public List<Goods_Detail> getGoods_detail() {
		return goods_detail;
	}
	public void setGoods_detail(List<Goods_Detail> goods_detail) {
		this.goods_detail = goods_detail;
	}
	public String getOperator_id() {
		return operator_id;
	}
	public void setOperator_id(String operator_id) {
		this.operator_id = operator_id;
	}
	public String getStore_id() {
		return store_id;
	}
	public void setStore_id(String store_id) {
		this.store_id = store_id;
	}
	public String getTerminal_id() {
		return terminal_id;
	}
	public void setTerminal_id(String terminal_id) {
		this.terminal_id = terminal_id;
	}
	public String getTimeout_express() {
		return timeout_express;
	}
	public void setTimeout_express(String timeout_express) {
		this.timeout_express = timeout_express;
	}
	public Extend_params getExtend_params() {
		return extend_params;
	}
	public void setExtend_params(Extend_params extend_params) {
		this.extend_params = extend_params;
	}
	
	
}
