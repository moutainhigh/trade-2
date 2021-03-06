package com.liantuo.trade.client.trade.packet.body.single_payment;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Pattern;

import com.liantuo.trade.client.trade.packet.Paging;
import com.liantuo.trade.client.trade.packet.body.TradeRequestBody;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.validate.Format;
import com.liantuo.trade.validator.date.DateTime;
/**
 * @ClassName:   QueryPage4SinlePaymentDetail.java
 * @Description: 0006_001_999 V1.0
 * @Company:     联拓金融信息服务有限公司
 * @author       zoran.huang
 * @version      V1.0  
 * @date         2016年7月8日 上午11:03:26
 */
public class InnerQueryPage4SinlePaymentDetail extends TradeRequestBody implements Paging {

	/**
	 * 参数名：交易编号<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "trade_no format error", regexp = ".{0}|([\\d|a-zA-Z|_]{8,64})", groups = Format.class)
	private String trade_no;

	/**
	 * 参数名：付款方外部收付款备注4【提交第三方批次号】<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "external_pay_channel_batch_no format error", regexp = ".{0,1024}", groups = Format.class)
	private String external_pay_channel_batch_no;
	/**
	 * 参数名：付款方外部收付款备注5【提交第三方请求流水号】 <br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "external_pay_channel_serial_no format error", regexp = ".{0,1024}", groups = Format.class)
	private String external_pay_channel_serial_no;
	/**
	 * 参数名：交易发起方发起请求编号<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "out_trade_no_ext format error", regexp = ".{0}|([\\d|a-zA-Z|_]{8,64})", groups = Format.class)
	private String out_trade_no_ext;
	/**
	 * 参数名：交易发起方业务系统订单号<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "out_trade_no format error", regexp = ".{0}|([\\d|a-zA-Z|_]{8,64})", groups = Format.class)
	private String out_trade_no;
	/**
	 * 参数名：付款方冻结余额起始<br/>
	 * 参数类型：可空<br/>
	 */
    @DecimalMin(message = "frozen_amt_start format error,max length 13", groups = { Format.class }, value = "0.00")
    @Digits(message = "frozen_amt_start format error,max length 13", groups = { Format.class }, integer = TradeConstants.DIGIT_AMOUNT_LENGTH, fraction = 2)
	private String frozen_amt_start;
	/**
	 * 参数名：付款方冻结余额截止<br/>
	 * 参数类型：可空<br/>
	 */
    @DecimalMin(message = "frozen_amt_end format error,max length 13", groups = { Format.class }, value = "0.00")
    @Digits(message = "frozen_amt_end format error,max length 13", groups = { Format.class }, integer = TradeConstants.DIGIT_AMOUNT_LENGTH, fraction = 2)
    private String frozen_amt_end;
	/**
	 * 参数名：交易客户保留字段1精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_2 format error", regexp = ".{0,64}", groups = Format.class)
	 private String merchant_extend_field_1;
	/**
	 * 参数名：交易客户保留字段2精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_2 format error", regexp = ".{0,64}", groups = Format.class)
	private String merchant_extend_field_2;
	/**
	 * 参数名：交易客户保留字段3精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_3 format error", regexp = ".{0,64}", groups = Format.class)
	private String merchant_extend_field_3;

	/**
	 * 参数名：交易客户保留字段4精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_4 format error", regexp = ".{0,128}", groups = Format.class)
	private String merchant_extend_field_4;

	/**
	 * 参数名：交易客户保留字段5精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_5 format error", regexp = ".{0,128}", groups = Format.class)
	private String merchant_extend_field_5;
	/**
	 * 参数名：交易客户保留字段6精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_6 format error", regexp = ".{0,256}", groups = Format.class)
	private String merchant_extend_field_6;
	/**
	 * 参数名：交易客户保留字段7精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_7 format error", regexp = ".{0,256}", groups = Format.class)
	private String merchant_extend_field_7;
	/**
	 * 参数名：交易客户保留字段8精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_8 format error", regexp = ".{0,256}", groups = Format.class)
	private String merchant_extend_field_8;
	/**
	 * 参数名：交易客户保留字段9精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_9 format error", regexp = ".{0,256}", groups = Format.class)
	private String merchant_extend_field_9;
	/**
	 * 参数名：交易客户保留字段10精确<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_10 format error", regexp = ".{0,1024}", groups = Format.class)
	private String merchant_extend_field_10;
	/**
	 * 参数名：交易客户保留字段1模糊<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_1_fuzzy format error", regexp = ".{0,64}", groups = Format.class)
	private String merchant_extend_field_1_fuzzy;
	/**
	 * 参数名：交易客户保留字段2模糊<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_2_fuzzy format error", regexp = ".{0,64}", groups = Format.class)
	private String merchant_extend_field_2_fuzzy;
	/**
	 * 参数名：交易客户保留字段3模糊<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_3_fuzzy format error", regexp = ".{0,64}", groups = Format.class)
	private String merchant_extend_field_3_fuzzy;
	/**
	 * 参数名：交易客户保留字段4模糊<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_4_fuzzy format error", regexp = ".{0,128}", groups = Format.class)
	private String merchant_extend_field_4_fuzzy;
	/**
	 * 参数名：交易客户保留字段5模糊<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "merchant_extend_field_5_fuzzy format error", regexp = ".{0,128}", groups = Format.class)
	private String merchant_extend_field_5_fuzzy;
	/**
	 * 参数名：交易创建日期时间起始<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_created_start;
	/**
	 * 参数名：交易创建日期时间结束<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_created_end;
	/**
	 * 参数名：最后变更日期时间起始<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_latest_modified_start;
	/**
	 * 参数名：最后变更日期时间截止<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_latest_modified_end;
	/**
	 * 参数名：支付成功日期时间起始<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_success_pay_start;
	/**
	 * 参数名：支付成功日期时间截止<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_success_pay_end;

	/**
	 * 参数名：等待入账日期时间起始<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_wait_in_accountting_start;
	/**
	 * 参数名：等待入账日期时间截止<br/>
	 * 参数类型：可空<br/>
	 */
    @DateTime(message = "gmt_created_start format error", groups = Format.class)
	private String gmt_wait_in_accountting_end;
	/**
	 * 参数名：交易组别<br/>
	 * 参数类型：可空<br/>
	 */
    @Pattern(message = "trade_catagory format error", regexp = "({0})||([\\d]{1}(\\^[\\d]{1})*)", groups = Format.class)
	private String trade_catagory;
	/**
	 * 参数名：交易状态<br/>
	 * 参数类型：可空<br/>
	 */
	@Pattern(message = "status format error", regexp = "({0})||([\\d]{2}(\\^[\\d]{2})*)", groups = Format.class)
	private String status;

	// 分页页数 字符串 可空 默认0页，0表示第一页, 如总页数为20页，则分页页数为0-19页
	@Pattern(message = "page format error", regexp = "(.{0})||(0|[1-9][\\d]{0,7})", groups = Format.class)
	private String page = "0";

	/**
	 * 参数名：分页每页条数<br/>
	 * 参数类型：可空<br/>
	 * 分页每页条数 字符串 可空 默认50条，最大100条
	 */
	@Pattern(message = "page_size format error", regexp = "(.{0})||([1-9][0-9]?0?)", groups = Format.class)
	private String page_size = "50";

	
	
	public String getTrade_no() {
		return trade_no;
	}

	public void setTrade_no(String trade_no) {
		this.trade_no = trade_no;
	}

	public String getExternal_pay_channel_batch_no() {
		return external_pay_channel_batch_no;
	}

	public void setExternal_pay_channel_batch_no(String external_pay_channel_batch_no) {
		this.external_pay_channel_batch_no = external_pay_channel_batch_no;
	}

	public String getExternal_pay_channel_serial_no() {
		return external_pay_channel_serial_no;
	}

	public void setExternal_pay_channel_serial_no(String external_pay_channel_serial_no) {
		this.external_pay_channel_serial_no = external_pay_channel_serial_no;
	}

	public String getOut_trade_no_ext() {
		return out_trade_no_ext;
	}

	public void setOut_trade_no_ext(String out_trade_no_ext) {
		this.out_trade_no_ext = out_trade_no_ext;
	}

	public String getOut_trade_no() {
		return out_trade_no;
	}

	public void setOut_trade_no(String out_trade_no) {
		this.out_trade_no = out_trade_no;
	}

	public String getFrozen_amt_start() {
		return frozen_amt_start;
	}

	public void setFrozen_amt_start(String frozen_amt_start) {
		this.frozen_amt_start = frozen_amt_start;
	}

	public String getFrozen_amt_end() {
		return frozen_amt_end;
	}

	public void setFrozen_amt_end(String frozen_amt_end) {
		this.frozen_amt_end = frozen_amt_end;
	}

	public String getMerchant_extend_field_1() {
		return merchant_extend_field_1;
	}

	public void setMerchant_extend_field_1(String merchant_extend_field_1) {
		this.merchant_extend_field_1 = merchant_extend_field_1;
	}

	public String getMerchant_extend_field_2() {
		return merchant_extend_field_2;
	}

	public void setMerchant_extend_field_2(String merchant_extend_field_2) {
		this.merchant_extend_field_2 = merchant_extend_field_2;
	}

	public String getMerchant_extend_field_3() {
		return merchant_extend_field_3;
	}

	public void setMerchant_extend_field_3(String merchant_extend_field_3) {
		this.merchant_extend_field_3 = merchant_extend_field_3;
	}

	public String getMerchant_extend_field_4() {
		return merchant_extend_field_4;
	}

	public void setMerchant_extend_field_4(String merchant_extend_field_4) {
		this.merchant_extend_field_4 = merchant_extend_field_4;
	}

	public String getMerchant_extend_field_5() {
		return merchant_extend_field_5;
	}

	public void setMerchant_extend_field_5(String merchant_extend_field_5) {
		this.merchant_extend_field_5 = merchant_extend_field_5;
	}

	public String getMerchant_extend_field_6() {
		return merchant_extend_field_6;
	}

	public void setMerchant_extend_field_6(String merchant_extend_field_6) {
		this.merchant_extend_field_6 = merchant_extend_field_6;
	}

	public String getMerchant_extend_field_7() {
		return merchant_extend_field_7;
	}

	public void setMerchant_extend_field_7(String merchant_extend_field_7) {
		this.merchant_extend_field_7 = merchant_extend_field_7;
	}

	public String getMerchant_extend_field_8() {
		return merchant_extend_field_8;
	}

	public void setMerchant_extend_field_8(String merchant_extend_field_8) {
		this.merchant_extend_field_8 = merchant_extend_field_8;
	}

	public String getMerchant_extend_field_9() {
		return merchant_extend_field_9;
	}

	public void setMerchant_extend_field_9(String merchant_extend_field_9) {
		this.merchant_extend_field_9 = merchant_extend_field_9;
	}

	public String getMerchant_extend_field_10() {
		return merchant_extend_field_10;
	}

	public void setMerchant_extend_field_10(String merchant_extend_field_10) {
		this.merchant_extend_field_10 = merchant_extend_field_10;
	}

	public String getMerchant_extend_field_1_fuzzy() {
		return merchant_extend_field_1_fuzzy;
	}

	public void setMerchant_extend_field_1_fuzzy(String merchant_extend_field_1_fuzzy) {
		this.merchant_extend_field_1_fuzzy = merchant_extend_field_1_fuzzy;
	}

	public String getMerchant_extend_field_2_fuzzy() {
		return merchant_extend_field_2_fuzzy;
	}

	public void setMerchant_extend_field_2_fuzzy(String merchant_extend_field_2_fuzzy) {
		this.merchant_extend_field_2_fuzzy = merchant_extend_field_2_fuzzy;
	}

	public String getMerchant_extend_field_3_fuzzy() {
		return merchant_extend_field_3_fuzzy;
	}

	public void setMerchant_extend_field_3_fuzzy(String merchant_extend_field_3_fuzzy) {
		this.merchant_extend_field_3_fuzzy = merchant_extend_field_3_fuzzy;
	}

	public String getMerchant_extend_field_4_fuzzy() {
		return merchant_extend_field_4_fuzzy;
	}

	public void setMerchant_extend_field_4_fuzzy(String merchant_extend_field_4_fuzzy) {
		this.merchant_extend_field_4_fuzzy = merchant_extend_field_4_fuzzy;
	}

	public String getMerchant_extend_field_5_fuzzy() {
		return merchant_extend_field_5_fuzzy;
	}

	public void setMerchant_extend_field_5_fuzzy(String merchant_extend_field_5_fuzzy) {
		this.merchant_extend_field_5_fuzzy = merchant_extend_field_5_fuzzy;
	}

	public String getGmt_created_start() {
		return gmt_created_start;
	}

	public void setGmt_created_start(String gmt_created_start) {
		this.gmt_created_start = gmt_created_start;
	}

	public String getGmt_created_end() {
		return gmt_created_end;
	}

	public void setGmt_created_end(String gmt_created_end) {
		this.gmt_created_end = gmt_created_end;
	}

	public String getGmt_latest_modified_start() {
		return gmt_latest_modified_start;
	}

	public void setGmt_latest_modified_start(String gmt_latest_modified_start) {
		this.gmt_latest_modified_start = gmt_latest_modified_start;
	}

	public String getGmt_latest_modified_end() {
		return gmt_latest_modified_end;
	}

	public void setGmt_latest_modified_end(String gmt_latest_modified_end) {
		this.gmt_latest_modified_end = gmt_latest_modified_end;
	}

	public String getGmt_success_pay_start() {
		return gmt_success_pay_start;
	}

	public void setGmt_success_pay_start(String gmt_success_pay_start) {
		this.gmt_success_pay_start = gmt_success_pay_start;
	}

	public String getGmt_success_pay_end() {
		return gmt_success_pay_end;
	}

	public void setGmt_success_pay_end(String gmt_success_pay_end) {
		this.gmt_success_pay_end = gmt_success_pay_end;
	}

	public String getGmt_wait_in_accountting_start() {
		return gmt_wait_in_accountting_start;
	}

	public void setGmt_wait_in_accountting_start(String gmt_wait_in_accountting_start) {
		this.gmt_wait_in_accountting_start = gmt_wait_in_accountting_start;
	}

	public String getGmt_wait_in_accountting_end() {
		return gmt_wait_in_accountting_end;
	}

	public void setGmt_wait_in_accountting_end(String gmt_wait_in_accountting_end) {
		this.gmt_wait_in_accountting_end = gmt_wait_in_accountting_end;
	}

	public String getTrade_catagory() {
		return trade_catagory;
	}

	public void setTrade_catagory(String trade_catagory) {
		this.trade_catagory = trade_catagory;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String getPage() {
		return page;
	}

	@Override
	public String getPage_size() {
		return page_size;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public void setPage_size(String page_size) {
		this.page_size = page_size;
	}
}
