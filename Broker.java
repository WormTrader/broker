package com.wormtrader.broker;
/********************************************************************
* @(#)Broker.java 1.00 20120312
* Copyright © 2007-2012 by Richard T. Salamone, Jr. All rights reserved.
*
* Broker: Interface specifying the functions of a broker in taking
* and filling orders.
*
* @author Rick Salamone
* @version 1.00
* 20120312 rts created
* 20120429 rts added generalize placeOrder method
* 20120505 rts added stop order type
* 20120510 rts added getOrderList: AT/CT can see/cancel orders
* 20121002 rts cancelOrder takes OrderTracker rather than order id
* 20121017 rts eliminated placeMktOrder
*******************************************************/
import com.wormtrader.positions.PositionLeg;

public interface Broker
	{
	/** Order types... */
	public static final String MKT_ORDER="MKT";
	public static final String LMT_ORDER="LMT";
	public static final String STP_ORDER="STP";
	public static final String[] ORDER_TYPES = {MKT_ORDER, LMT_ORDER, STP_ORDER};

	abstract public void setTime(long aTime);
	abstract long time();

	abstract void placeOrder(OrderTracker aTracker);
	abstract public boolean getLogOrders();
	abstract public void setLogOrders(boolean on);
	abstract public void cancelOrder(OrderTracker aTracker);
	abstract public OrderList getOrderList();
	}
