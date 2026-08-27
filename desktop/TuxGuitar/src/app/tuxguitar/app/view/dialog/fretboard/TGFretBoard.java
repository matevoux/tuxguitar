package app.tuxguitar.app.view.dialog.fretboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.action.TGActionProcessorListener;
import app.tuxguitar.app.action.impl.caret.TGGoLeftAction;
import app.tuxguitar.app.action.impl.caret.TGGoRightAction;
import app.tuxguitar.app.action.impl.caret.TGMoveToAction;
import app.tuxguitar.app.action.impl.tools.TGOpenScaleDialogAction;
import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.system.icons.TGIconManager;
import app.tuxguitar.app.transport.TGTransport;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.component.tab.TablatureEditor;
import app.tuxguitar.app.view.main.TGWindow;
import app.tuxguitar.app.view.util.TGBufferedPainterListenerLocked;
import app.tuxguitar.app.view.util.TGBufferedPainterLocked.TGBufferedPainterHandle;
import app.tuxguitar.document.TGDocumentContextAttributes;
import app.tuxguitar.editor.TGEditorManager;
import app.tuxguitar.editor.action.TGActionProcessor;
import app.tuxguitar.editor.action.duration.TGDecrementDurationAction;
import app.tuxguitar.editor.action.duration.TGIncrementDurationAction;
import app.tuxguitar.editor.action.note.TGChangeNoteAction;
import app.tuxguitar.editor.action.note.TGDeleteNoteAction;
import app.tuxguitar.player.base.MidiPlayer;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGNote;
import app.tuxguitar.song.models.TGScale;
import app.tuxguitar.song.models.TGString;
import app.tuxguitar.song.models.TGTrack;
import app.tuxguitar.song.models.TGVoice;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.event.UIMouseEvent;
import app.tuxguitar.ui.event.UIMouseUpListener;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.layout.UIAbstractLayout;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.ui.resource.UIColor;
import app.tuxguitar.ui.resource.UIImage;
import app.tuxguitar.ui.resource.UIPainter;
import app.tuxguitar.ui.resource.UIRectangle;
import app.tuxguitar.ui.resource.UISize;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UICanvas;
import app.tuxguitar.ui.widget.UIContainer;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UIImageView;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UISeparator;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMusicKeyUtils;

public class TGFretBoard {

	public static final int MAX_FRETS = 24;
	public static final int TOP_SPACING = 10;
	public static final int BOTTOM_SPACING = 10;

	private static final int STRING_SPACING_MIN = 10;
	private static final int STRING_SPACING_MAX = 60;
	private static final int STRING_SPACING_INCREMENT = 2;
	private static final int FRET_FROM_X = 10;
	private static final int LEARNING_MARGIN_UNITS = 16;
	private static final float LEARNING_MARGIN_MAX_RATIO = 0.35f;

	private TGContext context;
	private TGFretBoardConfig config;
	private UIPanel control;
	private UIPanel toolComposite;
	private UIPanel boardHost;
	private UIImageView durationLabel;
	private UILabel scaleName;
	private UIButton scale;
	private UIButton goLeft;
	private UIButton goRight;
	private UIButton increment;
	private UIButton decrement;
	private UIButton settings;
	private UIButton smaller;
	private UIButton bigger;
	private UIButton learningMode;
	private UIImage fretBoard;
	private TGBeat beat;
	private TGBeat externalBeat;
	private int[] frets = new int[0];
	private int[] strings = new int[0];
	private float fretSpacing;
	private boolean changes;
	private UISize lastSize;
	private int stringSpacing;
	private int lastStringSpacing;
	private int duration;
	protected UIDropDownSelect<Integer> handSelector;
	protected UICanvas fretBoardComposite;
	protected UICanvas notesComposite;
	private float notesLayerOffsetX;
	private List<LearningSprite> learningSprites;
	private long learningLastPlayPrecise;
	private long learningLastStampedPrecise;
	private int learningTrackNumber;
	/** Wall-clock timestamp (ms) when the current count-in phase started. */
	private long learningCountInWallStartMs;
	/**
	 * Precise-time duration of the neck travel (last fret -> fret 0).
	 * Used as look-ahead while playing and as count-in length at start.
	 */
	private long learningLeadInPrecise;
	/** Song precise-time of the first note that will be hit at the end of count-in. */
	private long learningCountInTargetPrecise;
	/** True while MidiPlayerCountDown is running and LM is active. */
	private boolean learningCountInActive;
	/** Frozen metronome tick length (ms) and beat precise duration for the active count-in. */
	private long learningCountInTickMs;
	private long learningBeatPrecise;

	public TGFretBoard(TGContext context, UIContainer parent) {
		this.context = context;
		this.config = new TGFretBoardConfig(context);
		this.config.load();
		this.learningSprites = new ArrayList<LearningSprite>();
		this.resetLearningLayer();
		this.stringSpacing = TuxGuitar.getInstance().getConfig().getIntegerValue(TGConfigKeys.FRETBOARD_STRING_SPACING);
		this.control = getUIFactory().createPanel(parent, false);

		this.initToolBar();
		this.initEditor();
		this.createControlLayout();
		this.loadIcons();
		this.loadProperties();

		TuxGuitar.getInstance().getKeyBindingManager().appendListenersTo(this.toolComposite);
		TuxGuitar.getInstance().getKeyBindingManager().appendListenersTo(this.fretBoardComposite);
		TuxGuitar.getInstance().getKeyBindingManager().appendListenersTo(this.notesComposite);

		this.initLearningMode();
	}

	public void createControlLayout() {
		UITableLayout uiLayout = new UITableLayout(0f);
		uiLayout.set(this.toolComposite, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, false, 1, 1, null, null, 0f);
		uiLayout.set(this.boardHost, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, null, null, 0f);

		this.control.setLayout(uiLayout);
	}

	private void initToolBar() {
		UIFactory uiFactory = getUIFactory();

		int column = 0;

		this.toolComposite = uiFactory.createPanel(this.control, false);
		this.createToolBarLayout();

		// position
		this.goLeft = uiFactory.createButton(this.toolComposite);
		this.goLeft.addSelectionListener(new TGActionProcessorListener(this.context, TGGoLeftAction.NAME));
		this.createToolItemLayout(this.goLeft, ++column);

		this.goRight = uiFactory.createButton(this.toolComposite);
		this.goRight.addSelectionListener(new TGActionProcessorListener(this.context, TGGoRightAction.NAME));
		this.createToolItemLayout(this.goRight, ++column);

		// separator
		this.createToolSeparator(uiFactory, ++column);

		// duration
		this.increment = uiFactory.createButton(this.toolComposite);
		this.increment.addSelectionListener(new TGActionProcessorListener(this.context, TGIncrementDurationAction.NAME));
		this.createToolItemLayout(increment, ++column);

		this.durationLabel = uiFactory.createImageView(this.toolComposite);
		this.createToolItemLayout(this.durationLabel, ++column);

		this.decrement = uiFactory.createButton(this.toolComposite);
		this.decrement.addSelectionListener(new TGActionProcessorListener(this.context, TGDecrementDurationAction.NAME));
		this.createToolItemLayout(decrement, ++column);

		// separator
		this.createToolSeparator(uiFactory, ++column);

		// hand selector
		this.handSelector = uiFactory.createDropDownSelect(this.toolComposite);
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.right-mode"), TGFretBoardConfig.DIRECTION_RIGHT));
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.left-mode"), TGFretBoardConfig.DIRECTION_LEFT));
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, this.getDirection(this.config.getDirection())));
		this.handSelector.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				Integer direction = TGFretBoard.this.handSelector.getSelectedValue();
				if( direction != null ) {
					updateDirection(direction);
				}
			}
		});
		this.createToolItemLayout(this.handSelector, ++column);

		// separator
		this.createToolSeparator(uiFactory, ++column);

		// scale
		this.scale = uiFactory.createButton(this.toolComposite);
		this.scale.setText(TuxGuitar.getProperty("scale"));
		this.scale.addSelectionListener(new TGActionProcessorListener(this.context, TGOpenScaleDialogAction.NAME));
		this.createToolItemLayout(this.scale, ++column);

		// scale name
		this.scaleName = uiFactory.createLabel(this.toolComposite);
		this.createToolItemLayout(this.scaleName, ++column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, false, false);

		// fretboard height
		this.smaller = uiFactory.createButton(this.toolComposite);
		this.smaller.setImage(TuxGuitar.getInstance().getIconManager().getFretboardSmaller());
		this.smaller.setToolTipText(TuxGuitar.getProperty("fretboard.smaller"));
		this.smaller.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGFretBoard.this.updateStringSpacing(-STRING_SPACING_INCREMENT);
			}
		});
		this.createToolItemLayout(this.smaller, ++column, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, false);
		this.bigger = uiFactory.createButton(this.toolComposite);
		this.bigger.setImage(TuxGuitar.getInstance().getIconManager().getFretboardBigger());
		this.bigger.setToolTipText(TuxGuitar.getProperty("fretboard.bigger"));
		this.bigger.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGFretBoard.this.updateStringSpacing(STRING_SPACING_INCREMENT);
			}
		});
		this.createToolItemLayout(this.bigger, ++column, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);

		// settings
		this.settings = uiFactory.createButton(this.toolComposite);
		this.settings.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.SETTINGS));
		this.settings.setToolTipText(TuxGuitar.getProperty("settings"));
		this.settings.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				configure();
			}
		});
		this.createToolItemLayout(this.settings, ++column, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);

		// learning mode
		this.learningMode = uiFactory.createButton(this.toolComposite);
		this.learningMode.setText("LM");
		this.learningMode.setToolTipText(TuxGuitar.getProperty("learning.mode"));
		this.learningMode.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				TGFretBoard.this.toggleLearningMode();
			}
		});
		this.createToolItemLayout(this.learningMode, ++column, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, false, false);

		// separator
		this.createToolSeparator(uiFactory, ++column);

		this.toolComposite.getLayout().set(goLeft, UITableLayout.MARGIN_LEFT, 0f);
		this.toolComposite.getLayout().set(this.settings, UITableLayout.MARGIN_RIGHT, 0f);
	}

	private void updateStringSpacing(int increment) {
		this.lastStringSpacing = this.stringSpacing;
		this.stringSpacing += increment;
		this.stringSpacing = Math.min(this.stringSpacing, STRING_SPACING_MAX);
		this.stringSpacing = Math.max(this.stringSpacing, STRING_SPACING_MIN);
		this.smaller.setEnabled(this.stringSpacing > STRING_SPACING_MIN);
		this.bigger.setEnabled(this.stringSpacing < STRING_SPACING_MAX);
		if (this.stringSpacing != this.lastStringSpacing) {
			TuxGuitar.getInstance().getConfig().setValue(TGConfigKeys.FRETBOARD_STRING_SPACING,this.stringSpacing);
			this.setChanges(true);
			this.updateEditor();
		}
	}

	private void createToolBarLayout(){
		UITableLayout uiLayout = new UITableLayout();
		uiLayout.set(UITableLayout.MARGIN_LEFT, 0f);
		uiLayout.set(UITableLayout.MARGIN_RIGHT, 0f);

		this.toolComposite.setLayout(uiLayout);
	}

	private void createToolItemLayout(UIControl uiControl, int column){
		this.createToolItemLayout(uiControl, column, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, false, false);
	}

	private void createToolItemLayout(UIControl uiControl, int column, Integer alignX, Integer alignY, Boolean fillX, Boolean fillY){
		UITableLayout uiLayout = (UITableLayout) this.toolComposite.getLayout();
		uiLayout.set(uiControl, 1, column, alignX, alignY, fillX, fillX);
	}

	private void createToolSeparator(UIFactory uiFactory, int column){
		UISeparator uiSeparator = uiFactory.createVerticalSeparator(this.toolComposite);
		UITableLayout uiLayout = (UITableLayout) this.toolComposite.getLayout();
		uiLayout.set(uiSeparator, 1, column, UITableLayout.ALIGN_CENTER, UITableLayout.ALIGN_CENTER, false, false);
		uiLayout.set(uiSeparator, UITableLayout.PACKED_WIDTH, 20f);
		uiLayout.set(uiSeparator, UITableLayout.PACKED_HEIGHT, 20f);
	}

	private void initEditor() {
		this.lastSize = new UISize();
		this.boardHost = getUIFactory().createPanel(this.control, false);
		this.boardHost.setLayout(new OverlayLayout());

		// Neck canvas kept only as geometry reference / future plane; not shown.
		// Visible surface is notesComposite: blits neck cache + draws sprites (works on SWT/JFX/QT).
		this.fretBoardComposite = getUIFactory().createCanvas(this.boardHost, false);
		this.fretBoardComposite.setBgColor(this.config.getColorBackground());
		this.fretBoardComposite.setVisible(false);

		this.notesComposite = getUIFactory().createCanvas(this.boardHost, false, false);
		this.notesComposite.setBgColor(this.config.getColorBackground());
		this.notesComposite.addMouseUpListener(new TGFretBoardMouseListener(this.context));
		this.notesComposite.addPaintListener(new TGBufferedPainterListenerLocked(this.context, new TGNotesPainterHandle()));
	}

	private void loadDurationImage(boolean force) {
		int duration = TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getDuration().getValue();
		if(force || this.duration != duration){
			this.duration = duration;
			this.durationLabel.setImage(TuxGuitar.getInstance().getIconManager().getDuration(this.duration));
		}
	}

	private void loadScaleName() {
		int scaleKeyIndex = TuxGuitar.getInstance().getScaleManager().getSelectionKeyIndex();
		int scaleIndex = TuxGuitar.getInstance().getScaleManager().getScaleIndex();
		String key = TuxGuitar.getInstance().getScaleManager().getKeyName( scaleKeyIndex );
		String name = TuxGuitar.getInstance().getScaleManager().getScaleName( scaleIndex );
		this.scaleName.setText( ( key != null && name != null ) ? ( key + " - " + name ) : "" );
	}

	private void calculateFretSpacing(float width) {
		this.fretSpacing = (width / MAX_FRETS);
		int aux = 0;
		for (int i = 0; i < MAX_FRETS; i++) {
			aux += (i * 2);
		}
		this.fretSpacing += (aux / MAX_FRETS) + 2;
	}

	private void disposeFretBoardImage(){
		if( this.fretBoard != null && !this.fretBoard.isDisposed() ){
			this.fretBoard.dispose();
		}
	}

	protected void initFrets(int fromX) {
		this.frets = new int[MAX_FRETS];
		int nextX = fromX;
		int direction = this.getDirection(this.config.getDirection());
		if (direction == TGFretBoardConfig.DIRECTION_RIGHT) {
			for (int i = 0; i < this.frets.length; i++) {
				this.frets[i] = nextX;
				nextX += (this.fretSpacing - ((i + 1) * 2));
			}
		} else if (direction == TGFretBoardConfig.DIRECTION_LEFT) {
			for (int i = this.frets.length - 1; i >= 0; i--) {
				this.frets[i] = nextX;
				nextX += (this.fretSpacing - (i * 2));
			}
		}
	}

	private int getDirection( int value ){
		int direction = value;
		if( direction != TGFretBoardConfig.DIRECTION_RIGHT && direction != TGFretBoardConfig.DIRECTION_LEFT ){
			direction = TGFretBoardConfig.DIRECTION_RIGHT;
		}
		return direction;
	}

	private void initStrings(int count) {
		int fromY = TOP_SPACING;
		this.strings = new int[count];

		for (int i = 0; i < this.strings.length; i++) {
			this.strings[i] = fromY + (this.stringSpacing * i);
		}
	}

	private void updateEditor(){
		if( isVisible() ){
			if( MidiPlayer.getInstance(this.context).isRunning()){
				this.beat = TGTransport.getInstance(this.context).getCache().getPlayBeat();
			}else if(this.externalBeat != null){
				this.beat = this.externalBeat;
			}else{
				this.beat = TablatureEditor.getInstance(this.context).getTablature().getCaret().getSelectedBeat();
			}

			if ((this.strings.length != getStringCount()) || (this.stringSpacing != this.lastStringSpacing)) {
				disposeFretBoardImage();
				initStrings(getStringCount());
				//Fuerzo a cambiar el ancho
				this.lastSize.setHeight(0);
			}

			UIRectangle childArea = this.boardHost.getBounds();
			float clientWidth = childArea.getWidth();
			if (clientWidth <= 0f) {
				clientWidth = this.control.getChildArea().getWidth();
			}
			float clientHeight = this.control.getChildArea().getHeight();

			if( this.lastSize.getWidth() != clientWidth || hasChanges() ){
				this.lastSize.setWidth(clientWidth);
				this.layout(clientWidth);
			}

			if( this.lastSize.getHeight() != clientHeight ) {
				this.lastSize.setHeight(clientHeight);
				TuxGuitar.getInstance().getFretBoardEditor().showFretBoard();
			}
			this.lastStringSpacing = this.stringSpacing;
		}
	}

	private void paintFretBoard(UIPainter painter){
		this.paintFretBoard(painter, 0f);
	}

	private void paintFretBoard(UIPainter painter, float offsetX){
		if(this.fretBoard == null || this.fretBoard.isDisposed()){
			UIFactory factory = getUIFactory();
			// Neck image uses the board host width (without LM overflow strip)
			float neckWidth = this.boardHost.getBounds().getWidth();
			if (neckWidth <= 0f) {
				neckWidth = this.control.getChildArea().getWidth();
			}
			float neckHeight = (this.stringSpacing * Math.max(this.strings.length - 1, 0)) + TOP_SPACING + BOTTOM_SPACING;

			this.fretBoard = factory.createImage(neckWidth, neckHeight);

			UIPainter painterBuffer = this.fretBoard.createPainter();

			//fondo
			painterBuffer.setBackground(this.config.getColorBackground());
			painterBuffer.initPath(UIPainter.PATH_FILL);
			painterBuffer.addRectangle(0, 0, neckWidth, neckHeight);
			painterBuffer.closePath();


			// pinto las cegillas
			TGIconManager iconManager = TGIconManager.getInstance(this.context);
			UIImage fretImage = iconManager.getFretboardFret();
			UIImage firstFretImage = iconManager.getFretboardFirstFret();

			painterBuffer.drawImage(firstFretImage, 0, 0, firstFretImage.getWidth(), firstFretImage.getHeight(), this.frets[0] - 5,this.strings[0] - 5, firstFretImage.getWidth(),this.strings[this.strings.length - 1] );

			paintFretPoints(painterBuffer,0);
			for (int i = 1; i < this.frets.length; i++) {
				painterBuffer.drawImage(fretImage, 0, 0, fretImage.getWidth(), fretImage.getHeight(), this.frets[i], this.strings[0] - 5,fretImage.getWidth(),this.strings[this.strings.length - 1] );
				paintFretPoints(painterBuffer, i);
			}

			// pinto las cuerdas
			for (int i = 0; i < this.strings.length; i++) {
				painterBuffer.setForeground(this.config.getColorString());
				if(i > 2){
					painterBuffer.setLineWidth(2);
				}
				painterBuffer.initPath();
				painterBuffer.setAntialias(false);
				painterBuffer.moveTo(this.frets[0], this.strings[i]);
				painterBuffer.lineTo(this.frets[this.frets.length - 1], this.strings[i]);
				painterBuffer.closePath();
			}

			// pinto la escala
			paintScale(painterBuffer);

			painterBuffer.dispose();
		}
		painter.drawImage(this.fretBoard, offsetX, 0);
	}

	private void paintFretPoints(UIPainter painter, int fretIndex) {
		painter.setBackground(this.config.getColorFretPoint());
		if ((fretIndex + 1) < this.frets.length) {
			int fret = ((fretIndex + 1) % 12);
			painter.setLineWidth(10);
			if (fret == 0) {
				int size = getOvalSize();
				int x = this.frets[fretIndex] + ((this.frets[fretIndex + 1] - this.frets[fretIndex]) / 2);
				int y1 = this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2) - this.stringSpacing;
				int y2 = this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2) + this.stringSpacing;
				painter.initPath(UIPainter.PATH_FILL);
				painter.addCircle(x, y1, size);
				painter.addCircle(x, y2, size);
				painter.closePath();
			} else if (fret == 3 || fret == 5 || fret == 7 || fret == 9) {
				int size = getOvalSize();
				int x = this.frets[fretIndex] + ((this.frets[fretIndex + 1] - this.frets[fretIndex]) / 2);
				int y = this.strings[0] + ((this.strings[this.strings.length - 1] - this.strings[0]) / 2);
				painter.initPath(UIPainter.PATH_FILL);
				painter.addCircle(x, y, size);
				painter.closePath();
			}
			painter.setLineWidth(1);
		}
	}

	private void paintScale(UIPainter painter) {
		TGTrack track = getTrack();
		TGScale scale = TuxGuitar.getInstance().getScaleManager().getScale();
		int keySignature = TGMusicKeyUtils.getKeySignature(scale);
		int tonicKey = scale.getKey();

		for (int i = 0; i < this.strings.length; i++) {
			TGString string = track.getString(i + 1);
			for (int j = 0; j < this.frets.length; j++) {

				int noteValue = string.getValue() + j;
				if(scale.getNote(noteValue)){
					int x = this.frets[j];
					if(j > 0){
						x -= ((x - this.frets[j - 1]) / 2);
					}
					int y = this.strings[i];

					boolean isTonic = ((noteValue % 12) == tonicKey);
					UIColor ovalColor = isTonic ? this.config.getColorTonic() : this.config.getColorScale();

					if( (this.config.getStyle() & TGFretBoardConfig.DISPLAY_TEXT_SCALE) != 0 ){
						String noteName = TGMusicKeyUtils.noteName(noteValue, keySignature);
						UIColor textColor = isTonic ? this.config.getColorTonicText() : this.config.getColorScaleText();
						paintKeyText(painter, textColor, ovalColor, x, y, noteName);
					}
					else{
						paintKeyOval(painter, ovalColor, x, y);
					}
				}
			}
		}

		painter.setForeground(this.config.getColorBackground());
	}

	private void paintNotes(UIPainter painter) {
		if(this.beat != null){
			TGTrack track = getTrack();
			int keySignature = this.beat.getMeasure().getKeySignature();

			for(int v = 0; v < this.beat.countVoices(); v ++){
				TGVoice voice = this.beat.getVoice( v );
				Iterator<TGNote> it = voice.getNotes().iterator();
				while (it.hasNext()) {
					TGNote note = it.next();
					int fretIndex = note.getValue();
					int stringIndex = note.getString() - 1;
					if (fretIndex >= 0 && fretIndex < this.frets.length && stringIndex >= 0 && stringIndex < this.strings.length) {
						int x = this.toNotesLayerX(this.frets[fretIndex]);
						if (fretIndex > 0) {
							x = this.toNotesLayerX(this.frets[fretIndex] - ((this.frets[fretIndex] - this.frets[fretIndex - 1]) / 2));
						}
						int y = this.strings[stringIndex];

						if( (this.config.getStyle() & TGFretBoardConfig.DISPLAY_TEXT_NOTE) != 0 ){
							int realValue = track.getString(note.getString()).getValue() + note.getValue();
							paintKeyText(painter,this.config.getColorNoteText(), this.config.getColorNote(), x, y, TGMusicKeyUtils.noteName(realValue, keySignature, note.isAltEnharmonic()));
						}
						else{
							paintKeyOval(painter,this.config.getColorNote(), x, y);
						}
					}
				}
			}
			painter.setLineWidth(1);
		}
	}

	private void paintKeyOval(UIPainter painter, UIColor background,int x, int y) {
		this.paintKeyOval(painter, background, x, y, this.getOvalSize());
	}
	private void paintKeyOval(UIPainter painter, UIColor background,int x, int y, int ovalSize) {
		painter.setBackground(background);
		painter.initPath(UIPainter.PATH_FILL);
		painter.moveTo(x, y);
		painter.addCircle(x, y, ovalSize);
		painter.closePath();
	}

	private void paintLearningNote(UIPainter painter, UIColor background, int x, int y, int width, int height) {
		painter.setBackground(background);
		painter.initPath(UIPainter.PATH_FILL);
		float w = width;
		float h = height;
		float radius = Math.min(w, h) / 2f;
		float left = this.getLearningNoteLeft(x, w);
		painter.addRoundedRectangle(left, y - (h / 2f), w, h, radius);
		painter.closePath();
	}

	private void paintKeyText(UIPainter painter, UIColor foreground, UIColor background, int x, int y, String text) {
		if (!getTrack().isPercussion()) {
			painter.setBackground(background);
			painter.setForeground(foreground);
			painter.setFont(this.config.getFont());

			float fmWidth = painter.getFMWidth(text);
			float fmHeight = painter.getFMHeight();
			int ovalSize = (int)Math.max(fmWidth, fmHeight) + this.stringSpacing/10;
			ovalSize = Math.min(ovalSize, this.getMaxOvalSize());
			this.paintKeyOval(painter, background, x, y, ovalSize);
			painter.drawString(text, x - (fmWidth / 2f),y + painter.getFMMiddleLine());
		}
	}

	private void paintLearningNoteText(UIPainter painter, UIColor foreground, UIColor background, int x, int y, String text, int width) {
		painter.setBackground(background);
		painter.setForeground(foreground);
		painter.setFont(this.config.getFont());

		float fmWidth = painter.getFMWidth(text);
		float fmHeight = painter.getFMHeight();
		int height = (int)Math.max(fmWidth, fmHeight) + this.stringSpacing/10;
		height = Math.min(height, this.getMaxOvalSize());
		width = Math.max(width, 2);
		paintLearningNote(painter, background, x, y, width, height);
		float left = this.getLearningNoteLeft(x, width);
		painter.drawString(text, left + (width / 2f) - (fmWidth / 2f), y + painter.getFMMiddleLine());
	}

	private void paintSprites(UIPainter painter) {
		this.updateEditor();
		if (this.frets.length > 0 && this.strings.length > 0) {
			UIRectangle notesBounds = this.notesComposite.getBounds();
			painter.setBackground(this.config.getColorBackground());
			painter.initPath(UIPainter.PATH_FILL);
			painter.addRectangle(0, 0, Math.max(notesBounds.getWidth(), 1f), Math.max(notesBounds.getHeight(), 1f));
			painter.closePath();
			paintFretBoard(painter, this.notesLayerOffsetX);
			if (isLearningModeEnabled()) {
				this.updateLearningLayer();
				this.paintLearningSprites(painter);
			} else {
				paintNotes(painter);
			}
		}
	}

	protected void hit(float x, float y) {
		int fretIndex = getFretIndex(x);
		int stringIndex = getStringIndex(y);
		int stringNumber = (stringIndex + 1);

		this.selectString(stringNumber);
		if(!this.removeNote(fretIndex, stringNumber)) {
			this.addNote(fretIndex, stringNumber);
		}
	}

	private void selectString(int number) {
		TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGMoveToAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, getTrack().getString(number));
		tgActionProcessor.process();
	}

	private int getStringIndex(float y) {
		int index = -1;
		for (int i = 0; i < this.strings.length; i++) {
			if (index < 0) {
				index = i;
			} else {
				float distanceY = Math.abs(y - this.strings[index]);
				float currDistanceY = Math.abs(y - this.strings[i]);
				if( currDistanceY < distanceY) {
					index = i;
				}
			}
		}
		return index;
	}

	private int getFretIndex(float x) {
		int length = this.frets.length;
		if ((x - 10) <= this.frets[0] && this.frets[0] < this.frets[length - 1]) {
			return 0;
		}
		if ((x + 10) >= this.frets[0] && this.frets[0] > this.frets[length - 1]) {
			return 0;
		}

		for (int i = 0; i < length; i++) {
			if ((i + 1) < length) {
				if (x > this.frets[i] && x <= this.frets[i + 1] || x > this.frets[i + 1] && x <= this.frets[i]) {
					return i + 1;
				}
			}
		}
		return length - 1;
	}

	private boolean removeNote(int fret, int string) {
		if(this.beat != null){
			for(int v = 0; v < this.beat.countVoices(); v ++){
				TGVoice voice = this.beat.getVoice( v );
				Iterator<TGNote> it = voice.getNotes().iterator();
				while (it.hasNext()) {
					TGNote note = it.next();
					if( note.getValue() == fret && note.getString() == string ) {
						TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGDeleteNoteAction.NAME);
						tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_NOTE, note);
						tgActionProcessor.process();

						return true;
					}
				}
			}
		}
		return false;
	}

	private TGTrack getTrack() {
		if( this.beat != null ){
			TGMeasure measure = this.beat.getMeasure();
			if( measure != null ){
				TGTrack track = measure.getTrack();
				if( track != null ){
					return track;
				}
			}
		}
		return TuxGuitar.getInstance().getTablatureEditor().getTablature().getCaret().getTrack();
	}

	private int getStringCount() {
		TGTrack track = getTrack();
		if( track != null ){
			return track.stringCount();
		}
		return 0;
	}

	private int getOvalSize(){
		return ((this.stringSpacing / 2) + (this.stringSpacing / 10));
	}

	private int getMaxOvalSize() {
		return (this.stringSpacing - this.stringSpacing/10);
	}

	private void addNote(int fret, int string) {
		TGActionProcessor tgActionProcessor = new TGActionProcessor(this.context, TGChangeNoteAction.NAME);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_FRET, fret);
		tgActionProcessor.setAttribute(TGDocumentContextAttributes.ATTRIBUTE_STRING, getTrack().getString(string));
		tgActionProcessor.process();
	}

	protected void updateDirection( int direction ){
		this.config.saveDirection( this.getDirection(direction) );
		this.initFrets(FRET_FROM_X);
		this.setChanges(true);
		this.resetLearningLayer();
		this.layoutNotesOverlay();
		this.notesComposite.redraw();
	}

	private void initLearningMode() {
		boolean enabled = TuxGuitar.getInstance().getConfig().getBooleanValue(TGConfigKeys.LEMO_LM_ENABLED, false);
		if (enabled) {
			this.learningMode.setBgColor(getUIFactory().createColor(0, 255, 0));
			this.syncLearningCountIn();
		} else {
			this.learningMode.setBgColor(null);
		}
	}

	private void toggleLearningMode() {
		boolean current = TuxGuitar.getInstance().getConfig().getBooleanValue(TGConfigKeys.LEMO_LM_ENABLED, false);
		boolean next = !current;
		TuxGuitar.getInstance().getConfig().setValue(TGConfigKeys.LEMO_LM_ENABLED, Boolean.toString(next));
		
		if (next) {
			this.learningMode.setBgColor(getUIFactory().createColor(0, 255, 0));
		} else {
			this.learningMode.setBgColor(null);
		}
		this.resetLearningLayer();
		this.layoutNotesOverlay();
		if (next) {
			this.syncLearningCountIn();
		}
		this.notesComposite.redraw();
	}

	public boolean isLearningModeEnabled() {
		return TuxGuitar.getInstance().getConfig().getBooleanValue(TGConfigKeys.LEMO_LM_ENABLED, false);
	}

	private void resetLearningLayer() {
		if (this.learningSprites != null) {
			this.learningSprites.clear();
		}
		this.learningLastPlayPrecise = Long.MIN_VALUE;
		this.learningLastStampedPrecise = Long.MIN_VALUE;
		this.learningTrackNumber = -1;
		this.learningCountInActive = false;
		this.learningLeadInPrecise = 0L;
		this.learningCountInTargetPrecise = 0L;
		this.learningCountInWallStartMs = 0L;
		this.learningCountInTickMs = 0L;
		this.learningBeatPrecise = 0L;
	}

	/**
	 * Real play time from the transport / caret (no visual offset).
	 * This is the MIDI / staff cursor time.
	 */
	private long getRealPlayPreciseTime() {
		MidiPlayer player = MidiPlayer.getInstance(this.context);
		if (player.isRunning()) {
			return TGDuration.toPreciseTime(TGTransport.getInstance(this.context).getCache().getPlayStart());
		}
		if (this.beat != null) {
			return this.beatPreciseStart(this.beat);
		}
		return 0L;
	}

	/**
	 * Visual play time used for sprite positioning.
	 * During count-in the clock runs from (target - leadIn) to target so the
	 * first note travels from the last fret to fret 0. After count-in it
	 * equals the real MIDI time: a note is on fret 0 exactly when it sounds.
	 */
	private long getPlayPreciseTime() {
		if (this.learningCountInActive && this.learningLeadInPrecise > 0L
				&& this.learningCountInTickMs > 0L && this.learningBeatPrecise > 0L) {
			long elapsedMs = System.currentTimeMillis() - this.learningCountInWallStartMs;
			if (elapsedMs < 0L) {
				elapsedMs = 0L;
			}
			// Advance song time at the same rate as MidiPlayerCountDown:
			// one metronome tick (tickMs) = one denominator beat (beatPrecise).
			long advanced = Math.round((double) elapsedMs
					* (double) this.learningBeatPrecise
					/ (double) this.learningCountInTickMs);
			long visual = this.learningCountInTargetPrecise - this.learningLeadInPrecise + advanced;
			long min = this.learningCountInTargetPrecise - this.learningLeadInPrecise;
			if (visual < min) {
				visual = min;
			}
			if (visual > this.learningCountInTargetPrecise) {
				visual = this.learningCountInTargetPrecise;
			}
			return visual;
		}
		return this.getRealPlayPreciseTime();
	}

	private TGMeasure resolveLearningMeasure() {
		if (this.beat != null && this.beat.getMeasure() != null) {
			return this.beat.getMeasure();
		}
		try {
			TGBeat caretBeat = TablatureEditor.getInstance(this.context).getTablature().getCaret().getSelectedBeat();
			if (caretBeat != null && caretBeat.getMeasure() != null) {
				return caretBeat.getMeasure();
			}
		} catch (Throwable ignored) {
			// ignore
		}
		TGTrack track = this.getTrack();
		if (track != null && track.countMeasures() > 0) {
			return track.getMeasure(0);
		}
		return null;
	}

	/**
	 * Milliseconds of one metronome tick, same formula as MidiPlayerCountDown.start().
	 */
	private long getCountInTickLengthMs() {
		int qpm = 120;
		long denomTime = TGDuration.QUARTER_TIME;
		TGMeasure measure = this.resolveLearningMeasure();
		if (measure != null) {
			if (measure.getTempo() != null) {
				qpm = measure.getTempo().getQuarterValue();
			}
			if (measure.getTimeSignature() != null
					&& measure.getTimeSignature().getDenominator() != null) {
				denomTime = measure.getTimeSignature().getDenominator().getTime();
			}
		}
		if (qpm <= 0) {
			qpm = 120;
		}
		if (denomTime <= 0L) {
			denomTime = TGDuration.QUARTER_TIME;
		}
		int percent = 100;
		try {
			MidiPlayer player = MidiPlayer.getInstance(this.context);
			if (player.getCountDown() != null && player.getCountDown().getTempoPercent() > 0) {
				percent = player.getCountDown().getTempoPercent();
			} else {
				percent = player.getMode().getCurrentPercent();
			}
		} catch (Throwable ignored) {
			percent = 100;
		}
		if (percent <= 0) {
			percent = 100;
		}
		int tgTempo = (qpm * percent) / 100;
		if (tgTempo <= 0) {
			tgTempo = 120;
		}
		return Math.max(1L, (long) (1000.00 * (60.00 / tgTempo * denomTime) / TGDuration.QUARTER_TIME));
	}

	private void freezeLearningCountInClock() {
		this.learningBeatPrecise = this.getBeatPreciseDuration();
		this.learningCountInTickMs = this.getCountInTickLengthMs();
		this.learningLeadInPrecise = this.computeLearningLeadInPrecise();
	}

	private long beatPreciseStart(TGBeat beat) {
		Long preciseStart = beat.getPreciseStart();
		if (preciseStart != null && preciseStart.longValue() >= 0L) {
			return preciseStart.longValue();
		}
		return TGDuration.toPreciseTime(beat.getStart());
	}

	private float getLearningUnitWidth() {
		return Math.max(this.getOvalSize(), 8);
	}

	private float getPixelsPerPreciseTime() {
		long unitPrecise = TGDuration.WHOLE_PRECISE_DURATION / TGDuration.SIXTEENTH;
		if (unitPrecise <= 0L) {
			return 0f;
		}
		return this.getLearningUnitWidth() / (float) unitPrecise;
	}

	private int getLearningAnchorX() {
		if (this.frets.length == 0) {
			return 0;
		}
		// Hit line = fret 0 (nut). Notes spawn at the last fret and scroll here.
		return this.toNotesLayerX(this.frets[0]);
	}

	private int toLearningX(long notePreciseStart, long playPrecise) {
		float dir = this.isLeftHanded() ? -1f : 1f;
		return Math.round(this.getLearningAnchorX() + dir * (notePreciseStart - playPrecise) * this.getPixelsPerPreciseTime());
	}

	private int toLearningWidth(long preciseStart, long preciseDuration, long playPrecise) {
		int startX = this.toLearningX(preciseStart, playPrecise);
		int endX = this.toLearningX(preciseStart + preciseDuration, playPrecise);
		return Math.max(2, Math.abs(endX - startX));
	}

	private void updateLearningLayer() {
		this.syncLearningCountIn();
		this.updateLearningCountInState();

		long playPrecise = this.getPlayPreciseTime();
		long lookAhead = this.learningLeadInPrecise;
		if (lookAhead <= 0L) {
			lookAhead = this.computeLearningLeadInPrecise();
			this.learningLeadInPrecise = lookAhead;
		}
		long horizon = playPrecise + lookAhead;

		TGTrack track = this.getTrack();
		int trackNumber = (track != null ? track.getNumber() : -1);

		boolean reset = this.learningLastPlayPrecise == Long.MIN_VALUE
			|| trackNumber != this.learningTrackNumber
			|| playPrecise < this.learningLastPlayPrecise
			|| (playPrecise - this.learningLastPlayPrecise) > Math.max(TGDuration.WHOLE_PRECISE_DURATION, lookAhead);

		if (reset) {
			this.learningSprites.clear();
			this.stampLearningNotesBetween(playPrecise - 1L, horizon);
			this.learningLastStampedPrecise = horizon;
		} else if (horizon > this.learningLastStampedPrecise) {
			this.stampLearningNotesBetween(this.learningLastStampedPrecise, horizon);
			this.learningLastStampedPrecise = horizon;
		}

		this.cullLearningSprites(playPrecise);
		this.learningLastPlayPrecise = playPrecise;
		this.learningTrackNumber = trackNumber;
	}

	/**
	 * Detect start / end of MidiPlayerCountDown and maintain the visual
	 * lead-in clock so that the first note reaches fret 0 when the
	 * sequencer actually begins.
	 */
	private void updateLearningCountInState() {
		if (!isLearningModeEnabled()) {
			this.learningCountInActive = false;
			return;
		}
		MidiPlayer player = MidiPlayer.getInstance(this.context);
		boolean countInRunning = player.getCountDown() != null
				&& player.getCountDown().isEnabled()
				&& player.getCountDown().isRunning();

		if (countInRunning && !this.learningCountInActive) {
			this.freezeLearningCountInClock();
			long target = this.getRealPlayPreciseTime();
			if (this.beat != null) {
				target = this.beatPreciseStart(this.beat);
			}
			this.learningCountInTargetPrecise = target;
			this.learningCountInWallStartMs = System.currentTimeMillis();
			this.learningCountInActive = true;
			this.learningLastPlayPrecise = Long.MIN_VALUE;
		} else if (!countInRunning && this.learningCountInActive) {
			this.learningCountInActive = false;
			this.learningLastPlayPrecise = Long.MIN_VALUE;
		}
	}

	/**
	 * Enable the player count-in and set its tick count to the neck-travel
	 * duration (beats needed for a note to go from last fret to fret 0).
	 */
	private void syncLearningCountIn() {
		if (!isLearningModeEnabled()) {
			return;
		}
		MidiPlayer player = MidiPlayer.getInstance(this.context);
		if (player.getCountDown() != null && player.getCountDown().isRunning()) {
			return;
		}
		this.freezeLearningCountInClock();
		int ticks = this.computeLearningCountInTicks(this.computeLearningTravelPrecise());
		player.getCountDown().setEnabled(true);
		player.getCountDown().setTickCount(ticks);
	}

	/**
	 * Precise-time distance from last fret to fret 0 at the current scroll speed.
	 */
	private long computeLearningTravelPrecise() {
		if (this.frets.length < 2) {
			return TGDuration.WHOLE_PRECISE_DURATION;
		}
		float distance = Math.abs(this.frets[this.frets.length - 1] - this.frets[0]);
		float pxPer = this.getPixelsPerPreciseTime();
		if (pxPer <= 0f || distance <= 0f) {
			return TGDuration.WHOLE_PRECISE_DURATION;
		}
		long travel = Math.round(distance / pxPer);
		long min = TGDuration.WHOLE_PRECISE_DURATION / TGDuration.QUARTER;
		return Math.max(min, travel);
	}

	private long getBeatPreciseDuration() {
		// One metronome tick = denominator duration, converted to precise time
		// (same unit MidiPlayerCountDown uses via denominator.getTime()).
		TGMeasure measure = this.resolveLearningMeasure();
		if (measure != null && measure.getTimeSignature() != null
				&& measure.getTimeSignature().getDenominator() != null) {
			long denomTime = measure.getTimeSignature().getDenominator().getTime();
			if (denomTime > 0L && TGDuration.WHOLE_PRECISE_DURATION > 0L && TGDuration.QUARTER_TIME > 0L) {
				return Math.max(1L, denomTime * TGDuration.WHOLE_PRECISE_DURATION
						/ (TGDuration.QUARTER_TIME * TGDuration.QUARTER));
			}
		}
		long quarter = TGDuration.WHOLE_PRECISE_DURATION / TGDuration.QUARTER;
		return Math.max(1L, quarter);
	}

	private int computeLearningCountInTicks(long travelPrecise) {
		long beatPrecise = this.getBeatPreciseDuration();
		if (beatPrecise <= 0L) {
			beatPrecise = 1L;
		}
		int ticks = (int) Math.ceil((double) Math.max(1L, travelPrecise) / (double) beatPrecise);
		return Math.max(1, ticks);
	}

	/**
	 * Count-in / look-ahead duration in song precise time: an integer number
	 * of beats covering at least the last-fret -> fret 0 travel.
	 */
	private long computeLearningLeadInPrecise() {
		long travel = this.computeLearningTravelPrecise();
		long beatPrecise = this.getBeatPreciseDuration();
		int ticks = this.computeLearningCountInTicks(travel);
		return ticks * beatPrecise;
	}

	private void stampLearningNotesBetween(long fromExclusive, long toInclusive) {
		TGTrack track = this.getTrack();
		if (track == null) {
			return;
		}
		for (int i = 0; i < track.countMeasures(); i++) {
			TGMeasure measure = track.getMeasure(i);
			long measureStart = measure.getPreciseStart();
			long measureEnd = measureStart + measure.getPreciseLength();
			if (measureEnd <= fromExclusive || measureStart > toInclusive) {
				continue;
			}
			List<TGBeat> beats = measure.getBeats();
			for (int b = 0; b < beats.size(); b++) {
				TGBeat beat = beats.get(b);
				long start = this.beatPreciseStart(beat);
				if (start > fromExclusive && start <= toInclusive) {
					this.stampLearningBeat(beat);
				}
			}
		}
	}

	private void stampLearningBeat(TGBeat beat) {
		if (beat == null || beat.getMeasure() == null) {
			return;
		}
		TGTrack track = beat.getMeasure().getTrack();
		if (track == null) {
			track = this.getTrack();
		}
		if (track == null) {
			return;
		}
		int keySignature = beat.getMeasure().getKeySignature();
		boolean percussion = track.isPercussion();
		for (int v = 0; v < beat.countVoices(); v++) {
			TGVoice voice = beat.getVoice(v);
			if (voice == null || voice.isEmpty() || voice.isRestVoice()) {
				continue;
			}
			Iterator<TGNote> it = voice.getNotes().iterator();
			while (it.hasNext()) {
				this.stampLearningNote(it.next(), track, keySignature, percussion);
			}
		}
	}

	private void stampLearningNote(TGNote note, TGTrack track, int keySignature, boolean percussion) {
		TGNote chainStart = this.getTiedChainStart(note);
		if (chainStart.getVoice() == null || chainStart.getVoice().getBeat() == null) {
			return;
		}
		TGBeat startBeat = chainStart.getVoice().getBeat();
		long preciseStart = this.beatPreciseStart(startBeat);
		int fretIndex = chainStart.getValue();
		int stringIndex = chainStart.getString() - 1;
		if (fretIndex < 0 || fretIndex >= this.frets.length || stringIndex < 0 || stringIndex >= this.strings.length) {
			return;
		}
		if (this.isLearningSpriteStamped(preciseStart, stringIndex, fretIndex)) {
			return;
		}

		LearningSprite sprite = new LearningSprite();
		sprite.preciseStart = preciseStart;
		sprite.preciseDuration = this.getTiedPreciseDuration(chainStart);
		sprite.stringIndex = stringIndex;
		sprite.fret = fretIndex;
		sprite.keySignature = keySignature;
		sprite.altEnharmonic = chainStart.isAltEnharmonic();
		sprite.percussion = percussion;
		if (!percussion) {
			sprite.midiNote = track.getString(chainStart.getString()).getValue() + chainStart.getValue();
		}
		this.learningSprites.add(sprite);
	}

	private boolean isLearningSpriteStamped(long preciseStart, int stringIndex, int fret) {
		for (int i = 0; i < this.learningSprites.size(); i++) {
			LearningSprite sprite = this.learningSprites.get(i);
			if (sprite.preciseStart == preciseStart && sprite.stringIndex == stringIndex && sprite.fret == fret) {
				return true;
			}
		}
		return false;
	}

	private void cullLearningSprites(long playPrecise) {
		if (this.notesComposite == null) {
			return;
		}
		float canvasWidth = this.notesComposite.getBounds().getWidth();
		if (canvasWidth <= 0f) {
			return;
		}
		Iterator<LearningSprite> it = this.learningSprites.iterator();
		while (it.hasNext()) {
			LearningSprite sprite = it.next();
			int x = this.toLearningX(sprite.preciseStart, playPrecise);
			int width = this.toLearningWidth(sprite.preciseStart, sprite.preciseDuration, playPrecise);
			float left = this.getLearningNoteLeft(x, width);
			if (left + width < 0f || left > canvasWidth) {
				it.remove();
			}
		}
	}

	private void paintLearningSprites(UIPainter painter) {
		long playPrecise = this.getPlayPreciseTime();
		for (int i = 0; i < this.learningSprites.size(); i++) {
			LearningSprite sprite = this.learningSprites.get(i);
			if (sprite.stringIndex < 0 || sprite.stringIndex >= this.strings.length) {
				continue;
			}
			int x = this.toLearningX(sprite.preciseStart, playPrecise);
			int width = this.toLearningWidth(sprite.preciseStart, sprite.preciseDuration, playPrecise);
			int y = this.strings[sprite.stringIndex];
			if (sprite.percussion) {
				paintLearningNote(painter, this.config.getColorNote(), x, y, width, this.getOvalSize());
			} else {
				UIColor noteColor = this.config.getLearningNoteColor(sprite.midiNote, sprite.keySignature, sprite.altEnharmonic);
				UIColor textColor = this.config.getLearningNoteTextColor(sprite.midiNote, sprite.keySignature, sprite.altEnharmonic);
				this.paintLearningNoteText(painter, textColor, noteColor, x, y, String.valueOf(sprite.fret), width);
			}
		}
		painter.setLineWidth(1);
	}


	private boolean hasTechniqueEffect(TGNote note) {
		return note.getEffect().isBend()
			|| note.getEffect().isTremoloBar()
			|| note.getEffect().isSlide()
			|| note.getEffect().isHammer()
			|| note.getEffect().isTrill()
			|| note.getEffect().isTremoloPicking()
			|| note.getEffect().isDeadNote()
			|| note.getEffect().isGrace()
			|| note.getEffect().isTapping();
	}

	private TGNote getTiedChainStart(TGNote note) {
		TGNote start = note;
		while (start.isTiedNote() && !hasTechniqueEffect(start)) {
			TGNote previous = TuxGuitar.getInstance().getSongManager().getTrackManager().getPreviousNoteForTie(start);
			if (previous == null || previous.getString() != start.getString() || previous.getValue() != start.getValue()) {
				break;
			}
			if (hasTechniqueEffect(previous)) {
				break;
			}
			start = previous;
		}
		return start;
	}

	private long getTiedPreciseDuration(TGNote note) {
		TGNote current = getTiedChainStart(note);
		long total = current.getVoice().getDuration().getPreciseTime();
		while (true) {
			TGNote next = TuxGuitar.getInstance().getSongManager().getTrackManager().getNextTiedNote(current);
			if (next == null || hasTechniqueEffect(next)) {
				break;
			}
			total += next.getVoice().getDuration().getPreciseTime();
			current = next;
		}
		return total;
	}

	public boolean hasChanges(){
		return this.changes;
	}

	public void setChanges(boolean changes){
		this.changes = changes;
	}

	public void setExternalBeat(TGBeat externalBeat){
		this.externalBeat = externalBeat;
	}

	public TGBeat getExternalBeat(){
		return this.externalBeat;
	}

	public void redraw() {
		if(!this.isDisposed()){
			this.control.redraw();
			this.notesComposite.redraw();
			this.loadDurationImage(false);
		}
	}

	public void redrawPlayingMode(){
		if(!this.isDisposed()){
			this.notesComposite.redraw();
		}
	}

	public void setVisible(boolean visible) {
		this.control.setVisible(visible);
	}

	public boolean isVisible() {
		return (this.control.isVisible());
	}

	public boolean isDisposed() {
		return (this.control.isDisposed());
	}

	public void dispose(){
		this.control.dispose();
		this.disposeFretBoardImage();
		this.config.dispose();
	}

	public void loadProperties(){
		int selection = this.handSelector.getSelectedItem().getValue();
		this.handSelector.removeItems();
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.right-mode"), TGFretBoardConfig.DIRECTION_RIGHT));
		this.handSelector.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.left-mode"), TGFretBoardConfig.DIRECTION_LEFT));
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, selection));

		this.smaller.setToolTipText(TuxGuitar.getProperty("fretboard.smaller"));
		this.bigger.setToolTipText(TuxGuitar.getProperty("fretboard.bigger"));
		this.settings.setToolTipText(TuxGuitar.getProperty("settings"));
		this.learningMode.setToolTipText(TuxGuitar.getProperty("learning.mode"));
		this.scale.setText(TuxGuitar.getProperty("scale"));
		this.loadScaleName();
		this.setChanges(true);
		this.control.layout();
	}

	public void loadIcons(){
		this.goLeft.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.ARROW_LEFT));
		this.goRight.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.ARROW_RIGHT));
		this.decrement.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.ARROW_DOWN));
		this.increment.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.ARROW_UP));
		this.settings.setImage(TuxGuitar.getInstance().getIconManager().getImageByName(TGIconManager.SETTINGS));
		this.smaller.setImage(TuxGuitar.getInstance().getIconManager().getFretboardSmaller());
		this.bigger.setImage(TuxGuitar.getInstance().getIconManager().getFretboardBigger());
		this.loadDurationImage(true);
		this.control.layout();
		this.layout(this.control.getChildArea().getWidth());
	}

	public void loadScale(){
		this.loadScaleName();
		this.setChanges(true);
		this.control.layout();
	}

	public int getWidth(){
		return this.frets[this.frets.length - 1];
	}

	public void computePackedSize() {
		this.control.getLayout().set(this.boardHost, UITableLayout.PACKED_HEIGHT, Float.valueOf((this.stringSpacing * (this.strings.length - 1)) + TOP_SPACING + BOTTOM_SPACING));
		this.control.computePackedSize(null, null);
	}

	public void layout(float width){
		this.disposeFretBoardImage();
		this.calculateFretSpacing(width);
		this.initFrets(FRET_FROM_X);
		this.initStrings(getStringCount());
		this.layoutNotesOverlay();
		if (isLearningModeEnabled()) {
			this.syncLearningCountIn();
		}
		this.setChanges(false);
	}

	private void layoutNotesOverlay() {
		if (this.boardHost != null && !this.boardHost.isDisposed()) {
			this.boardHost.layout();
		}
	}

	private boolean isLeftHanded() {
		return this.getDirection(this.config.getDirection()) == TGFretBoardConfig.DIRECTION_LEFT;
	}

	private float getLearningEndMargin(float width) {
		if (!isLearningModeEnabled() || width <= 0f) {
			return 0f;
		}
		int unit = Math.max(this.getOvalSize(), 8);
		float margin = unit * LEARNING_MARGIN_UNITS;
		float max = width * LEARNING_MARGIN_MAX_RATIO;
		float min = unit * 4f;
		if (min > max) {
			return max;
		}
		return Math.max(min, Math.min(margin, max));
	}

	private float getLearningNoteLeft(float anchorX, float width) {
		return this.isLeftHanded() ? (anchorX - width) : anchorX;
	}

	private int toNotesLayerX(int neckX) {
		return Math.round(neckX + this.notesLayerOffsetX);
	}

	private class OverlayLayout extends UIAbstractLayout {

		public UISize getComputedPackedSize(UILayoutContainer container) {
			UISize size = new UISize();
			for (UIControl child : container.getChildren()) {
				UISize packed = this.getPreferredControlSize(child);
				if (packed.getWidth() > size.getWidth()) {
					size.setWidth(packed.getWidth());
				}
				if (packed.getHeight() > size.getHeight()) {
					size.setHeight(packed.getHeight());
				}
			}
			return size;
		}

		public void setBounds(UILayoutContainer container, UIRectangle bounds) {
			float overflow = TGFretBoard.this.getLearningEndMargin(bounds.getWidth());
			if (TGFretBoard.this.isLeftHanded()) {
				TGFretBoard.this.notesLayerOffsetX = overflow;
			} else {
				TGFretBoard.this.notesLayerOffsetX = 0f;
			}

			UIRectangle neckBounds = new UIRectangle(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
			UIRectangle notesBounds = new UIRectangle(
				bounds.getX() - TGFretBoard.this.notesLayerOffsetX,
				bounds.getY(),
				bounds.getWidth() + overflow,
				bounds.getHeight()
			);

			if (TGFretBoard.this.fretBoardComposite != null) {
				TGFretBoard.this.fretBoardComposite.setBounds(neckBounds);
			}
			if (TGFretBoard.this.notesComposite != null) {
				TGFretBoard.this.notesComposite.setBounds(notesBounds);
			}
		}
	}

	public void configure(){
		this.config.configure(TGWindow.getInstance(this.context).getWindow(), getTrack().isPercussion());
	}

	public void reloadFromConfig(){
		this.handSelector.setSelectedItem(new UISelectItem<Integer>(null, this.getDirection(this.config.getDirection())));
		this.setChanges(true);
		this.redraw();
	}

	public UIPanel getControl(){
		return this.control;
	}

	public UICanvas getFretBoardComposite(){
		// Visible interaction surface (notes / composite layer)
		return this.notesComposite;
	}

	public UIFactory getUIFactory() {
		return TGApplication.getInstance(this.context).getFactory();
	}

	private class TGFretBoardMouseListener implements UIMouseUpListener {

		private TGContext context;

		public TGFretBoardMouseListener(TGContext context){
			this.context = context;
		}

		public void onMouseUp(final UIMouseEvent event) {
			TGFretBoard.this.notesComposite.setFocus();
			if( event.getButton() == 1 ) {
				if(!MidiPlayer.getInstance(this.context).isRunning()) {
					TGEditorManager.getInstance(this.context).asyncRunLocked(new Runnable() {
						public void run() {
							if( getExternalBeat() == null ){
								hit(event.getPosition().getX() - TGFretBoard.this.notesLayerOffsetX, event.getPosition().getY());
							}else{
								setExternalBeat( null );
								TuxGuitar.getInstance().updateCache(true);
							}
						}
					});
				}
			}else{
				new TGActionProcessor(TGFretBoard.this.context, TGGoRightAction.NAME).process();
			}
		}
	}

	private static class LearningSprite {
		long preciseStart;
		long preciseDuration;
		int stringIndex;
		int fret;
		int midiNote;
		int keySignature;
		boolean altEnharmonic;
		boolean percussion;
	}

	private class TGNotesPainterHandle implements TGBufferedPainterHandle {

		public void paintControl(UIPainter painter) {
			TGFretBoard.this.paintSprites(painter);
		}

		public UICanvas getPaintableControl() {
			return TGFretBoard.this.notesComposite;
		}
	}
}
