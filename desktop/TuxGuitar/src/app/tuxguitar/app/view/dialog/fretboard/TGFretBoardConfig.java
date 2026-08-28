package app.tuxguitar.app.view.dialog.fretboard;

import app.tuxguitar.app.TuxGuitar;
import app.tuxguitar.app.system.config.TGConfigDefaults;
import app.tuxguitar.app.system.config.TGConfigKeys;
import app.tuxguitar.app.system.config.TGConfigManager;
import app.tuxguitar.app.ui.TGApplication;
import app.tuxguitar.app.view.util.TGDialogUtil;
import app.tuxguitar.ui.UIFactory;
import app.tuxguitar.ui.chooser.UIColorChooser;
import app.tuxguitar.ui.chooser.UIColorChooserHandler;
import app.tuxguitar.ui.chooser.UIFontChooser;
import app.tuxguitar.ui.chooser.UIFontChooserHandler;
import app.tuxguitar.ui.event.UIDisposeEvent;
import app.tuxguitar.ui.event.UIDisposeListener;
import app.tuxguitar.ui.event.UISelectionEvent;
import app.tuxguitar.ui.event.UISelectionListener;
import app.tuxguitar.ui.layout.UITableLayout;
import app.tuxguitar.ui.resource.UIColor;
import app.tuxguitar.ui.resource.UIColorModel;
import app.tuxguitar.ui.resource.UIFont;
import app.tuxguitar.ui.resource.UIFontModel;
import app.tuxguitar.ui.widget.UIButton;
import app.tuxguitar.ui.widget.UICheckBox;
import app.tuxguitar.ui.widget.UIContainer;
import app.tuxguitar.ui.widget.UIControl;
import app.tuxguitar.ui.widget.UIDropDownSelect;
import app.tuxguitar.ui.widget.UILabel;
import app.tuxguitar.ui.widget.UILayoutContainer;
import app.tuxguitar.ui.widget.UILegendPanel;
import app.tuxguitar.ui.widget.UIPanel;
import app.tuxguitar.ui.widget.UISelectItem;
import app.tuxguitar.ui.widget.UIWindow;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.TGMusicKeyUtils;
import app.tuxguitar.util.properties.TGProperties;

public class TGFretBoardConfig {

	private static final float MINIMUM_CONTROL_WIDTH = 180f;
	private static final float MINIMUM_BUTTON_WIDTH = 80;
	private static final float MINIMUM_BUTTON_HEIGHT = 25;

	public static final int DISPLAY_TEXT_NOTE = 0x01;
	public static final int DISPLAY_TEXT_SCALE = 0x02;
	public static final int DIRECTION_RIGHT = 0;
	public static final int DIRECTION_LEFT = 1;
	public static final int FRET_OCTAVE = 12;

	private static final int LEARNING_VARIANT_NATURAL = 0;
	private static final int LEARNING_VARIANT_SHARP = 1;
	private static final int LEARNING_VARIANT_FLAT = 2;
	private static final int LEARNING_VARIANT_COUNT = 3;

	// Boomwhackers / educational rainbow for naturals, interpolated chromatics
	private static final int[][] LEARNING_NOTE_RGB = new int[][] {
		{ 229,  57,  53 }, // C
		{ 244,  81,  30 }, // C# / Db
		{ 251, 140,   0 }, // D
		{ 249, 168,  37 }, // D# / Eb
		{ 253, 216,  53 }, // E
		{  67, 160,  71 }, // F
		{   0, 137, 123 }, // F# / Gb
		{   3, 155, 229 }, // G
		{  92, 107, 192 }, // G# / Ab
		{  57,  73, 171 }, // A
		{ 194,  24,  91 }, // A# / Bb
		{ 123,  31, 162 }  // B
	};

	// Octave contour: brown family below fret 12, gray family from 12 up.
	// Dark/light shade is chosen from the fill so the ring stays visible on every note color.
	private static final int[] BORDER_BROWN_DARK = { 72, 42, 24 };
	private static final int[] BORDER_BROWN_LIGHT = { 232, 204, 158 };
	private static final int[] BORDER_GRAY_DARK = { 48, 50, 54 };
	private static final int[] BORDER_GRAY_LIGHT = { 206, 208, 212 };

	private TGContext context;
	private int style;
	private int direction;
	private UIFont font;
	private UIColor colorBackground;
	private UIColor colorString;
	private UIColor colorFretPoint;
	private UIColor colorNote;
	private UIColor colorScale;
	private UIColor colorTonic;
	private UIColor colorNoteText;
	private UIColor colorScaleText;
	private UIColor colorTonicText;
	private UIColor colorBorderBrownDark;
	private UIColor colorBorderBrownLight;
	private UIColor colorBorderGrayDark;
	private UIColor colorBorderGrayLight;
	private UIColor[][] learningNoteColors;
	private UIColor[][] learningNoteTextColors;

	public TGFretBoardConfig(TGContext context){
		this.context = context;
	}

	public int getStyle() {
		return this.style;
	}

	public UIFont getFont() {
		return this.font;
	}

	public UIColor getColorBackground() {
		return this.colorBackground;
	}

	public UIColor getColorString() {
		return this.colorString;
	}

	public UIColor getColorFretPoint() {
		return this.colorFretPoint;
	}

	public UIColor getColorNote() {
		return this.colorNote;
	}

	public UIColor getColorScale() {
		return this.colorScale;
	}

	public UIColor getColorTonic() {
		return this.colorTonic;
	}

	public UIColor getColorNoteText() {
		return colorNoteText;
	}

	public UIColor getColorScaleText() {
		return colorScaleText;
	}

	public UIColor getColorTonicText() {
		return colorTonicText;
	}

	public UIColor getNoteBorderColor(int fret, UIColor fill) {
		boolean lightFill = isLightColor(fill);
		if (fret < FRET_OCTAVE) {
			return lightFill ? this.colorBorderBrownDark : this.colorBorderBrownLight;
		}
		return lightFill ? this.colorBorderGrayDark : this.colorBorderGrayLight;
	}

	public UIColor getLearningNoteColor(int midiNote, int keySignature, boolean altEnharmonic) {
		int pitch = positiveModulo(midiNote, LEARNING_NOTE_RGB.length);
		int variant = getLearningColorVariant(midiNote, keySignature, altEnharmonic);
		return this.learningNoteColors[pitch][variant];
	}

	public UIColor getLearningNoteTextColor(int midiNote, int keySignature, boolean altEnharmonic) {
		int pitch = positiveModulo(midiNote, LEARNING_NOTE_RGB.length);
		int variant = getLearningColorVariant(midiNote, keySignature, altEnharmonic);
		return this.learningNoteTextColors[pitch][variant];
	}

	public int getDirection(){
		return this.direction;
	}

	public UIFont createFont(UIFactory factory, UIFontModel fm) {
		return TGApplication.getInstance(this.context).getFactory().createFont(fm.getName(), fm.getHeight(), fm.isBold(), fm.isItalic());
	}

	public UIColor createColor(UIFactory factory, UIColorModel cm) {
		return TGApplication.getInstance(this.context).getFactory().createColor(cm.getRed(), cm.getGreen(), cm.getBlue());
	}

	public void load(){
		UIFactory factory = TGApplication.getInstance(this.context).getFactory();
		TGConfigManager config = TuxGuitar.getInstance().getConfig();
		this.style = config.getIntegerValue(TGConfigKeys.FRETBOARD_STYLE);
		this.direction = config.getIntegerValue(TGConfigKeys.FRETBOARD_DIRECTION, DIRECTION_RIGHT );
		this.font = createFont(factory, config.getFontModelConfigValue(TGConfigKeys.FRETBOARD_FONT));
		this.colorBackground = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_BACKGROUND));
		this.colorString = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_STRING));
		this.colorFretPoint = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_FRET_POINT));
		this.colorNote = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_NOTE));
		this.colorScale = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_SCALE));
		this.colorTonic = createColor(factory,config.getColorModelConfigValue(TGConfigKeys.FRETBOARD_COLOR_TONIC));
		this.colorNoteText = createColor(factory, this.colorForeground(this.colorNote));
		this.colorScaleText = createColor(factory, this.colorForeground(this.colorScale));
		this.colorTonicText = createColor(factory, this.colorForeground(this.colorTonic));
		this.colorBorderBrownDark = createColor(factory, toColorModel(BORDER_BROWN_DARK));
		this.colorBorderBrownLight = createColor(factory, toColorModel(BORDER_BROWN_LIGHT));
		this.colorBorderGrayDark = createColor(factory, toColorModel(BORDER_GRAY_DARK));
		this.colorBorderGrayLight = createColor(factory, toColorModel(BORDER_GRAY_LIGHT));
		this.loadLearningNoteColors(factory);
	}

	private UIColorModel colorForeground(UIColor colorBackground) {
		if (isLightColor(colorBackground)) {
			return new UIColorModel(0x00, 0x00, 0x00);
		}
		return new UIColorModel(0xff, 0xff, 0xff);
	}

	private static boolean isLightColor(UIColor color) {
		int brightness = color.getRed() + color.getGreen() + color.getBlue();
		return brightness > 3 * 0x80;
	}

	private static UIColorModel toColorModel(int[] rgb) {
		return new UIColorModel(rgb[0], rgb[1], rgb[2]);
	}

	private void loadLearningNoteColors(UIFactory factory) {
		this.learningNoteColors = new UIColor[LEARNING_NOTE_RGB.length][LEARNING_VARIANT_COUNT];
		this.learningNoteTextColors = new UIColor[LEARNING_NOTE_RGB.length][LEARNING_VARIANT_COUNT];
		for (int pitch = 0; pitch < LEARNING_NOTE_RGB.length; pitch++) {
			int[] base = LEARNING_NOTE_RGB[pitch];
			int[][] variants = new int[][] {
				adjustHsl(base[0], base[1], base[2], 1f, 1f),
				adjustHsl(base[0], base[1], base[2], 0.45f, 0.72f),
				adjustHsl(base[0], base[1], base[2], 1.1f, 1.25f)
			};
			for (int variant = 0; variant < LEARNING_VARIANT_COUNT; variant++) {
				UIColor background = createColor(factory, new UIColorModel(variants[variant][0], variants[variant][1], variants[variant][2]));
				this.learningNoteColors[pitch][variant] = background;
				this.learningNoteTextColors[pitch][variant] = createColor(factory, this.colorForeground(background));
			}
		}
	}

	private void disposeLearningNoteColors() {
		if (this.learningNoteColors != null) {
			for (int pitch = 0; pitch < this.learningNoteColors.length; pitch++) {
				for (int variant = 0; variant < this.learningNoteColors[pitch].length; variant++) {
					this.learningNoteColors[pitch][variant].dispose();
					this.learningNoteTextColors[pitch][variant].dispose();
				}
			}
			this.learningNoteColors = null;
			this.learningNoteTextColors = null;
		}
	}

	private int getLearningColorVariant(int midiNote, int keySignature, boolean altEnharmonic) {
		int ks = keySignature;
		if (altEnharmonic) {
			String normalName = TGMusicKeyUtils.noteName(midiNote, keySignature);
			if (normalName != null) {
				String sharpName = TGMusicKeyUtils.noteName(midiNote, 7);
				if (sharpName != null && !sharpName.equals(normalName)) {
					ks = 7;
				} else {
					ks = 14;
				}
			}
		}
		int alteration = TGMusicKeyUtils.noteAlteration(midiNote, ks);
		if (alteration == TGMusicKeyUtils.SHARP) {
			return LEARNING_VARIANT_SHARP;
		}
		if (alteration == TGMusicKeyUtils.FLAT) {
			return LEARNING_VARIANT_FLAT;
		}
		return LEARNING_VARIANT_NATURAL;
	}

	private static int positiveModulo(int value, int modulo) {
		int result = value % modulo;
		return result < 0 ? result + modulo : result;
	}

	private static int[] adjustHsl(int red, int green, int blue, float saturationScale, float lightnessScale) {
		float[] hsl = rgbToHsl(red, green, blue);
		float saturation = clamp(hsl[1] * saturationScale, 0f, 1f);
		float lightness = clamp(hsl[2] * lightnessScale, 0f, 0.88f);
		return hslToRgb(hsl[0], saturation, lightness);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int clampChannel(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static float[] rgbToHsl(int red, int green, int blue) {
		float r = red / 255f;
		float g = green / 255f;
		float b = blue / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float lightness = (max + min) / 2f;
		float saturation = 0f;
		float hue = 0f;
		if (max != min) {
			float delta = max - min;
			saturation = lightness > 0.5f ? delta / (2f - max - min) : delta / (max + min);
			if (max == r) {
				hue = (g - b) / delta + (g < b ? 6f : 0f);
			} else if (max == g) {
				hue = (b - r) / delta + 2f;
			} else {
				hue = (r - g) / delta + 4f;
			}
			hue *= 60f;
		}
		return new float[] { hue, saturation, lightness };
	}

	private static int[] hslToRgb(float hue, float saturation, float lightness) {
		float h = hue / 360f;
		float r;
		float g;
		float b;
		if (saturation == 0f) {
			r = g = b = lightness;
		} else {
			float q = lightness < 0.5f ? lightness * (1f + saturation) : lightness + saturation - lightness * saturation;
			float p = 2f * lightness - q;
			r = hueToRgb(p, q, h + 1f / 3f);
			g = hueToRgb(p, q, h);
			b = hueToRgb(p, q, h - 1f / 3f);
		}
		return new int[] {
			clampChannel(Math.round(r * 255f)),
			clampChannel(Math.round(g * 255f)),
			clampChannel(Math.round(b * 255f))
		};
	}

	private static float hueToRgb(float p, float q, float t) {
		float hue = t;
		if (hue < 0f) {
			hue += 1f;
		}
		if (hue > 1f) {
			hue -= 1f;
		}
		if (hue < 1f / 6f) {
			return p + (q - p) * 6f * hue;
		}
		if (hue < 1f / 2f) {
			return q;
		}
		if (hue < 2f / 3f) {
			return p + (q - p) * (2f / 3f - hue) * 6f;
		}
		return p;
	}

	public void defaults(){
		TGConfigManager config = TuxGuitar.getInstance().getConfig();
		TGProperties defaults = TGConfigDefaults.createDefaults();
		config.setValue(TGConfigKeys.FRETBOARD_STYLE,defaults.getValue(TGConfigKeys.FRETBOARD_STYLE));
		config.setValue(TGConfigKeys.FRETBOARD_DIRECTION,defaults.getValue(TGConfigKeys.FRETBOARD_DIRECTION));
		config.setValue(TGConfigKeys.FRETBOARD_FONT,defaults.getValue(TGConfigKeys.FRETBOARD_FONT));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_BACKGROUND,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_BACKGROUND));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_STRING,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_STRING));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_FRET_POINT,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_FRET_POINT));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_NOTE,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_NOTE));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_SCALE,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_SCALE));
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_TONIC,defaults.getValue(TGConfigKeys.FRETBOARD_COLOR_TONIC));
	}

	public void save(int style, int direction, UIFontModel fm, UIColorModel rgbBackground, UIColorModel rgbString, UIColorModel rgbFretPoint, UIColorModel rgbNote, UIColorModel rgbScale, UIColorModel rgbTonic){
		TGConfigManager config = TuxGuitar.getInstance().getConfig();
		config.setValue(TGConfigKeys.FRETBOARD_STYLE,style);
		config.setValue(TGConfigKeys.FRETBOARD_DIRECTION,direction);
		config.setValue(TGConfigKeys.FRETBOARD_FONT,fm);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_BACKGROUND,rgbBackground);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_STRING,rgbString);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_FRET_POINT,rgbFretPoint);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_NOTE,rgbNote);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_SCALE,rgbScale);
		config.setValue(TGConfigKeys.FRETBOARD_COLOR_TONIC,rgbTonic);
	}

	public void saveDirection( int direction ){
		TGConfigManager config = TuxGuitar.getInstance().getConfig();
		config.setValue(TGConfigKeys.FRETBOARD_DIRECTION,direction);

		this.direction = direction;
	}

	public void dispose(){
		this.font.dispose();
		this.colorBackground.dispose();
		this.colorString.dispose();
		this.colorFretPoint.dispose();
		this.colorNote.dispose();
		this.colorScale.dispose();
		this.colorTonic.dispose();
		this.colorNoteText.dispose();
		this.colorScaleText.dispose();
		this.colorTonicText.dispose();
		this.colorBorderBrownDark.dispose();
		this.colorBorderBrownLight.dispose();
		this.colorBorderGrayDark.dispose();
		this.colorBorderGrayLight.dispose();
		this.disposeLearningNoteColors();
	}

	public void configure(UIWindow parent, boolean isPercussion) {
		final UIFactory factory = getUIFactory();
		final UITableLayout windowLayout = new UITableLayout();
		final UIWindow window = factory.createWindow(parent, true, false);
		window.setLayout(windowLayout);
		window.setText(TuxGuitar.getProperty("fretboard.settings"));

		// ----------------------------------------------------------------------
		UITableLayout groupLayout = new UITableLayout();
		UILegendPanel group = factory.createLegendPanel(window);
		group.setLayout(groupLayout);
		group.setText(TuxGuitar.getProperty("fretboard.settings"));
		windowLayout.set(group, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		int groupRow = 0;

		final UIFontModel fontData = getFontChooser(window, group, TuxGuitar.getProperty("fretboard.font") + ":", this.font, ++groupRow);

		// Color
		final UIColorModel rgbBackground = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.background-color") + ":", this.colorBackground, ++groupRow);
		final UIColorModel rgbString = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.string-color") + ":", this.colorString, ++groupRow);
		final UIColorModel rgbFretPoint = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.fretpoint-color") + ":", this.colorFretPoint, ++groupRow);
		final UIColorModel rgbNote = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.note-color") + ":", this.colorNote, ++groupRow);
		final UIColorModel rgbScale = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.scale-note-color") + ":", this.colorScale, ++groupRow);
		final UIColorModel rgbTonic = getColorChooser(window, group, TuxGuitar.getProperty("fretboard.tonic-color") + ":", this.colorTonic, ++groupRow);


		UILabel directionLabel = factory.createLabel(group);
		directionLabel.setText(TuxGuitar.getProperty("fretboard.direction") + ":");
		groupLayout.set(directionLabel, ++groupRow, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, true, true);

		final UIDropDownSelect<Integer> directionCombo = factory.createDropDownSelect(group);
		directionCombo.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.right-mode"), DIRECTION_RIGHT));
		directionCombo.addItem(new UISelectItem<Integer>(TuxGuitar.getProperty("fretboard.left-mode"), DIRECTION_LEFT));
		directionCombo.setSelectedItem(new UISelectItem<Integer>(null, this.direction));
		groupLayout.set(directionCombo, groupRow, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true, 1, 1, MINIMUM_CONTROL_WIDTH, null, null);

		// ----------------------------------------------------------------------
		groupLayout = new UITableLayout();
		group = factory.createLegendPanel(window);
		group.setLayout(groupLayout);
		group.setText(TuxGuitar.getProperty("fretboard.settings.options"));
		windowLayout.set(group, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		final UICheckBox displayTextNote = factory.createCheckBox(group);
		displayTextNote.setText(TuxGuitar.getProperty("fretboard.display-note-text"));
		displayTextNote.setSelected(!isPercussion && ((this.style & DISPLAY_TEXT_NOTE) != 0) );
		displayTextNote.setEnabled( !isPercussion );
		groupLayout.set(displayTextNote, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		final UICheckBox displayTextScale = factory.createCheckBox(group);
		displayTextScale.setText(TuxGuitar.getProperty("fretboard.display-scale-text"));
		displayTextScale.setSelected(!isPercussion && ((this.style & DISPLAY_TEXT_SCALE) != 0) );
		displayTextScale.setEnabled( !isPercussion );
		groupLayout.set(displayTextScale, 2, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true);

		// ------------------BUTTONS--------------------------
		UITableLayout buttonsLayout = new UITableLayout(0f);
		UIPanel buttons = factory.createPanel(window, false);
		buttons.setLayout(buttonsLayout);
		windowLayout.set(buttons, 3, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_FILL, true, true);

		final UIButton buttonDefaults = factory.createButton(buttons);
		buttonDefaults.setText(TuxGuitar.getProperty("defaults"));
		buttonDefaults.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				window.dispose();
				defaults();
				applyChanges();
			}
		});
		buttonsLayout.set(buttonDefaults, 1, 1, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, MINIMUM_BUTTON_WIDTH, MINIMUM_BUTTON_HEIGHT, null);

		final UIButton buttonOK = factory.createButton(buttons);
		buttonOK.setDefaultButton();
		buttonOK.setText(TuxGuitar.getProperty("ok"));
		buttonOK.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				int style = 0;
				style |= (displayTextNote.isSelected() ? DISPLAY_TEXT_NOTE : 0 );
				style |= (displayTextScale.isSelected() ? DISPLAY_TEXT_SCALE : 0 );

				Integer direction = directionCombo.getSelectedValue();
				if( direction == null ) {
					direction = DIRECTION_RIGHT;
				}

				window.dispose();

				save(style, direction, fontData, rgbBackground, rgbString, rgbFretPoint, rgbNote, rgbScale, rgbTonic);
				applyChanges();
			}
		});
		buttonsLayout.set(buttonOK, 1, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, MINIMUM_BUTTON_WIDTH, MINIMUM_BUTTON_HEIGHT, null);

		final UIButton buttonCancel = factory.createButton(buttons);
		buttonCancel.setText(TuxGuitar.getProperty("cancel"));
		buttonCancel.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				window.dispose();
			}
		});
		buttonsLayout.set(buttonCancel, 1, 3, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_FILL, true, true, 1, 1, MINIMUM_BUTTON_WIDTH, MINIMUM_BUTTON_HEIGHT, null);
		buttonsLayout.set(buttonCancel, UITableLayout.MARGIN_RIGHT, 0f);

		TGDialogUtil.openDialog(window, TGDialogUtil.OPEN_STYLE_CENTER | TGDialogUtil.OPEN_STYLE_PACK);
	}

	public UIFactory getUIFactory() {
		return TGApplication.getInstance(this.context).getFactory();
	}

	protected void applyChanges(){
		this.dispose();
		this.load();

		TGFretBoardEditor.getInstance(this.context).getFretBoard().reloadFromConfig();
	}

	private UIColorModel getColorChooser(final UIWindow window, UILayoutContainer parent, String title, UIColor rgb, int row){
		final UIFactory factory = getUIFactory();

		UITableLayout layout = (UITableLayout) parent.getLayout();
		UILabel label = factory.createLabel(parent);
		label.setText(title);
		layout.set(label, row, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, true, true);

		ButtonColor button = new ButtonColor(window, parent, TuxGuitar.getProperty("choose"));
		button.loadColor(new UIColorModel(rgb.getRed(), rgb.getGreen(), rgb.getBlue()));
		layout.set(button.getControl(), row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true, 1, 1, MINIMUM_CONTROL_WIDTH, null, null);

		return button.getValue();
	}

	private UIFontModel getFontChooser(final UIWindow window, UILayoutContainer parent, String title, UIFont font, int row) {
		final UIFactory factory = getUIFactory();
		final UIFontModel selection = new UIFontModel(font.getName(), font.getHeight(), font.isBold(), font.isItalic());

		UITableLayout layout = (UITableLayout) parent.getLayout();
		UILabel label = factory.createLabel(parent);
		label.setText(title);
		layout.set(label, row, 1, UITableLayout.ALIGN_RIGHT, UITableLayout.ALIGN_CENTER, true, true);

		UIButton button = factory.createButton(parent);
		button.setText(TuxGuitar.getProperty("choose"));
		button.addSelectionListener(new UISelectionListener() {
			public void onSelect(UISelectionEvent event) {
				UIFontChooser uiFontChooser = factory.createFontChooser(window);
				uiFontChooser.setDefaultModel(selection);
				uiFontChooser.choose(new UIFontChooserHandler() {
					public void onSelectFont(UIFontModel model) {
						if( model != null ){
							selection.setName(model.getName());
							selection.setHeight(model.getHeight());
							selection.setBold(model.isBold());
							selection.setItalic(model.isItalic());
						}
					}
				});
			}
		});
		layout.set(button, row, 2, UITableLayout.ALIGN_FILL, UITableLayout.ALIGN_CENTER, true, true, 1, 1, MINIMUM_CONTROL_WIDTH, null, null);

		return selection;
	}

	private class ButtonColor {

		private UIWindow window;
		private UIButton button;
		private UIColor color;
		private UIColorModel value;

		public ButtonColor(UIWindow window, UIContainer parent, String text){
			this.window = window;
			this.value = new UIColorModel();
			this.button = getUIFactory().createButton(parent);
			this.button.setText(text);
			this.addListeners();
		}

		public void loadColor(UIColorModel cm){
			this.value.setRed(cm.getRed());
			this.value.setGreen(cm.getGreen());
			this.value.setBlue(cm.getBlue());

			UIColor color = getUIFactory().createColor(this.value);
			this.button.setFgColor(color);
			this.disposeColor();
			this.color = color;
		}

		public void disposeColor(){
			if( this.color != null && !this.color.isDisposed()){
				this.color.dispose();
				this.color = null;
			}
		}

		public void addListeners(){
			this.button.addSelectionListener(new UISelectionListener() {
				public void onSelect(UISelectionEvent event) {
					UIColorChooser dlg = getUIFactory().createColorChooser(ButtonColor.this.window);
					dlg.setDefaultModel(ButtonColor.this.value);
					dlg.setText(TuxGuitar.getProperty("choose-color"));
					dlg.choose(new UIColorChooserHandler() {
						public void onSelectColor(UIColorModel model) {
							if( model != null) {
								ButtonColor.this.loadColor(model);
							}
						}
					});
				}
			});
			this.button.addDisposeListener(new UIDisposeListener() {
				public void onDispose(UIDisposeEvent event) {
					ButtonColor.this.disposeColor();
				}
			});
		}

		public UIControl getControl() {
			return this.button;
		}

		public UIColorModel getValue(){
			return this.value;
		}
	}
}
