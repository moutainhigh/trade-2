package com.liantuo.trade.bus.process.impl.simple_fund_transfer.v1;

import javax.annotation.Resource;

import org.springframework.util.StringUtils;

import com.liantuo.trade.bus.process.TradePageQueryInterface;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.v1_1.BizTradePageQueryService;
import com.liantuo.trade.bus.template.impl.v1_1.query.ATradePageQueryNoPaymentTemp;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.TradeResponse;
import com.liantuo.trade.client.trade.packet.body.simple_fund_transfer.QueryPage4TransferDetail;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.spring.annotation.JobFlow;

@JobFlow(value = "0007_001_999", version = "1.0", template = ATradePageQueryNoPaymentTemp.class)
public class QueryByPage4TransferInnerProcess implements TradePageQueryInterface<QueryPage4TransferDetail> {

	@Resource(name = "transferPageQueryServiceImpl")
    private BizTradePageQueryService bizTradePageQueryServ;
	
	@Resource
    protected ExceptionService exceptionService;
	
	
	@Override
	public void formatValidate(TradeRequest<QueryPage4TransferDetail> tradeRquest) throws Exception {
		 String errMsg;
	        errMsg = TradeValidationUtil.validateRequest(tradeRquest, HEADR_EQUIRED_INNER, HEAD_FORMAT, "*");
	        if (!StringUtils.isEmpty(errMsg)) {
	            throw exceptionService.buildBusinessException("JY00070019991000100", BusinessException.class, new Object[]{errMsg});
	        }
	}

	@Override
	public Object query(TradeRequest<QueryPage4TransferDetail> tradeRquest, TradeResponse tradeResponse)
			throws Exception {
		return this.bizTradePageQueryServ.queryByPage(tradeRquest, tradeResponse);
	}

}
