package app.tuxguitar.graphics.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.tuxguitar.song.models.effects.TGEffectBend;
import app.tuxguitar.song.models.effects.TGEffectBend.BendPoint;

/**
 * Maps a bend's time/pitch points onto the note duration box, for tablature drawing.
 */
public final class TGBendPath {

	public static final int VALUE_FULL_TONE = 4;
	/** Horizontal pixels per dialog time unit (0..12), so timing stays readable. */
	public static final float WIDTH_PER_POSITION = 8.0f;

	private static final String[] AMPLITUDE = {
		"", "1/4", "1/2", "3/4", "1",
		"1\u00BC", "1\u00BD", "1\u00BE", "2",
		"2\u00BC", "2\u00BD", "2\u00BE", "3"
	};

	private TGBendPath() {
	}

	public static final class Geometry {
		public float xStart;
		public float xEnd;
		public float yOpen;
		public float yFull;
		public float yLabel;
		public float scale;
	}

	public enum SegmentKind {
		HOLD,
		BEND_UP,
		RELEASE
	}

	public static final class Vertex {
		private final float x;
		private final float y;
		private final int position;
		private final int value;

		private Vertex(float x, float y, int position, int value) {
			this.x = x;
			this.y = y;
			this.position = position;
			this.value = value;
		}

		public float getX() {
			return this.x;
		}

		public float getY() {
			return this.y;
		}

		public int getPosition() {
			return this.position;
		}

		public int getValue() {
			return this.value;
		}
	}

	public static final class Segment {
		private final Vertex from;
		private final Vertex to;
		private final SegmentKind kind;
		private final boolean arrowAtEnd;
		private final boolean labelAtEnd;
		private final boolean preBend;

		private Segment(Vertex from, Vertex to, SegmentKind kind, boolean arrowAtEnd, boolean labelAtEnd, boolean preBend) {
			this.from = from;
			this.to = to;
			this.kind = kind;
			this.arrowAtEnd = arrowAtEnd;
			this.labelAtEnd = labelAtEnd;
			this.preBend = preBend;
		}

		public Vertex getFrom() {
			return this.from;
		}

		public Vertex getTo() {
			return this.to;
		}

		public SegmentKind getKind() {
			return this.kind;
		}

		public boolean isArrowAtEnd() {
			return this.arrowAtEnd;
		}

		public boolean isLabelAtEnd() {
			return this.labelAtEnd;
		}

		public boolean isPreBend() {
			return this.preBend;
		}
	}

	public static float minimumWidth(float scale) {
		return TGEffectBend.MAX_POSITION_LENGTH * WIDTH_PER_POSITION * scale;
	}

	public static String amplitudeLabel(int value) {
		if (value <= 0 || value >= AMPLITUDE.length) {
			return "";
		}
		return AMPLITUDE[value];
	}

	public static List<Segment> build(List<BendPoint> points, Geometry geometry) {
		if (points == null || points.isEmpty() || geometry == null) {
			return Collections.emptyList();
		}

		List<int[]> raw = copyAndCollapse(points);
		if (raw.get(0)[0] > 0) {
			raw.add(0, new int[] {0, raw.get(0)[1]});
		}
		int lastIndex = raw.size() - 1;
		if (raw.get(lastIndex)[0] < TGEffectBend.MAX_POSITION_LENGTH) {
			raw.add(new int[] {TGEffectBend.MAX_POSITION_LENGTH, raw.get(lastIndex)[1]});
		}

		float usableWidth = geometry.xEnd - geometry.xStart;
		float minWidth = minimumWidth(geometry.scale);
		if (usableWidth < minWidth) {
			usableWidth = minWidth;
		}

		List<Vertex> vertices = new ArrayList<Vertex>();
		for (int[] point : raw) {
			vertices.add(new Vertex(
				mapX(point[0], geometry, usableWidth),
				mapY(point[1], geometry),
				point[0],
				point[1]));
		}

		boolean preBend = vertices.get(0).getValue() > 0;
		List<Segment> segments = new ArrayList<Segment>();
		for (int i = 0; i < vertices.size() - 1; i++) {
			Vertex from = vertices.get(i);
			Vertex to = vertices.get(i + 1);
			if (from.getPosition() == to.getPosition() && from.getValue() == to.getValue()) {
				continue;
			}
			SegmentKind kind = kindOf(from.getValue(), to.getValue());
			boolean last = (i == vertices.size() - 2);
			int nextValue = last ? to.getValue() : vertices.get(i + 2).getValue();
			boolean arrowAtEnd = false;
			if (kind != SegmentKind.HOLD) {
				int nextDelta = nextValue - to.getValue();
				if (last || nextDelta == 0
						|| (kind == SegmentKind.BEND_UP && nextDelta < 0)
						|| (kind == SegmentKind.RELEASE && nextDelta > 0)) {
					arrowAtEnd = true;
				}
			}
			boolean labelAtEnd = (to.getValue() > from.getValue()) && (last || to.getValue() >= nextValue);
			segments.add(new Segment(from, to, kind, arrowAtEnd, labelAtEnd, i == 0 && preBend));
		}
		return segments;
	}

	private static List<int[]> copyAndCollapse(List<BendPoint> points) {
		List<int[]> raw = new ArrayList<int[]>();
		for (BendPoint point : points) {
			int position = clamp(point.getPosition(), 0, TGEffectBend.MAX_POSITION_LENGTH);
			int value = clamp(point.getValue(), 0, TGEffectBend.MAX_VALUE_LENGTH);
			raw.add(new int[] {position, value});
		}
		Collections.sort(raw, new Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				return Integer.compare(a[0], b[0]);
			}
		});
		List<int[]> collapsed = new ArrayList<int[]>();
		for (int[] point : raw) {
			if (!collapsed.isEmpty() && collapsed.get(collapsed.size() - 1)[0] == point[0]) {
				collapsed.get(collapsed.size() - 1)[1] = point[1];
			} else {
				collapsed.add(point);
			}
		}
		return collapsed;
	}

	private static SegmentKind kindOf(int fromValue, int toValue) {
		if (toValue > fromValue) {
			return SegmentKind.BEND_UP;
		}
		if (toValue < fromValue) {
			return SegmentKind.RELEASE;
		}
		return SegmentKind.HOLD;
	}

	private static float mapX(int position, Geometry geometry, float usableWidth) {
		return geometry.xStart + (position / (float) TGEffectBend.MAX_POSITION_LENGTH) * usableWidth;
	}

	private static float mapY(int value, Geometry geometry) {
		float span = geometry.yOpen - geometry.yFull;
		if (span <= 0) {
			return geometry.yOpen;
		}
		float t = value / (float) TGEffectBend.MAX_VALUE_LENGTH;
		if (t < 0f) {
			t = 0f;
		}
		if (t > 1f) {
			t = 1f;
		}
		return geometry.yOpen - t * span;
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}
}
