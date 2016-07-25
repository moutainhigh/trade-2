package com.liantuo.deposit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.liantuo.deposit.testsuite.AllControllerTestSuite;
import com.liantuo.deposit.testsuite.AllDepositMapperTestSuite;
import com.liantuo.deposit.testsuite.AllDepositServiceTestSuite;
import com.liantuo.deposit.testsuite.AllProcessorTestSuite;

/**
 * 账务模块所有单元测试 com.liantuo.deposit.*
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ AllDepositMapperTestSuite.class, AllDepositServiceTestSuite.class,
		AllControllerTestSuite.class,AllProcessorTestSuite.class})
public class AllDepositTestSuite {

}
