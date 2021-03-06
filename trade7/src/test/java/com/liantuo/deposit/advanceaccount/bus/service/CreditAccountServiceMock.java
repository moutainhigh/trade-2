package com.liantuo.deposit.advanceaccount.bus.service;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.List;

import com.ebill.framework.exception.BusinessException;
import com.liantuo.deposit.advanceaccount.orm.pojo.CreditAccount;
import com.liantuo.deposit.advanceaccount.web.inner.vo.queryvo.CreditAccountQueryInnerVo;
import com.liantuo.deposit.advanceaccount.web.inner.vo.returnvo.CreditAccountInnerVo;
import com.liantuo.deposit.advanceaccount.web.vo.queryvo.CreditAccountQueryVo;
import com.liantuo.deposit.advanceaccount.web.vo.req.OpenCreditAccountReqVo;
import com.liantuo.deposit.advanceaccount.web.vo.returnvo.CreditAccountVo;
import com.liantuo.trade.exception.BusinessUncheckedException;
import com.liantuo.unittest.mockito.IsTClass;

public class CreditAccountServiceMock {
	
	public static void mockServiceVerification(CreditAccountService creditAccountService) throws Exception{
		doNothing().when(creditAccountService).serviceVerification(
				argThat(new IsTClass<OpenCreditAccountReqVo>(new OpenCreditAccountReqVo())));
	}
	
	public static void mockServiceVerificationException(CreditAccountService creditAccountService) throws Exception{
		doThrow(new Exception()).when(creditAccountService).serviceVerification(
				argThat(new IsTClass<OpenCreditAccountReqVo>(new OpenCreditAccountReqVo())));
	}
	
	public static void mockInsertOpenCreditAccount(CreditAccountService creditAccountService,CreditAccount returnCA) throws BusinessException{
		doReturn(returnCA).when(creditAccountService).insertOpenCreditAccount(
				argThat(new IsTClass<OpenCreditAccountReqVo>(new OpenCreditAccountReqVo())));
	}
	
	public static void mockInsertOpenCreditAccountBusinessException(CreditAccountService creditAccountService,CreditAccount returnCA) throws BusinessException{
		doThrow(new BusinessException()).when(creditAccountService).insertOpenCreditAccount(
				argThat(new IsTClass<OpenCreditAccountReqVo>(new OpenCreditAccountReqVo())));
	}
	
	public static void mockFindAccountsQuery(CreditAccountService creditAccountService,List<CreditAccountVo> list){
		doReturn(list).when(creditAccountService).findAdvanceAccountByQueryVO(
				argThat(new IsTClass<CreditAccountQueryVo>(new CreditAccountQueryVo())));
	}
	
	public static void mockFindAccountsQueryBusinessUncheckedException(CreditAccountService creditAccountService,List<CreditAccountVo> list){
		doThrow(new BusinessUncheckedException()).when(creditAccountService).findAdvanceAccountByQueryVO(
				argThat(new IsTClass<CreditAccountQueryVo>(new CreditAccountQueryVo())));
	}
	
	public static void mockFindAccountsQueryException(CreditAccountService creditAccountService,List<CreditAccountVo> list){
		doThrow(new RuntimeException()).when(creditAccountService).findAdvanceAccountByQueryVO(
				argThat(new IsTClass<CreditAccountQueryVo>(new CreditAccountQueryVo())));
	}
	
	public static void mockFindAccountsQueryCount(CreditAccountService creditAccountService,int count){
		doReturn(count).when(creditAccountService).countAdvanceAccountByQueryVO(
				argThat(new IsTClass<CreditAccountQueryVo>(new CreditAccountQueryVo())));
	}
	
	public static void mockFindAccountsInnerQuery(CreditAccountService creditAccountService,List<CreditAccountInnerVo> list){
		doReturn(list).when(creditAccountService).findAdvanceAccountInnerByQueryVO(
				argThat(new IsTClass<CreditAccountQueryInnerVo>(new CreditAccountQueryInnerVo())));
	}
	
	public static void mockFindAccountsInnerQueryException(CreditAccountService creditAccountService,List<CreditAccountInnerVo> list){
		doThrow(new RuntimeException()).when(creditAccountService).findAdvanceAccountInnerByQueryVO(
				argThat(new IsTClass<CreditAccountQueryInnerVo>(new CreditAccountQueryInnerVo())));
	}
	
	public static void mockFindAccountsInnerQueryCount(CreditAccountService creditAccountService,int count){
		doReturn(count).when(creditAccountService).countAdvanceAccountInnerByQueryVO(
				argThat(new IsTClass<CreditAccountQueryInnerVo>(new CreditAccountQueryInnerVo())));
	}
	

	
	

}
