package com.liantuo.trade.orm.service;

import java.util.List;

import com.liantuo.trade.orm.pojo.TradeProfitLossLedger;
/**
 * 
 * @ClassName:   TradeProfitLossLedgerService.java
 * @Description: 损益台账记录表操作记录 
 * @Company:     联拓金融信息服务有限公司
 * @author       zoran.huang
 * @version      V1.0  
 * @date         2016年4月26日 下午4:21:21
 */
public interface TradeProfitLossLedgerService {
	/**
	 * @Title:queryTradeLedgerBy 
	 * @Description: 根据业务台账编号，查询收益台账记录
	 * @param ledgerNo 业务台账编号
	 * @return
	 * @return:List<TradeProfitLossLedger>
	 * @author:zoran.huang
	 * @date:2016年4月26日 下午4:39:14
	 */
	List<TradeProfitLossLedger> queryTradeProfitLossLedgerBy(String ledgerNo);
	/**
	 * @Title:insertTradeProfitLossLedger 
	 * @Description: 创建损益台账 
	 * @param record
	 * @return:void
	 * @author:zoran.huang
	 * @date:2016年4月26日 下午4:21:46
	 */
	void insertTradeProfitLossLedger(TradeProfitLossLedger record);
	/**
	 * @Title:updateTradeProfitLossLedger 
	 * @Description: 更新损益台账记录 
	 * @param record
	 * @return:void
	 * @author:zoran.huang
	 * @date:2016年4月26日 下午4:22:11
	 */
	
	void updateTradeProfitLossLedger(TradeProfitLossLedger record);
	/**
	 * @Title:queryByLedgerNo 
	 * @Description:根据台账编号和交易编号查询对应损益台账  
	 * @param ledgerNo 损益台账编号
	 * @param tradeNo 交易编号
	 * @return
	 * @return:TradeProfitLossLedger
	 * @author:zoran.huang
	 * @date:2016年6月6日 下午5:07:13
	 */
	public TradeProfitLossLedger queryByLedgerNoAndTradeNo(String ledgerNo,String tradeNo) ;
	
	
	public List<TradeProfitLossLedger> queryProfitLossLedgerByTradeNo(String tradeNo);
}
