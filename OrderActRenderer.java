package com.wormtrader.broker;
/********************************************************************
* @(#)OrderActRenderer.java 1.00 20120318
* Copyright © 2012-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* OrderActRenderer: Displays signal/order actions (CLOSE, BUY, LONG...)
* in vibrant colors.
*
* @author Rick Salamone
* @version 2.00
* 20120318 rts created
* 20130427 rts upgraded for signal processing rewrite
*******************************************************/
import com.wormtrader.broker.OrderTracker;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import java.awt.Color;
import javax.swing.table.DefaultTableCellRenderer;

public class OrderActRenderer
	extends DefaultTableCellRenderer
	{
	public OrderActRenderer()
		{
		super();
		this.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		}

	public void setValue( Object value )
		{
		Color bg = WHITE;
		Color fg = WHITE;
		byte act = (value instanceof OrderTracker)?
		            ((OrderTracker)value).act() : 0;
		bg = OrderTracker.ACT_COLOR[act];
		fg = (bg == WHITE)? BLACK : WHITE;
		setBackground( bg );
		setForeground( fg );
		setText(OrderTracker.ACT_DESC[act]);
		}
	}
