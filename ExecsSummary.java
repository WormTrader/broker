package com.wormtrader.broker;
/********************************************************************
* @(#)ExecsSummary.java 1.00 20120312
* Copyright © 2007-2012 by Richard T. Salamone, Jr. All rights reserved.
*
* ExecsSummary: Data structure to hold a tally of the day's executions.
*
* @author Rick Salamone
* @version 1.00
* 20130224 rts created
*******************************************************/

public final class ExecsSummary
	{
	// Summary Data Fields
	public int m_sldTrades = 0;
	public int	m_sldQty = 0;
	public int m_sldCents = 0;
	public int m_botTrades = 0;
	public int	m_botQty = 0;
	public int m_botCents = 0;

	public int totalNumTrades() { return m_botTrades + m_sldTrades; }
	public int totalQty() { return m_sldQty + m_botQty; }
	public int netCents() { return m_sldCents - m_botCents; }
	public void reset()
		{
		m_sldTrades = 0;
		m_sldQty = 0;
		m_sldCents = 0;
		m_botTrades = 0;
		m_botQty = 0;
		m_botCents = 0;
		}
	}
