package com.liantuo.trade.bus.process.impl.single_payment;

import javax.annotation.Resource;

import org.springframework.util.StringUtils;

import com.liantuo.trade.bus.process.TradePageQueryInterface;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.v1_1.BizTradePageQueryService;
import com.liantuo.trade.bus.template.impl.v1_1.query.ATradePageQueryNoPaymentTemp;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.TradeResponse;
import com.liantuo.trade.client.trade.packet.body.single_payment.QueryPage4SinlePaymentDetail;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.spring.annotation.JobFlow;

@JobFlow(value = "0006_001_998", version = "1.0", template = ATradePageQueryNoPaymentTemp.class)
public class QueryByPagePaymentOuterProcess implements TradePageQueryInterface<QueryPage4SinlePaymentDetail> {
	@Resource(name = "paymentPageQueryOuterServiceImpl")
	private BizTradePageQueryService bizTradePageQueryServ;

	@Resource
	protected ExceptionService exceptionService;

	@Override
	public void formatValidate(TradeRequest<QueryPage4SinlePaymentDetail> tradeRquest) throws Exception {
		String errMsg;
        errMsg = TradeValidationUtil.validateRequest(tradeRquest, HEADR_EQUIRED_OUTER_SINGLE_PAYMENT, HEADR_EQUIRED_OUTER_SINGLE_PAYMENT, "*");
        if (!StringUtils.isEmpty(errMsg)) {
            throw exceptionService.buildBusinessException("JY00060019981000100", BusinessException.class, new Object[]{errMsg});
        }
	}

	@Override
	public Object query(TradeRequest<QueryPage4SinlePaymentDetail> tradeRquest, TradeResponse tradeResponse) throws Exception {
        return this.bizTradePageQueryServ.queryByPage(tradeRquest, tradeResponse);
	}

}
