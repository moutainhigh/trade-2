package com.liantuo.deposit.advanceaccount.orm.mapper;

import java.util.List;

import com.liantuo.deposit.advanceaccount.orm.pojo.Pool;
import com.liantuo.deposit.advanceaccount.web.inner.vo.queryvo.PoolQueryInnerVo;
import com.liantuo.deposit.advanceaccount.web.inner.vo.rsp.PoolInnerRspVo;
import com.liantuo.deposit.advanceaccount.web.vo.queryvo.PoolQueryVo;
import com.liantuo.deposit.advanceaccount.web.vo.rsp.PoolRspVo;

public interface PoolMapper {

    /**
     * 保存记录,不管记录里面的属性是否为空
     */
    int insert(Pool record);

    /**
     * 根据资金池查询记录
     */
    Pool selectByPoolNo(String poolNo);

	List<PoolRspVo> findPoolsByQueryVO(PoolQueryVo poolQueryVo);
	int countPoolsByQueryVO(PoolQueryVo poolQueryVo);

	List<PoolInnerRspVo> findPoolsByQueryInnerVO(PoolQueryInnerVo poolQueryVo);

	int countPoolsByQueryInnerVO(PoolQueryInnerVo poolQueryVo);

	PoolRspVo findByMerchantNoAndPoolName(PoolQueryVo poolQueryVo);
	
//	/*******未使用的方法begin*********/
//    /**
//     * 根据主键删除记录
//     */
//    int deleteByPrimaryKey(Integer id);
//    /**
//     * 根据主键查询记录
//     */
//    Pool selectByPrimaryKey(Integer id);
//    /**
//     * 根据主键更新记录
//     */
//    int updateByPrimaryKey(Pool record);
//	
	/*******未使用的方法end*********/
}