package com.wormtrader.broker;
/********************************************************************
* @(#)OrderTracker.java 1.00 20120508
* Copyright © 2012-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* OrderTracker: The basic order object for the WormTrader apps. A OrderTracker
* object is created by the Broker when an order is placed, and is
* added to the open order list. It is updated to reflect activity
* and status such as sent to broker, received by broker, partial fill,
* cancel requested.
*
* @version 1.00
* @author Rick Salamone
* 20120508 rts created by generalizing former OrderTracker
* 20130307 rts added merge to combine "matching" soft orders
* 20130424 rts added signal state and send() method
* 20130425 rts added isCancelable() and cancel() methods
* 20130427 rts upgraded for signal processing rewrite
* 20130505 rts vebose flag for logging
*******************************************************/
import static com.wormtrader.broker.Broker.STP_ORDER;
import static com.wormtrader.broker.Broker.LMT_ORDER;
import static com.wormtrader.broker.Broker.MKT_ORDER;
import com.wormtrader.broker.Broker;
import com.wormtrader.positions.PositionLeg;
import com.wormtrader.positions.Trader;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBFormat;
import com.shanebow.util.SBLog;
import static java.awt.Color.WHITE;
import static java.awt.Color.YELLOW;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.MAGENTA;
import java.awt.Color;

public class OrderTracker
	{
	public static final int SOFT_ID=-1;
	public static final int SOFT_STOP_ID=-2;

	/** Action types for the order */
	public static final byte ACT_BTC=(byte)1;
	public static final byte ACT_BUY=(byte)2;
	public static final byte ACT_LONG= (byte)3;
	public static final byte ACT_STC= (byte)4;
	public static final byte ACT_SELL=(byte)5;
	public static final byte ACT_SHORT=(byte)6;
	public static final byte ACT_STOP=(byte)7;
	public static final String[] ACT_DESC=
		{ "?", "BTC", "Buy", "Long", "STC", "Sell", "Short", "Stop" };
	public static final Color[] ACT_COLOR=
		{ WHITE, BLUE, BLUE, BLUE, RED, RED, RED, MAGENTA };

	/** Possible states of the order being tracked */
	public static final byte SIGNAL   =(byte)0; // Unconfirmed signal
	public static final byte SOFT     =(byte)1;
	public static final byte SENT     =(byte)2;
	public static final byte OPEN     =(byte)3;
	public static final byte CAN_REQ  =(byte)4;
	public static final byte CANCELED=(byte)5;
	public static final byte PART_FILL=(byte)6;
	public static final byte FILLED   =(byte)7;
	public static final byte ERROR    =(byte)8;
	public static final String[] STATE_DESC=
		{ "Disabled", "Soft", "Sent", "Open", "Canceling", "Canceled",
		  "Partial", "Filled", "Rejected" };

	public boolean isAtBroker()
		{
		return fState == SENT || fState == OPEN || fState == CAN_REQ;
		}

	protected final PositionLeg fLeg;
	private int         fID;          // Broker id if sent, otherwise undefined
	private final long  fTime;        // time order created
	private int         fQty;         // signed value!
	private String      fType;        // MKT, LMT, STP
	private int         fLmt;         // limit price in cents
	private int         fAux;         // aux price in cents
	private int         fInitialStop; // stop order to be created when this order fills
	protected byte      fAct;         // byte flags ACT_BUY, ACT_LONG, etc

	/**
	* Reason for this order: Can be an initial stop or an exit of some sort
	*/
	private String  fReason;

	private int     fFilled;
	private String  fTIF = "???";
	private String  fStatus = "";
	private String  fWhyHeld = null;
	private byte    fState;

	/**
	* Legacy Constructor which does not specify a reason or an initial stop.
	*/
	public OrderTracker (int aID, PositionLeg aLeg, String aType, int aQty,
		int aLmt, int aAux)
		{
		this(aID, aLeg, aType, aQty, aLmt, aAux, 0, null);
		}

	/**
	* Constructor for creating a soft (not yet sent) order
	*/
	public OrderTracker (PositionLeg aLeg, String aType, int aQty,
		int aLmt, int aAux, int aIStop, String aReason)
		{
		this(SOFT_ID, aLeg, aType, aQty, aLmt, aAux, aIStop, aReason);
		}

	/**
	* Main constructor for explicitly specifying a quantity
	* @param int aID - the unique broker order ID or SOFT_ID if not sent yet
	* @param String aType - MKT, LMT, or STP
	* @param int aQty - signed quantity (negative means sell)
	* @param int aLmt - the stop or limit price in cents if a STP or LMT type
	*                     (ignored for market orders)
	* @param int aAux - the aux price in cents (currently unused)
	* @param int aIStop - the initial stop price to set when this order executes
	*                     (ignored if 0 or if this order closes a position)
	* @param aReason - the user's reason for this order - saved in the exec database
	*/
	public OrderTracker (int aID, PositionLeg aLeg, String aType, int aQty,
		int aLmt, int aAux, int aIStop, String aReason)
		{
		fID = aID;
		fLeg = aLeg;
		fType = aType;
		fQty = aQty;
		fLmt = aLmt;
		fAux = aAux;
		fFilled = 0;
		fAct = (aQty > 0)? ACT_BUY : ACT_SELL;
		fState = (fID <= SOFT_ID)? SOFT : SENT;
		fInitialStop = aIStop;
		fReason = aReason;
		fTime = Trader.time();
		}

	/**
	* For TradeSignal
	*/
	protected OrderTracker (PositionLeg aLeg, byte aAct, String aType,
		int aLmt, int aIStop, String aReason)
		{
		fID = SOFT_ID;
		fLeg = aLeg;
		fAct = aAct;
		fType = aType;
		fLmt = aLmt;
		fAux = 0;
		fFilled = 0;
		fInitialStop = 0;
		fReason = aReason;
		fTime = Trader.time();
		fState = (fID <= SOFT_ID)? SOFT : SENT;
		if (fAct == ACT_STOP)
			{
			fQty = 0;
			send();
			}
		else
			{
			fState = aLeg.isAutoTraded()? SOFT : SIGNAL;
			int oneUnit = fLeg.getStrategy().getTrader().sizePosition(fLeg);
			fQty = isBuy() ? oneUnit
			     : isSell() ? -oneUnit
			     : 0;
			}
		}

	protected final boolean isBuy()
		{ return (fAct==ACT_BTC || fAct==ACT_BUY || fAct==ACT_LONG); }
	protected final boolean isSell()
		{ return (fAct==ACT_STC || fAct == ACT_SELL || fAct==ACT_SHORT); }

	public final String qtyString()
		{
		return (isAtBroker() || fState ==PART_FILL)? "" + fQty
		     : (fAct == ACT_BUY) ? "" + fQty
		     : (fAct == ACT_SELL) ? "" + fQty
		     : (fAct == ACT_LONG) ? "C+" + fQty
		     : (fAct == ACT_SHORT) ? "C" + fQty
		     : "Close"; // (fAct == ACT_BTC || fAct == ACT_STC)
		}

	public final int  getFilled() { return fFilled; }
	public void setFilled(int aFilled)
		{
		fFilled = (aFilled * fQty < 0)? -aFilled : aFilled;
		if (fFilled == fQty)
			setState(FILLED);
		else if (fFilled != 0)
			setState(PART_FILL);
		}

	public final boolean     atLeastPartiallyFilled() { return fFilled != 0; }
	public final byte        getState() { return fState; }
	public       void        setState(byte state) { fState = state; }

	public final byte        act()        { return fAct; }
	public final String      action()     { return (fQty > 0)? "BUY" : "SELL"; }
	public final int         id()         { return fID; }
	public final PositionLeg leg()        { return fLeg; }
	public final int         qty()        { return fQty; }
	public final long        time()       { return fTime; }
	public final String      type()       { return fType; }
	public final int         getLmt()     { return fLmt; }
	public final int         getAux()     { return fAux; }

	public final String      getTIF()     { return fTIF; }
	public final void        setTIF(String aTIF) { fTIF = aTIF; }

	public final void setID(int id) { fID = id; }
	public final void setStatus(String aStatus, String aWhyHeld)
		{
		fStatus = aStatus;
		fWhyHeld = aWhyHeld;
		}

	public boolean isPriorTo(OrderTracker aNother)
		{
		return (fTime < aNother.fTime);
		}

	/**
	* @return true if this is a soft order - an order that is being monitored
	* by the PositionLeg, that has not been sent to the broker yet
	*/
	public final boolean isSoft() { return fState < SENT; }

	public final boolean isSent() { return fState >= SENT; }

	/**
	* A PositionLeg calls this method on all of its orders every time a new
	* "last" price is recieved by the leg.
	* @return true if this is a soft order and it should be triggered based on
	* the specified price. Triggered means the leg should stop monitoring the
	* order whether because it was sent to broker or cancelled.
	*/
	public boolean softTriggered(int aPrice)
		{
		return fState == SOFT
		    && triggered(aPrice)
		    && send();
		}

	public final boolean send()
		{
		setType(MKT_ORDER);
		fLeg.getTrader().getBroker().placeOrder(this);
		setState(SENT);
		return true;
		}

	public void setType(String aType) { fType = aType; }
	/**
	* Intended for use by the simulator's broker
	* @return true if this order is triggered at the specified price
	*/
	public boolean triggered(int price)
		{
		if (fType.equals(STP_ORDER))
			{
			if ((fQty > 0 && price < fLmt)
			||  (fQty < 0 && price > fLmt))
				return false;
			}
		else if (fType.equals(LMT_ORDER))
			{
			if ((fQty > 0 && price > fLmt)
			||  (fQty < 0 && price < fLmt))
				return false;
			}
		return true;
		}

	public final void setQty(int qty)
		{
		fQty = qty;
		}

	public final void modify(int qty, int lmt)
		{
		setQty(qty);
		fLmt = lmt;
		}

	public final void modify(String type, int qty, int lmt, int aux)
		{
		fQty = qty;
		fLmt = lmt;
		fAux = aux;
		fType = type;
		}

	public final String getStatus()
		{
		if ( fWhyHeld != null && !fWhyHeld.isEmpty())
			return fStatus + ": " + fWhyHeld;
		return fStatus;
		}

	@Override public String toString()
		{
		return String.format("Order(%d) %s %d/%d %s: %s ", fID, action(),
				fFilled, fQty, fLeg.toString(), getStatus());
		}

	/**
	* called when saving a SoftOrder to the position file
	*/
	public final String toCSV()
		{
		return fType + "," + fQty + "," + fLmt + "," + fAux
		             + "," + fInitialStop + "," + fReason; }

	public final String toolTip()
		{
		String msg = "<font color=" + ((fQty > 0)? "BLUE>":"RED>" + ACT_DESC[fAct]);
		if (fAct == ACT_BUY || fAct == ACT_SELL)
			msg += Math.abs(fQty);
		msg += "<br>" + fType + " $" + SBFormat.toDollarString(fLmt)
		    + " " + STATE_DESC[fState];
		return msg;
		}

	@Override public boolean equals(Object aOther)
		{
		return aOther != null
		    && aOther instanceof OrderTracker
		    && equals((OrderTracker)aOther);
		}

	public boolean equals(OrderTracker aOther)
		{
		return (fState >= SENT)? aOther.fID == this.fID
		     : aOther == this;
		}

	public boolean merge(OrderTracker aOther)
		{
		if ((fState >= SENT)
		||  (aOther.fState >= SENT)
		||  (fType != aOther.fType)
		||  (fAux != aOther.fAux)
		||  (fLmt != aOther.fLmt)
		||  (fInitialStop != aOther.fInitialStop))
			return false;
		fQty += aOther.fQty;
		if (fQty != 0 && !fReason.equals(aOther.fReason))
			fReason += "/" + aOther.fReason;
		return true;
		}

	public boolean equals(int aHardID)
		{
		return (fState >= SENT) && (aHardID == this.fID);
		}

	public final String getReason()
		{
		return (fReason==null)? "?" : fReason;
		}
	public final void setReason(String aReason) { fReason = aReason; }

	/**
	* Initial stop placement when this order is executed (if opening order)
	*/
	public final int getInitialStop() { return fInitialStop; }
	public final void setInitialStop(int cents) { fInitialStop = cents; }

	public final boolean isEnablable() { return (fState == SIGNAL); }
	public final void enable()
		{
		if ( isEnablable()) setState(SOFT);
		}

	/**
	* Cancel Support
	*/
	public final boolean isCancelable()
		{
		return (fState < SENT)
		    || (fState == OrderTracker.OPEN)
		    || (fState == OrderTracker.PART_FILL);
		}

	public boolean cancel(boolean userInitiated)
		{
		if (fState < SENT)
			{
			setState(CANCELED);
			if (userInitiated) fLeg.remove(this);
			return true;
			}
		else if (fState == OPEN || fState == PART_FILL)
			{
			setState(CAN_REQ);
			fLeg.getTrader().getBroker().cancelOrder(this);
			return true;
			}
		return false;
		}

	public static boolean _verbose;
	protected final void log(String fmt, Object... args)
		{ if (_verbose) SBLog.format(fmt, args); }
	}
