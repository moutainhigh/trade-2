package com.liantuo.deposit.advanceaccount.bus.service;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.ArrayList;
import java.util.List;

import com.ebill.framework.exception.BusinessException;
import com.liantuo.deposit.advanceaccount.bus.service.AccountForbidTradeService;
import com.liantuo.deposit.advanceaccount.orm.pojo.AccountForbidTrade;
import com.liantuo.deposit.advanceaccount.orm.pojo.CreditAccount;
import com.liantuo.deposit.advanceaccount.web.vo.req.AccountForbidReqVo;
import com.liantuo.deposit.advanceaccount.web.vo.req.AccountForbidUpdateReqVo;
import com.liantuo.unittest.mockito.IsTClass;

public class AccountForbidServiceMock {
	
	public static void mockCreateAccountForbid(AccountForbidTradeService accountForbidTradeService,int returnObject) throws Exception{
		doReturn(returnObject).when(accountForbidTradeService).createAccountForbid(
				argThat(new IsTClass<AccountForbidUpdateReqVo>(new AccountForbidUpdateReqVo())),
				argThat(new IsTClass<List<AccountForbidTrade>>(new ArrayList<AccountForbidTrade>())),
				argThat(new IsTClass<List<AccountForbidReqVo>>(new ArrayList<AccountForbidReqVo>())));
	}
	public static void mockMasterSelectByCA(AccountForbidTradeService accountForbidTradeService,List<AccountForbidTrade> returnObject){
		doReturn(returnObject).when(accountForbidTradeService).masterSelectByCA(
				argThat(new IsTClass<String>(new String())));
	}
	public static void mockSelectWhetherForbidAllByCA(AccountForbidTradeService accountForbidTradeService,AccountForbidTrade returnObject){
		doReturn(returnObject).when(accountForbidTradeService).selectWhetherForbidAllByCA(
				argThat(new IsTClass<String>(new String())));
	}
	public static void mockSelectWhetherForbidThisTrade(AccountForbidTradeService accountForbidTradeService,AccountForbidTrade returnObject){
		doReturn(returnObject).when(accountForbidTradeService).selectWhetherForbidThisTrade(
				argThat(new IsTClass<AccountForbidTrade>(new AccountForbidTrade())));
	} 
	
	public static void mockBusinessException(AccountForbidTradeService accountForbidTradeService,AccountForbidTrade returnObject) throws Exception{
		doThrow(new BusinessException()).when(accountForbidTradeService).createAccountForbid(
				argThat(new IsTClass<AccountForbidUpdateReqVo>(new AccountForbidUpdateReqVo())),
				argThat(new IsTClass<List<AccountForbidTrade>>(new ArrayList<AccountForbidTrade>())),
				argThat(new IsTClass<List<AccountForbidReqVo>>(new ArrayList<AccountForbidReqVo>())));	} 
	 
}
