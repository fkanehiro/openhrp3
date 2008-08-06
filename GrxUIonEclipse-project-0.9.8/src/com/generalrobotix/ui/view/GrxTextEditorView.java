package com.generalrobotix.ui.view;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.generalrobotix.ui.GrxBaseItem;
import com.generalrobotix.ui.GrxBaseView;
import com.generalrobotix.ui.GrxBaseViewPart;
import com.generalrobotix.ui.GrxPluginManager;
import com.generalrobotix.ui.grxui.Activator;
import com.generalrobotix.ui.item.GrxTextItem;

@SuppressWarnings("serial")
public class GrxTextEditorView extends GrxBaseView {
	private Text area_;
	private Action save_,saveAs_;

	private GrxTextItem currentItem_ = null;
	
	public GrxTextEditorView(String name, GrxPluginManager manager_,
			GrxBaseViewPart vp, Composite parent) {
		super(name, manager_, vp, parent);
		area_ = new Text( composite_, SWT.MULTI|SWT.V_SCROLL|SWT.BORDER );

		IToolBarManager toolbar = vp.getViewSite().getActionBars().getToolBarManager();

		save_ = new Action() {
            public void run() {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.save();
					//save_.setEnabled(false);
				}
            }
        };
        save_.setToolTipText( "Save" );
        save_.setImageDescriptor( Activator.getDefault().getDescriptor("save_edit.png") );
        toolbar.add( save_ );

		saveAs_ = new Action() {
            public void run() {
				if (currentItem_ != null) {
					currentItem_.setValue(area_.getText());
					currentItem_.saveAs();
				}
            }
        };
        saveAs_.setToolTipText( "Save As" );
        saveAs_.setImageDescriptor( Activator.getDefault().getDescriptor("saveas_edit.png") );
        toolbar.add( saveAs_ );
        setScrollMinSize();

	}

	private void _syncCurrentItem() {
		if (currentItem_ != null) {
			currentItem_.setValue(area_.getText());
			currentItem_.setCaretPosition(area_.getCaretPosition());
		}
	}

	public void itemSelectionChanged(List<GrxBaseItem> itemList) {
		_syncCurrentItem();
		
		currentItem_ = null;
		GrxBaseItem item = manager_.getSelectedItem(GrxTextItem.class, null);
		if (item != null) {
			area_.setText( (String)item.getValue() );
			// keep currentItem_ null until setValue not to set undo
			currentItem_ = (GrxTextItem) item;
//			area_.setCaretPosition(currentItem_.getCaretPositoin());
			area_.setEnabled(true);
	//		area_.setBackground(Color.white);
			//save_.setEnabled(currentItem_.isEdited());
			save_.setEnabled(true);
			saveAs_.setEnabled(true);
	//		undoAction_.updateUndoState();
		//	redoAction_.updateRedoState();
		} else {
			area_.setText("");
			area_.setEnabled(false);
	//		area_.setBackground(Color.lightGray);
			save_.setEnabled(false);
			saveAs_.setEnabled(false);
		}
		
//		_updateCaretLabel();
	//	_updateTitle();
	}
}
