package app.tuxguitar.ui.swt.widget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import app.tuxguitar.ui.event.UIPaintListener;
import app.tuxguitar.ui.resource.UISize;
import app.tuxguitar.ui.swt.event.SWTPaintListenerManager;
import app.tuxguitar.ui.widget.UICanvas;

public class SWTCanvas extends SWTControl<Composite> implements UICanvas {

	private SWTPaintListenerManager selectionListener;

	public SWTCanvas(SWTContainer<? extends Composite> parent, boolean bordered) {
		this(parent, bordered, false);
	}

	public SWTCanvas(SWTContainer<? extends Composite> parent, boolean bordered, boolean transparent) {
		super(new Composite(parent.getControl(), canvasStyle(bordered, transparent)), parent);

		this.selectionListener = new SWTPaintListenerManager(this);
	}

	private static int canvasStyle(boolean bordered, boolean transparent) {
		int style = SWT.DOUBLE_BUFFERED;
		if (transparent) {
			style |= SWT.NO_BACKGROUND;
		}
		if (bordered) {
			style |= SWT.BORDER;
		}
		return style;
	}

	public void computePackedSize(Float fixedWidth, Float fixedHeight) {
		UISize size = this.getPackedSize();

		this.setPackedSize(new UISize(fixedWidth != null ? fixedWidth : size.getWidth(), fixedHeight != null ? fixedHeight : size.getHeight()));
	}

	public void addPaintListener(UIPaintListener listener) {
		if( this.selectionListener.isEmpty() ) {
			this.getControl().addPaintListener(this.selectionListener);
		}
		this.selectionListener.addListener(listener);
	}

	public void removePaintListener(UIPaintListener listener) {
		this.selectionListener.removeListener(listener);
		if( this.selectionListener.isEmpty() ) {
			this.getControl().removePaintListener(this.selectionListener);
		}
	}
}
