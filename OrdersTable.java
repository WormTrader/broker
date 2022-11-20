package com.wormtrader.broker;
/********************************************************************
* @(#)OrdersTable.java	1.00 ??/??/??
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* OrdersTable: Shows the broker's order list
*
* @version 1.00 07/18/10
* @author Rick Salamone
* 20120509 rts generalized for use with simulators
* 20120514 rts added popup menu for order cancelation
* 20130245 rts calls tracker's cancel method
*******************************************************/
import com.shanebow.ui.SBAction;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class OrdersTable
	extends JTable
	{
	private JPopupMenu fPopup; // table's popup menu
	private int fClickedRow; // row of most recent mouse press

	private final SBAction fActTryCancel
		= new SBAction("Cancel", 'C', "Attempt to cancel this order", null)
			{
			@Override public void actionPerformed(ActionEvent e)
				{
				OrderList model = (OrderList)getModel();
				OrderTracker ot = model.getRow(fClickedRow);
				boolean wasSoft = ot.isSoft();
				ot.cancel(true);
				if (!wasSoft) // soft orders are removed from model, so don't update
					model.fireTableCellUpdated(fClickedRow, OrderList.COL_STATE);
				}
			};

	public void setModel(OrderList aOrders)
		{
		super.setModel(aOrders);
		aOrders.initColumns(this);
		}

	public OrdersTable(OrderList aOrders)
		{
		super(aOrders);
		// set up to handle selection events
//		setSelectionMode ( ListSelectionModel.SINGLE_SELECTION );
		aOrders.initColumns(this);

		//Create the popup menu
		fPopup = new JPopupMenu();
		fPopup.add(fActTryCancel);

		addMouseListener(new MouseAdapter() // to handle double clicks
			{
			public void mousePressed (MouseEvent e)
				{
				fClickedRow = rowAtPoint(e.getPoint());
				if (fClickedRow >= 0
				&&  SwingUtilities.isRightMouseButton(e))
					showPopup(e);
				}
	//		public void mouseReleased (MouseEvent e) { showPopup(e); }
			private void showPopup (MouseEvent e)
				{
				OrderTracker ot = ((OrderList)getModel()).getRow(fClickedRow);
				byte state = ot.getState();
				fActTryCancel.setEnabled(ot.isCancelable());
				fPopup.show (e.getComponent(), e.getX(), e.getY());
				}
			});
		}
	}
